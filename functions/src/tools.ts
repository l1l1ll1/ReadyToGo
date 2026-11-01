/**
 * "말로 일정 고치기"에서 Claude에게 주는 도구와 지시문.
 *
 * **도구는 기준만 고른다.** 어느 식당으로 바꿀지는 앱의 추천 점수(평점 0.5 + 동선 0.5)가
 * 정한다. 모델이 식당을 직접 고르면 점수 규칙을 우회하고, 없는 가게를 지어낼 수도 있다.
 *
 * **날짜 인자가 없다.** 앱이 일정표 탭에서 보고 있는 하루만 보내므로 날짜는 앱이 채운다.
 * 다른 날을 가리키는 명령이 애초에 나올 수 없다.
 *
 * 관광지 교체(swap_place)는 ⑨-2로 미뤘다 — 계획에 관광지 예비 후보가 없다.
 */
import type Anthropic from "@anthropic-ai/sdk";

export const MEALS = ["breakfast", "lunch", "dinner"] as const;
export const MEAL_BASES = [
  "near_lodging",
  "near_prev",
  "cheaper",
  "exclude_same_kind",
] as const;

// strict: 모델이 enum 밖의 값이나 빠진 필드를 보내지 못하게 한다.
// 그래도 앱이 다시 검사한다(없는 id 같은 것은 스키마로 막을 수 없다).
export const TOOLS: Anthropic.Tool[] = [
  {
    name: "remove",
    description: "관광지 하나를 그날 일정에서 뺀다. 끼니(m_로 시작하는 id)는 뺄 수 없다.",
    strict: true,
    input_schema: {
      type: "object",
      properties: {
        id: {type: "string", description: "뺄 관광지의 id. 예: a2"},
      },
      required: ["id"],
      additionalProperties: false,
    },
  },
  {
    name: "swap_meal",
    description:
      "끼니 식당을 다른 후보로 바꾼다. 식당은 고르지 말고 기준만 정한다. " +
      "near_lodging=숙소에 더 가까운 곳, near_prev=바로 앞 일정에 더 가까운 곳, " +
      "cheaper=더 싼 곳, exclude_same_kind=지금과 다른 종류의 음식.",
    strict: true,
    input_schema: {
      type: "object",
      properties: {
        meal: {type: "string", enum: [...MEALS]},
        basis: {type: "string", enum: [...MEAL_BASES]},
      },
      required: ["meal", "basis"],
      additionalProperties: false,
    },
  },
  {
    name: "move",
    description:
      "관광지 하나를 같은 날 안에서 다른 항목 바로 뒤로 옮긴다. 끼니는 옮길 수 없다.",
    strict: true,
    input_schema: {
      type: "object",
      properties: {
        id: {type: "string", description: "옮길 관광지의 id"},
        after_id: {type: "string", description: "이 항목 바로 뒤에 놓는다"},
      },
      required: ["id", "after_id"],
      additionalProperties: false,
    },
  },
];

// 마지막 문장은 사고(thinking)를 끈 모델이 도구 호출을 본문 글로 써 버리는 일을 막는다
// (Anthropic 문서의 권장 문구).
export const SYSTEM_PROMPT = [
  "너는 여행 앱의 하루 일정 편집기다. 사용자 문장을 읽고 도구 호출로만 답한다.",
  "일정은 위에서 아래로 시간 순이다. 점심 뒤에 오는 관광지가 '오후'다.",
  "요청에 맞는 도구를 필요한 만큼 호출한다. 여러 요청이면 여러 번 호출한다.",
  "일정 편집과 관계없는 말이거나 도구로 할 수 없는 요청이면 " +
    "도구를 호출하지 않는다.",
  "id는 목록에 있는 것만 쓴다.",
  "When you use a tool, you may say a brief sentence first. " +
    "If no tool can express what the user asked for, " +
    "say so instead of guessing. " +
    "Do not include internal or system XML tags in your response.",
].join("\n");

// ---- 일정 검토(reviewIssues) ----

/**
 * 검토용 도구. **위 세 도구에 issue_id만 덧붙여 만든다** — 정의를 두 벌 두면 한쪽만 고쳐져 어긋난다.
 *
 * issue_id는 그 요청의 문제 id만 enum으로 허용한다(strict). 없는 문제에 명령을 붙일 수 없다.
 * 요청마다 도구가 달라지지만, 이 길이에서는 프롬프트 캐시가 걸리지 않아 잃는 것이 없다.
 * @param {string[]} issueIds 이번 요청의 문제 id
 * @return {Anthropic.Tool[]} explain + 편집 도구 세 개
 */
export function reviewTools(issueIds: string[]): Anthropic.Tool[] {
  const issueId = {
    type: "string",
    enum: issueIds,
    description: "이 명령이 고치는 문제의 id",
  };
  const explain: Anthropic.Tool = {
    name: "explain",
    description:
      "문제 하나가 왜 여행에 불편한지 한 문장(40자 안팎)으로 말한다. " +
      "문제 문장과 같은 언어로 쓴다(앱 화면 언어다). " +
      "문제마다 정확히 한 번 호출한다.",
    strict: true,
    input_schema: {
      type: "object",
      properties: {
        issue_id: issueId,
        why: {type: "string"},
      },
      required: ["issue_id", "why"],
      additionalProperties: false,
    },
  };
  const edits = TOOLS.map((tool): Anthropic.Tool => {
    const schema = tool.input_schema as {
      properties: Record<string, unknown>;
      required: string[];
    };
    return {
      ...tool,
      input_schema: {
        ...tool.input_schema,
        properties: {issue_id: issueId, ...schema.properties},
        required: ["issue_id", ...schema.required],
      },
    };
  });
  return [explain, ...edits];
}

// editDay와 같은 이유로 마지막 영어 문장을 둔다.
export const REVIEW_SYSTEM_PROMPT = [
  "너는 여행 앱의 일정 검토관이다. " +
    "앱이 규칙으로 찾은 문제 목록과 해당 날의 일정을 받는다.",
  "문제마다 explain을 한 번 호출해 왜 불편한지 한 문장으로 말하고, " +
    "고칠 편집 도구를 필요한 만큼 호출한다. " +
    "모든 호출에 그 문제의 issue_id를 넣는다.",
  "문제와 일정에 적힌 사실만 쓴다. " +
    "영업시간·거리·가격 등 목록에 없는 사실을 지어내지 않는다.",
  "id는 그 문제가 속한 날의 일정 목록에 있는 것만 쓴다. 날마다 id가 따로 매겨진다.",
  "끼니(m_로 시작)는 빼거나 옮길 수 없고 swap_meal로만 바꾼다. " +
    "식당은 고르지 말고 기준만 정한다.",
  "When you use a tool, you may say a brief sentence first. " +
    "Do not include internal or system XML tags in your response.",
].join("\n");
