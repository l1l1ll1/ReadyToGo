package kr.ac.baekseok.readytogo.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import kr.ac.baekseok.readytogo.R

/**
 * 워드마크 전용 글꼴 (Sora, SIL Open Font License 1.1).
 *
 * **본문에는 쓰지 않는다.** 본문은 시스템 기본 글꼴을 쓴다(Type.kt 참고) — 한글 자소가
 * 들어 있는 글꼴을 따로 넣으면 APK가 몇 MB씩 커지는데, Sora에는 한글이 없어 본문에
 * 쓰면 한글만 다른 글꼴로 떨어져 나간다. 로고 글자에만 쓴다.
 *
 * **가변 글꼴(variable font) 한 파일로 두 굵기를 낸다.** Sora는 google/fonts에 가변
 * 파일만 올라와 있고, 굵기별 파일을 따로 넣으면 그만큼 APK가 커진다.
 * `variationSettings`로 'wght' 축을 지정하면 같은 파일에서 600·800을 뽑아 쓸 수 있다.
 */
val SoraFontFamily = FontFamily(
    Font(
        resId = R.font.sora,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId = R.font.sora,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800)),
    ),
)
