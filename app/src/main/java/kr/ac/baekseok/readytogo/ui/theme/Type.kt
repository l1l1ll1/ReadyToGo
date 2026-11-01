package kr.ac.baekseok.readytogo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 글꼴 크기는 [Tokens.FontSize]에서 가져온다. 여기서는 굵기와 줄간격만 정한다.
 *
 * 별도 폰트를 넣지 않고 시스템 기본(한글은 본고딕)을 쓴다. 폰트 파일을 추가하면
 * APK가 커지고 한글·영문·숫자 폭이 어긋나는 문제를 따로 손봐야 한다.
 *
 * 줄간격을 크기의 1.3~1.4배로 둔 이유: 한글은 영문보다 글자가 빽빽해서
 * Material 기본 줄간격으로는 답답해 보인다.
 *
 * **⑫부터 시안(design/mockup) 굵기를 따른다** — 제목류는 한 단계 굵게(ExtraBold·Bold),
 * 카드 제목도 SemiBold → Bold. 흰 카드·옅은 바탕이라 글자가 굵어야 위계가 선다.
 */
val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = Tokens.FontSize.display,
        lineHeight = 42.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = Tokens.FontSize.headline,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = Tokens.FontSize.sheetTitle,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = Tokens.FontSize.title,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = Tokens.FontSize.section,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = Tokens.FontSize.body,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = Tokens.FontSize.body,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = Tokens.FontSize.caption,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = Tokens.FontSize.body,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = Tokens.FontSize.label,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = Tokens.FontSize.label,
        lineHeight = 17.sp,
    ),
)
