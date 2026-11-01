/**
 * 일정 검토(⑨-2) callable 함수.
 *
 * **문제는 앱의 규칙이 찾는다.** 여기로는 문제 목록과 그 문제가 있는 날의 일정만 온다.
 * 모델은 문제마다 "왜 불편한지 한 줄"과 고칠 명령(editDay와 같은 세 도구)만 낸다 —
 * 일정 전체를 주고 문제를 찾게 하면 없는 휴무일을 지어낼 수 있다.
 *
 * 문제를 하나씩 따로 묻지 않고 **한 번에** 보낸다. editDay를 문제마다 부르면 문제 세 건에
 * 세 번이라 분당 한도(5회)를 금방 넘고, 지시문·도구 토큰을 매번 다시 낸다.
 *
 * 명령을 실행하지 않는다. 검증과 실행은 앱이 editDay와 같은 경로로 한다.
 */
import Anthropic from "@anthropic-ai/sdk";
import * as logger from "firebase-functions/logger";
import {HttpsError, onCall} from "firebase-functions/https";
import {defineSecret} from "firebase-functions/params";
import {DayItem, renderItemLine} from "./editDay";
import {consumeQuota} from "./rateLimit";
import {REVIEW_SYSTEM_PROMPT, reviewTools} from "./tools";

const ANTHROPIC_API_KEY = defineSecret("ANTHROPIC_API_KEY");

// editDay와 같은 모델. 바꾸면 비용과 동작이 함께 바뀐다.
const MODEL = "claude-haiku-5-5";
// 문제 하나에 설명 한 줄 + 명령 한두 개. 상한 문제 수만큼 넉넉히 두되 비용 상한으로도 쓴다.
const MAX_TOKENS = 1024;
// 입력 상한. 앱은 문제를 보통 몇 건만 보내지만, 터무니없이 큰 요청은 토큰 비용이 커진다.
const MAX_ISSUES = 8;
const MAX_DAYS = 8;
const MAX_ITEMS_PER_DAY = 20;
const MAX_FACT_LENGTH = 200;

/** 규칙이 찾은 문제 하나. fact는 앱이 쓴 사실 문장이다. */
interface Issue {
  id: string;
  day: number;
  fact: string;
}

interface DayBlock {
  day: number;
  items: DayItem[];
}

interface ReviewRequest {
  issues: Issue[];
  days: DayBlock[];
}

interface Fix {
  issue_id: string;
  why: string;
  commands: Array<Record<string, unknown>>;
}

export const reviewIssues = onCall(
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
    // editDay와 같은 한도를 나눠 쓴다. 검토는 카드를 펼칠 때 한 번이라 한도를 거의 먹지 않는다.
    await consumeQuota(uid);

    const client = new Anthropic({
      apiKey: ANTHROPIC_API_KEY.value(),
      timeout: 20_000,
      maxRetries: 1,
    });

    let response: Anthropic.Message;
    try {
      response = await client.messages.create({
        model: MODEL,
        max_tokens: MAX_TOKENS,
        thinking: {type: "disabled"},
        output_config: {effort: "low"},
        system: REVIEW_SYSTEM_PROMPT,
        tools: reviewTools(input.issues.map((issue) => issue.id)),
        // 문제가 있다는 것은 이미 정해졌다. 도구 없이 글로만 답하면 쓸 것이 없다.
        tool_choice: {type: "any"},
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

    logger.info("reviewIssues", {
      issues: input.issues.length,
      stop: response.stop_reason,
      input: response.usage.input_tokens,
      output: response.usage.output_tokens,
    });

    return {
      fixes: response.stop_reason === "refusal" ? [] : collectFixes(response),
      usage: {
        input: response.usage.input_tokens,
        output: response.usage.output_tokens,
      },
    };
  },
);

/**
 * 도구 호출을 문제별로 묶는다. explain은 설명, 나머지는 명령이 된다.
 * 명령 모양은 editDay 응답과 같게 둔다(op + 인자) — 앱이 같은 해석기를 쓴다.
 * @param {Anthropic.Message} response 모델 응답
 * @return {Fix[]} 문제별 설명과 명령
 */
function collectFixes(response: Anthropic.Message): Fix[] {
  const byIssue = new Map<string, Fix>();
  const fixFor = (id: string): Fix => {
    let fix = byIssue.get(id);
    if (!fix) {
      fix = {issue_id: id, why: "", commands: []};
      byIssue.set(id, fix);
    }
    return fix;
  };

  for (const block of response.content) {
    if (block.type !== "tool_use") continue;
    const {issue_id: issueId, ...args} = block.input as Record<string, unknown>;
    if (typeof issueId !== "string") continue;
    if (block.name === "explain") {
      fixFor(issueId).why = typeof args.why === "string" ? args.why : "";
    } else {
      fixFor(issueId).commands.push({op: block.name, ...args});
    }
  }
  return [...byIssue.values()];
}

/**
 * 형식이 틀린 요청은 Claude를 부르기 전에 돌려보낸다 — 한도도 쓰지 않는다.
 * @param {unknown} data 앱이 보낸 그대로의 값
 * @return {ReviewRequest} 검사를 통과한 요청
 */
function parseRequest(data: unknown): ReviewRequest {
  const body = data as Partial<ReviewRequest> | null;
  const issues = body?.issues;
  const days = body?.days;
  if (!Array.isArray(issues) || issues.length === 0 ||
      issues.length > MAX_ISSUES) {
    throw new HttpsError("invalid-argument", "문제 목록이 비었거나 너무 깁니다.");
  }
  if (!Array.isArray(days) || days.length === 0 || days.length > MAX_DAYS) {
    throw new HttpsError("invalid-argument", "일정이 비었거나 너무 깁니다.");
  }
  for (const issue of issues) {
    if (typeof issue?.id !== "string" || typeof issue?.day !== "number" ||
        typeof issue?.fact !== "string" ||
        issue.fact.length > MAX_FACT_LENGTH) {
      throw new HttpsError("invalid-argument", "문제 형식이 맞지 않습니다.");
    }
  }
  for (const day of days) {
    if (typeof day?.day !== "number" || !Array.isArray(day.items) ||
        day.items.length > MAX_ITEMS_PER_DAY) {
      throw new HttpsError("invalid-argument", "일정 형식이 맞지 않습니다.");
    }
    for (const item of day.items) {
      if (typeof item?.id !== "string" || typeof item?.name !== "string") {
        throw new HttpsError("invalid-argument", "일정 형식이 맞지 않습니다.");
      }
    }
  }
  return {issues, days};
}

/**
 * 날마다 일정 목록, 그다음 문제 목록.
 * 예) [3일차] 일정(시간 순):\na1 | 관광지 | …\n\n문제:\nkind-3-LUNCH (3일차): …
 * @param {ReviewRequest} request 검사를 통과한 요청
 * @return {string} 모델에 줄 사용자 메시지
 */
function renderUserMessage(request: ReviewRequest): string {
  const days = request.days.map((day) =>
    `[${day.day}일차] 일정(시간 순):\n` +
    day.items.map(renderItemLine).join("\n")
  );
  const issues = request.issues.map((issue) =>
    `${issue.id} (${issue.day}일차): ${issue.fact}`
  );
  return `${days.join("\n\n")}\n\n문제:\n${issues.join("\n")}`;
}
