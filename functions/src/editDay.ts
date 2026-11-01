/**
 * "말로 일정 고치기" callable 함수.
 *
 * 앱이 그날 일정 요약과 사용자 문장을 보내면, Claude Haiku가 도구 호출로 바꾼 명령 목록을
 * 돌려준다. **명령을 실행하지 않는다.** 검증과 실행은 앱이 한다 — 편집 규칙(끼니는 고정,
 * 아침 위로는 못 옮김 등)이 앱에 있고, 두 곳에 두면 어긋난다.
 */
import Anthropic from "@anthropic-ai/sdk";
import * as logger from "firebase-functions/logger";
import {HttpsError, onCall} from "firebase-functions/https";
import {defineSecret} from "firebase-functions/params";
import {consumeQuota} from "./rateLimit";
import {SYSTEM_PROMPT, TOOLS} from "./tools";

const ANTHROPIC_API_KEY = defineSecret("ANTHROPIC_API_KEY");

// 모델은 고정한다. 바꾸면 비용과 동작이 함께 바뀌므로 코드 변경으로만 바꾼다.
const MODEL = "claude-haiku-5-5";
// 명령 몇 개면 끝나는 응답이다. 길게 쓸 일이 없어 낮게 잡아 비용 상한으로도 쓴다.
const MAX_TOKENS = 512;
// 입력 상한. 요약이 터무니없이 크면 토큰 비용이 커지므로 여기서 자른다.
const MAX_SENTENCE_LENGTH = 200;
const MAX_ITEMS = 20;

/** 앱이 보내는 하루 일정의 한 줄. 별칭 id(a1, m_lunch)를 쓴다. */
export interface DayItem {
  id: string;
  kind: "meal" | "place";
  name: string;
  /** 식당 종류(라멘, 카페…) 또는 관광지 타입 목록. */
  category: string;
  stayMin: number;
  moveMin: number;
  /** 숙소까지 직선거리(km). 숙소가 없으면 null. */
  lodgingKm: number | null;
}

interface EditDayRequest {
  sentence: string;
  items: DayItem[];
}

interface EditCommand {
  op: string;
  [key: string]: unknown;
}

export const editDay = onCall(
  {
    region: "asia-northeast3",
    maxInstances: 2,
    timeoutSeconds: 30,
    secrets: [ANTHROPIC_API_KEY],
  },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "로그인이 필요합니다.");

    const input = parseRequest(request.data);
    await consumeQuota(uid);

    const client = new Anthropic({
      apiKey: ANTHROPIC_API_KEY.value(),
      // 사용자가 화면에서 기다리는 요청이다. 오래 걸리면 차라리 실패를 알린다.
      timeout: 20_000,
      maxRetries: 1,
    });

    let response: Anthropic.Message;
    try {
      response = await client.messages.create({
        model: MODEL,
        max_tokens: MAX_TOKENS,
        // 문장 하나를 명령 몇 개로 바꾸는 일이라 깊이 생각할 필요가 없다. 끄면 출력 토큰이 준다.
        thinking: {type: "disabled"},
        output_config: {effort: "low"},
        system: SYSTEM_PROMPT,
        tools: TOOLS,
        tool_choice: {type: "auto"},
        messages: [{role: "user", content: renderUserMessage(input)}],
      });
    } catch (error) {
      if (error instanceof Anthropic.RateLimitError) {
        throw new HttpsError("resource-exhausted", "잠시 후 다시 시도해 주세요.");
      }
      if (error instanceof Anthropic.APIError) {
        logger.error("Claude 호출 실패", {status: error.status});
        throw new HttpsError("unavailable", "지금은 처리할 수 없어요.");
      }
      throw error;
    }

    // 문장은 남기지 않는다(개인 일정이다). 토큰만 남겨 비용을 추적한다.
    logger.info("editDay", {
      stop: response.stop_reason,
      input: response.usage.input_tokens,
      output: response.usage.output_tokens,
    });

    // 거부(refusal)나 도구 없는 답이면 명령 0개 — 앱이 "이해 못 했어요"를 띄운다.
    const commands: EditCommand[] = response.stop_reason === "refusal" ? [] :
      response.content
        .filter((b): b is Anthropic.ToolUseBlock => b.type === "tool_use")
        .map((b) => ({op: b.name, ...(b.input as Record<string, unknown>)}));

    return {
      commands,
      usage: {
        input: response.usage.input_tokens,
        output: response.usage.output_tokens,
      },
    };
  },
);

/**
 * 형식이 틀린 요청은 Claude를 부르기 전에 돌려보낸다 — 한도도 쓰지 않는다.
 * @param {unknown} data 앱이 보낸 그대로의 값
 * @return {EditDayRequest} 검사를 통과한 요청
 */
function parseRequest(data: unknown): EditDayRequest {
  const body = data as Partial<EditDayRequest> | null;
  const sentence =
    typeof body?.sentence === "string" ? body.sentence.trim() : "";
  if (!sentence || sentence.length > MAX_SENTENCE_LENGTH) {
    throw new HttpsError("invalid-argument", "문장이 비었거나 너무 깁니다.");
  }
  const items = body?.items;
  if (!Array.isArray(items) || items.length === 0 || items.length > MAX_ITEMS) {
    throw new HttpsError("invalid-argument", "일정이 비었거나 너무 깁니다.");
  }
  for (const item of items) {
    if (typeof item?.id !== "string" || typeof item?.name !== "string") {
      throw new HttpsError("invalid-argument", "일정 형식이 맞지 않습니다.");
    }
  }
  return {sentence, items};
}

/**
 * 일정 한 줄. JSON 그대로 보내는 것보다 따옴표·키 이름이 빠져 토큰이 적다.
 * 일정 검토(reviewIssues)도 같은 형식을 쓴다 — 모델이 보는 일정 모양이 같아야 한다.
 * 예) a1 | 관광지 | 센소지 | buddhist_temple | 체류 90분 | 이동 15분 | 숙소 3.1km
 * @param {DayItem} item 일정 항목
 * @return {string} 한 줄
 */
export function renderItemLine(item: DayItem): string {
  return [
    item.id,
    item.kind === "meal" ? "끼니" : "관광지",
    item.name,
    item.category,
    `체류 ${item.stayMin}분`,
    `이동 ${item.moveMin}분`,
    item.lodgingKm == null ? "숙소 거리 모름" : `숙소 ${item.lodgingKm}km`,
  ].join(" | ");
}

/**
 * @param {EditDayRequest} request 검사를 통과한 요청
 * @return {string} 모델에 줄 사용자 메시지
 */
function renderUserMessage(request: EditDayRequest): string {
  const {sentence, items} = request;
  const lines = items.map(renderItemLine);
  return `오늘 일정(시간 순):\n${lines.join("\n")}\n\n사용자: ${sentence}`;
}
