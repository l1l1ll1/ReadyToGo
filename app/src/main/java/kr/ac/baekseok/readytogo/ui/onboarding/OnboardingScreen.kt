package kr.ac.baekseok.readytogo.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.annotation.StringRes
import kotlinx.coroutines.launch
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.ui.component.PhotoCard
import kr.ac.baekseok.readytogo.ui.component.PrimaryButton
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/** 온보딩 한 장. */
private data class OnboardingPage(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    /** 사진 자리 색을 고르는 기준. 장마다 다른 색이 나오게 한다. */
    val colorKey: String,
)

/**
 * 온보딩 3장 — **첫 실행에만** 보여준다.
 *
 * 앱이 무엇을 해주는지 세 문장으로 설명한다. 여행 계획 앱은 "조건만 넣으면 끝난다"는 점이
 * 전달되지 않으면 입력 화면의 항목 수를 보고 그냥 닫는다.
 *
 * @param onFinish 마지막 장에서 시작하거나 건너뛰었을 때.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == PAGES.lastIndex

    Surface(color = Tokens.Palette.Background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(Tokens.Space.screenHorizontal)) {

            // 건너뛰기는 오른쪽 위. 마지막 장에서는 "시작하기" 버튼과 겹쳐 의미가 없어 숨긴다.
            Box(modifier = Modifier.fillMaxWidth()) {
                if (!isLastPage) {
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                } else {
                    // 버튼이 사라지면 아래 내용이 위로 튀어오른다. 같은 높이를 비워 둔다.
                    Spacer(modifier = Modifier.height(Tokens.Size.button))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { index ->
                val page = PAGES[index]
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    PhotoCard(
                        key = page.colorKey,
                        title = "",
                        height = Tokens.Size.photoCardHeight * ONBOARDING_IMAGE_SCALE,
                    )
                    Spacer(modifier = Modifier.height(Tokens.Space.xl))
                    Text(
                        text = stringResource(page.titleRes),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(Tokens.Space.md))
                    Text(
                        text = stringResource(page.bodyRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Tokens.Palette.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            PageIndicator(
                count = PAGES.size,
                current = pagerState.currentPage,
                modifier = Modifier.padding(vertical = Tokens.Space.xl),
            )

            PrimaryButton(
                text = stringResource(
                    if (isLastPage) R.string.onboarding_start else R.string.common_next,
                ),
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
            )

            Spacer(modifier = Modifier.height(Tokens.Space.xl))
        }
    }
}

/** 몇 번째 장인지 알려주는 점. 세 장뿐이라 숫자보다 점이 읽기 쉽다. */
@Composable
private fun PageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(count) { index ->
            val color by animateColorAsState(
                targetValue = if (index == current) Tokens.Palette.Point else Tokens.Palette.Divider,
                label = "indicator",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = Tokens.Space.xs)
                    .size(Tokens.Space.sm)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/** 사진 자리를 온보딩에서는 더 크게 쓴다. 설명보다 그림이 먼저 눈에 들어와야 한다. */
private const val ONBOARDING_IMAGE_SCALE = 1.4f

private val PAGES = listOf(
    OnboardingPage(
        titleRes = R.string.onboarding_1_title,
        bodyRes = R.string.onboarding_1_body,
        colorKey = "onboarding-1",
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_2_title,
        bodyRes = R.string.onboarding_2_body,
        colorKey = "onboarding-2",
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_3_title,
        bodyRes = R.string.onboarding_3_body,
        colorKey = "onboarding-3",
    ),
)