package kr.ac.baekseok.readytogo.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.annotation.StringRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.ui.component.SecondaryButton
import kr.ac.baekseok.readytogo.ui.component.SectionHeader
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/** FAQ 한 항목. */
private data class FaqItem(@StringRes val questionRes: Int, @StringRes val answerRes: Int)

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
                    text = stringResource(R.string.support_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Tokens.Palette.TextSecondary,
                )

                val subject = stringResource(R.string.support_mail_subject)
                val noMailApp = stringResource(R.string.support_no_mail_app)
                SecondaryButton(
                    text = stringResource(R.string.support_send_mail),
                    onClick = {
                        // 메일 앱이 없는 기기도 있다. 예외를 던지면 앱이 죽으므로
                        // 토스트로 알리고 넘어간다 — 아무 일도 안 일어나면 버튼이 고장난 줄 안다.
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$SUPPORT_EMAIL")
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                        }
                        runCatching { context.startActivity(intent) }.onFailure {
                            Toast.makeText(context, noMailApp, Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.support_faq),
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
                Text(
                    text = stringResource(item.questionRes),
                    style = MaterialTheme.typography.titleSmall,
                )
                AnimatedVisibility(visible = expandedIndex == index) {
                    Text(
                        text = stringResource(item.answerRes),
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
    FaqItem(R.string.faq_1_q, R.string.faq_1_a),
    FaqItem(R.string.faq_2_q, R.string.faq_2_a),
    FaqItem(R.string.faq_3_q, R.string.faq_3_a),
    FaqItem(R.string.faq_4_q, R.string.faq_4_a),
    FaqItem(R.string.faq_5_q, R.string.faq_5_a),
)