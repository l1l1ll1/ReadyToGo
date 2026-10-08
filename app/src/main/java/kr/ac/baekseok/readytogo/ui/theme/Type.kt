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
 * 줄간격을 크기의 1.4배 정도로 둔 이유: 한글은 영문보다 글자가 빽빽해서
 * Material 기본 줄간격으로는 답답해 보인다.
 */
val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = Tokens.FontSize.display,
        lineHeight = 40.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = Tokens.FontSize.title,
        lineHeight = 30.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = Tokens.FontSize.section,
        lineHeight = 26.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = Tokens.FontSize.body,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = Tokens.FontSize.body,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = Tokens.FontSize.caption,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = Tokens.FontSize.body,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = Tokens.FontSize.caption,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = Tokens.FontSize.label,
        lineHeight = 16.sp,
    ),
)
