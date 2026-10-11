package kr.ac.baekseok.readytogo.data.model

import java.io.Serializable
import java.util.Locale

/**
 * 고를 수 있는 도시 하나.
 *
 * 자유 입력 대신 목록에서 고르게 만든 이유:
 * - 좌표를 미리 들고 있어서 "파리" 같은 글자를 좌표로 바꾸는 Geocoding 호출이 필요 없다(비용 0).
 * - 오타나 모호함("강남"이 어느 강남인지)이 생기지 않는다.
 * - 공항 코드를 같이 들고 있어서 항공권 API를 붙일 때 "도쿄 → NRT" 변환이 따로 필요 없다.
 *
 * 수록 기준은 **정기 여객편이 있는 국제공항 반경 200km 이내**다(차·기차로 2~3시간 근사).
 * 비행기로 가는 해외여행 앱이라 공항에서 닿지 않는 도시는 넣어봐야 쓸 수 없다.
 */
data class Destination(
    val country: String,
    val city: String,
    val location: LatLng,
    /** 이 도시로 갈 때 쓰는 공항 코드. 가까운 순. 도쿄처럼 공항이 둘 이상인 곳도 있다. */
    val airportCodes: List<String>,
    /**
     * 영문 이름. 영어로 보는 사용자에게 "인천 → 도쿄"를 보여줄 수는 없다.
     *
     * 비어 있으면 한글 이름을 그대로 쓴다 — 빈 칸보다 낫다. 기본값을 둔 이유는
     * 이 모델이 Firestore에 저장되기 때문이다. 예전에 저장된 일정에는 이 열이 없다.
     */
    val cityEn: String = "",
    val countryEn: String = "",
) : Serializable {

    /**
     * 지금 앱 언어에 맞는 이름.
     *
     * **모델이 직접 Locale을 본다.** 화면마다 "지금 영어인가"를 넘기면 도시 이름을 쓰는
     * 열 몇 군데에 전부 인자가 하나씩 붙는다. 앱 언어는 LocaleManager가 프로세스
     * 기본 Locale에 반영하므로 여기서 바로 읽을 수 있다(AppLocale 주석 참고).
     */
    val localizedCity: String
        get() = if (isEnglish) cityEn.ifBlank { city } else city

    val localizedCountry: String
        get() = if (isEnglish) countryEn.ifBlank { country } else country

    /** 목록에서 한 줄로 보여줄 때. 예: "도쿄 (HND)" */
    val displayName: String
        get() = if (airportCodes.isEmpty()) localizedCity else "$localizedCity (${airportCodes.first()})"

    /**
     * 좁은 자리에 쓸 짧은 이름. 예: "코타쿠알라무다(Kota Kuala Muda)" → "코타쿠알라무다"
     *
     * 한글 표기가 애매한 소도시는 이름에 영문을 병기해 두는데(tools/geo/README.md),
     * 그대로 쓰면 "인천 → 도쿄" 카드나 "도쿄 5일" 같은 제목에서 잘려 나간다.
     * 고를 때는 영문이 도움이 되지만, 이미 고른 뒤에는 한글만으로 충분하다.
     */
    val shortName: String
        get() = localizedCity.substringBefore('(').trim()

    private companion object {
        /** 앱 언어가 영어인지. 비교만 하므로 값을 들고 있지 않고 쓸 때마다 읽는다. */
        val isEnglish: Boolean get() = Locale.getDefault().language == "en"
    }
}
