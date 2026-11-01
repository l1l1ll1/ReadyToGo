package kr.ac.baekseok.readytogo.ui.format

/**
 * 받침에 따라 갈리는 조사. 앞이 받침 있음 / 없음일 때의 꼴.
 *
 * "이(가)"처럼 둘 다 적으면 장소 이름이 들어가는 문장마다 괄호가 붙어 읽기 어렵다(검토 카드에서 봤다).
 */
enum class Particle(val withFinal: String, val withoutFinal: String) {
    SUBJECT("이", "가"),
    OBJECT("을", "를"),
    TOPIC("은", "는"),
    AND("과", "와"),
}

/**
 * [word] 뒤에 받침에 맞는 조사를 붙인다. 예) "라멘 하루카" + AND → "라멘 하루카와"
 *
 * 받침은 **마지막 글자**로 정한다. 끝의 괄호·마침표·공백은 건너뛴다.
 * - 한글: 유니코드 음절의 종성으로 정확히 안다.
 * - 숫자: 읽는 소리로(1 일, 3 삼, 6 육, 7 칠, 8 팔, 0 영은 받침 있음).
 * - 영문: 대개 받침 없이 읽혀서(Cafe 카페, Bar 바) l·m·n으로 끝날 때만 받침으로 본다.
 *   정확하지 않지만 이름을 소리로 바꿀 방법이 없고, 어긋나도 뜻은 통한다.
 */
fun withParticle(word: String, particle: Particle): String =
    word + if (hasFinalConsonant(word)) particle.withFinal else particle.withoutFinal

internal fun hasFinalConsonant(word: String): Boolean {
    val last = word.trimEnd().lastOrNull { it.isLetterOrDigit() } ?: return false
    return when {
        last in HANGUL_FIRST..HANGUL_LAST -> (last - HANGUL_FIRST) % FINAL_COUNT != 0
        last.isDigit() -> last in DIGITS_WITH_FINAL
        else -> last.lowercaseChar() in LATIN_WITH_FINAL
    }
}

private const val HANGUL_FIRST = '가'
private const val HANGUL_LAST = '힣'

/** 한글 음절은 종성 27개 + 없음 1개가 한 묶음이다. 나머지가 0이면 받침이 없다. */
private const val FINAL_COUNT = 28

private const val DIGITS_WITH_FINAL = "013678"
private const val LATIN_WITH_FINAL = "lmn"
