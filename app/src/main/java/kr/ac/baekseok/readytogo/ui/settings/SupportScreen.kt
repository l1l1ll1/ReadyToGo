package kr.ac.baekseok.readytogo.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kr.ac.baekseok.readytogo.ui.component.SecondaryButton
import kr.ac.baekseok.readytogo.ui.component.SectionHeader
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/** FAQ 한 항목. */
private data class FaqItem(val question: String, val answer: String)

/**
 * 고객센터 — 문의 메일 + FAQ.
 *
 * FAQ를 접었다 펴는 형태로 둔다. 답변을 모두 펼쳐 놓으면 질문 목록을 훑을 수 없다.
 */
@Composable
fun SupportScreen() {
    val context = LocalContext.current
    // 어느 항목이 펼쳐졌는지. 화면 회전에도 남아야 해서 rememberSaveable을 쓴다.
    var expandedIndex by rememberSaveable { mutableStateOf(-1) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = Tokens.Space.screenHorizontal,
            end = Tokens.Space.screenHorizontal,
            top = Tokens.Space.lg,
            bottom = Tokens.Space.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.md)) {
                Text(
                    text = "문제가 있거나 제안할 것이 있으면 메일로 알려 주세요.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Tokens.Palette.TextSecondary,
                )
                SecondaryButton(
                    text = "문의 메일 보내기",
                    onClick = {
                        // 메일 앱이 없는 기기도 있다. 그때는 아무 일도 일어나지 않게 둔다 —
                        // 예외를 던지면 앱이 죽는다.
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$SUPPORT_EMAIL")
                            putExtra(Intent.EXTRA_SUBJECT, "[Readytogo] 문의")
                        }
                        runCatching { context.startActivity(intent) }
                    },
                )
            }
        }

        item {
            SectionHeader(
                title = "자주 묻는 질문",
                modifier = Modifier.padding(top = Tokens.Space.xl),
            )
        }

        items(FAQ.size) { index ->
            val item = FAQ[index]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // 같은 항목을 다시 누르면 접는다.
                        expandedIndex = if (expandedIndex == index) -1 else index
                    }
                    .padding(vertical = Tokens.Space.md),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
            ) {
                Text(text = item.question, style = MaterialTheme.typography.titleSmall)
                AnimatedVisibility(visible = expandedIndex == index) {
                    Text(
                        text = item.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Tokens.Palette.TextSecondary,
                    )
                }
                HorizontalDivider(color = Tokens.Palette.Divider)
            }
        }
    }
}

/** 문의 받을 주소. 실제 주소를 넣기 전까지 예시로 둔다. */
private const val SUPPORT_EMAIL = "support@readytogo.app"

private val FAQ = listOf(
    FaqItem(
        question = "항공권이나 숙소를 앱에서 바로 예약할 수 있나요?",
        answer = "아니요. 가격과 링크까지만 보여 드립니다. 예약은 해당 사이트에서 진행해 주세요.",
    ),
    FaqItem(
        question = "추천 식당은 어떤 기준으로 고르나요?",
        answer = "평점과 그날 동선을 반반으로 보고 점수를 냅니다. " +
            "평점만 보면 일정과 멀리 떨어진 곳이 뜨고, 동선만 보면 아무 데나 가까운 곳이 뜹니다.",
    ),
    FaqItem(
        question = "끼니마다 두 곳이 나오는 이유가 뭔가요?",
        answer = "하나는 점수가 가장 높은 곳, 다른 하나는 예산이나 가격·동선을 기준으로 고른 대안입니다. " +
            "카드에 어떤 기준으로 골랐는지 이름표가 붙습니다.",
    ),
    FaqItem(
        question = "로그인하지 않으면 무엇이 제한되나요?",
        answer = "일정을 만드는 것은 모두 됩니다. 저장과 즐겨찾기만 로그인이 필요합니다.",
    ),
    FaqItem(
        question = "항공 가격이 실제와 다릅니다.",
        answer = "검색 시점의 가격이고, 실제 요금은 수시로 바뀝니다. " +
            "\"예시 데이터\" 표시가 붙어 있으면 실시간 조회가 아니라 참고용 값입니다.",
    ),
)