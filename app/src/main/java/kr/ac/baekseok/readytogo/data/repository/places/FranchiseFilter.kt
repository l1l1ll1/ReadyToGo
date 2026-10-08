package kr.ac.baekseok.readytogo.data.repository.places

/**
 * 한국에도 있는 프랜차이즈를 추천에서 제외한다.
 *
 * 해외여행 앱인데 맥도날드·스타벅스를 추천하면 갈 이유가 없다.
 * 실제로 자카르타 검색에서 맥도날드가 아침 추천 1위로 올라온 적이 있다.
 *
 * 이름으로만 거르므로 오탐을 막는 장치를 둔다.
 * - **식당 업종에만 적용한다.** "Hotel Domino" 같은 무관한 이름이 걸리지 않게 한다.
 * - **단어 경계로 비교한다.** 다른 단어 속에 우연히 포함된 경우를 걸러낸다.
 * - 표기가 나라마다 달라(맥도날드 / McDonald's / マクドナルド) 브랜드마다 여러 표기를 둔다.
 */
object FranchiseFilter {

    /**
     * 제외할 브랜드. 한 줄이 한 브랜드이고, 같은 줄의 표기는 모두 같은 곳을 가리킨다.
     *
     * Places는 한국어를 요청해도 한국어 이름이 없는 곳은 현지어나 영어로 준다.
     * 그래서 한 브랜드에 한글·영문·현지어를 함께 적어야 한다.
     */
    val EXCLUDED_CHAINS: List<List<String>> = listOf(
        // 패스트푸드
        listOf("맥도날드", "McDonald", "マクドナルド"),
        listOf("버거킹", "Burger King", "バーガーキング"),
        listOf("KFC", "케이에프씨", "ケンタッキー"),
        listOf("서브웨이", "Subway", "サブウェイ"),
        listOf("롯데리아", "Lotteria"),
        listOf("맘스터치", "Mom's Touch"),

        // 커피·디저트
        listOf("스타벅스", "Starbucks", "スターバックス"),
        listOf("투썸플레이스", "A Twosome Place"),
        listOf("이디야", "Ediya"),
        listOf("폴바셋", "Paul Bassett"),
        listOf("공차", "Gong Cha", "貢茶"),
        listOf("크리스피크림", "Krispy Kreme"),
        listOf("던킨", "Dunkin"),

        // 일본 체인 (한국에도 매장이 있다)
        listOf("이치란", "Ichiran", "一蘭"),
        listOf("코코이찌방야", "Coco Ichibanya", "CoCo壱番屋"),
        listOf("요시노야", "Yoshinoya", "吉野家"),
        listOf("마루가메", "Marugame", "丸亀製麺"),
        listOf("모스버거", "MOS Burger", "モスバーガー"),
        // 회전초밥·디저트 체인. 실제로 도쿄 추천 결과에 올라와서 추가했다.
        // "스시로"는 한국어 조사 결합형("스시로 유명한 집")과 글자가 겹친다. 가게 이름에 그런
        // 표현이 들어갈 일은 드물어 그대로 두지만, 오탐이 보이면 영문·일본어 표기만 남기면 된다.
        listOf("스시로", "Sushiro", "スシロー"),
        listOf("쿠라스시", "구라스시", "Kura Sushi", "くら寿司"),
        listOf("하브스", "HARBS", "ハーブス"),
        listOf("사보텐", "Saboten", "さぼてん"),

        // 글로벌
        listOf("피자헛", "Pizza Hut"),
        listOf("도미노피자", "Domino's Pizza", "Dominos Pizza"),
        listOf("타코벨", "Taco Bell"),
        listOf("셰이크쉑", "Shake Shack"),
        listOf("파파존스", "Papa John"),
        listOf("하드락카페", "하드 락 카페", "Hard Rock Cafe"),
        listOf("하이디라오", "Haidilao", "海底捞", "海底撈"),
    )

    /**
     * 위 목록에 걸리더라도 남길 곳.
     *
     * 도쿄 나카메구로 스타벅스 리저브 로스터리처럼 **매장 자체가 관광지인 경우**를 위한 자리다.
     * 지금은 비워둔다 — 실제로 걸러진 결과를 보고 필요한 것만 넣는 편이 낫다.
     */
    val ALLOWED_EXCEPTIONS: List<String> = emptyList()

    /**
     * 제외 대상인지 판단한다.
     *
     * @param name 가게 이름
     * @param isEatery 주 업종이 식당 계열인지. false면 항상 통과시킨다.
     */
    fun isExcluded(name: String, isEatery: Boolean): Boolean {
        if (!isEatery) return false

        val normalized = normalize(name)
        if (ALLOWED_EXCEPTIONS.any { normalized.contains(normalize(it)) }) return false

        return EXCLUDED_CHAINS.any { brand ->
            brand.any { matchesWord(normalized, normalize(it)) }
        }
    }

    /**
     * 대소문자·구두점 차이를 없앤다.
     *
     * 구두점을 **지우지 않고 공백으로 바꾼다.** 지워버리면 "McDonald's"가 "mcdonalds"가 되어
     * 단어 경계 자체가 사라진다 — 경계를 보려고 정규화했는데 경계를 없애면 앞뒤가 안 맞는다.
     */
    private fun normalize(text: String): String =
        text.lowercase()
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .trim()

    /**
     * 단어 경계 비교. 앞뒤에 다른 알파벳이 붙어 있으면 다른 단어로 본다.
     *
     * 한글·한자 브랜드는 단어 사이에 공백이 없어 경계를 잡을 수 없으므로 포함 여부만 본다.
     * 짧고 고유한 이름들이라 다른 단어에 섞일 일이 드물다.
     */
    private fun matchesWord(haystack: String, needle: String): Boolean {
        if (needle.isEmpty()) return false

        var index = haystack.indexOf(needle)
        while (index >= 0) {
            if (needle.none { it in 'a'..'z' }) return true
            if (hasWordBoundary(haystack, index, needle.length)) return true
            index = haystack.indexOf(needle, index + 1)
        }
        return false
    }

    private fun hasWordBoundary(haystack: String, start: Int, length: Int): Boolean {
        val before = haystack.getOrNull(start - 1)
        if (before != null && before in 'a'..'z') return false

        var end = start + length
        // 영어 소유격·복수형의 s는 브랜드 이름의 일부로 본다.
        // "McDonald's"는 구두점이 공백이 되어 "mcdonald s"가 되지만, 구두점 없이 붙여 쓴
        // "mcdonalds"도 같은 가게다.
        if (haystack.getOrNull(end) == 's') end++

        val after = haystack.getOrNull(end)
        return after == null || after !in 'a'..'z'
    }
}
