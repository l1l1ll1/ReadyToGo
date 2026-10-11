# 도시 목록(destinations.json)에 영문 이름 열(cityEn, countryEn)을 채운다.
#
# 왜 필요한가: 앱을 영어로 바꿔도 출발지·목적지·도시 검색에 "인천 → 도쿄"가 그대로
# 남았다. 한글 이름만 들고 있었기 때문이다.
#
# 왜 영문을 다시 만들어야 했나: apply-korean-names.ps1이 영문 이름을 한글로 **덮어쓴다**.
# 원본(cities15000.txt)은 저장소에 두지 않으므로 영문은 어디에도 남지 않았다.
#
# 두 단계로 나눠 둔 이유는 한글 쪽과 같다 — 표만 있으면 원본 없이 다시 적용할 수 있다.
#   1. 표 만들기: cities15000.txt가 있으면 좌표로 맞춰 english-city-names.json을 새로 쓴다.
#   2. 적용: 표를 읽어 destinations.json에 열을 채운다. 원본이 없어도 된다.
#
# 여러 번 돌려도 결과가 같다.
#
# 사용법:
#   pwsh tools/geo/apply-english-names.ps1                       # 표만 적용
#   pwsh tools/geo/apply-english-names.ps1 -Source C:\...\cities15000.txt   # 표부터 새로

param(
    # GeoNames cities15000.txt 경로. 주면 표를 다시 만든다.
    # https://download.geonames.org/export/dump/cities15000.zip (CC BY 4.0)
    [string]$Source
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$namesPath = Join-Path $PSScriptRoot "english-city-names.json"
$dataPath = Join-Path $root "app\src\main\assets\destinations.json"

$cities = Get-Content $dataPath -Raw -Encoding UTF8 | ConvertFrom-Json

# 나라 이름은 서른 개뿐이라 표로 적어 둔다. GeoNames의 나라 이름은 "Korea, Republic of"처럼
# 목록에 쓰기 어색한 형태라 그대로 쓸 수 없다.
$COUNTRIES_EN = [ordered]@{
    "괌" = "Guam"; "뉴질랜드" = "New Zealand"; "대만" = "Taiwan"; "독일" = "Germany"
    "라오스" = "Laos"; "마카오" = "Macau"; "말레이시아" = "Malaysia"; "몽골" = "Mongolia"
    "미국" = "United States"; "베트남" = "Vietnam"; "스위스" = "Switzerland"; "스페인" = "Spain"
    "싱가포르" = "Singapore"; "아랍에미리트" = "United Arab Emirates"; "영국" = "United Kingdom"
    "오스트리아" = "Austria"; "이탈리아" = "Italy"; "인도" = "India"; "인도네시아" = "Indonesia"
    "일본" = "Japan"; "중국" = "China"; "체코" = "Czechia"; "캄보디아" = "Cambodia"
    "캐나다" = "Canada"; "태국" = "Thailand"; "터키" = "Türkiye"; "프랑스" = "France"
    "필리핀" = "Philippines"; "호주" = "Australia"; "홍콩" = "Hong Kong"
}

# build-destinations.ps1이 손으로 넣는 관광지들. 인구 기준에 걸려 GeoNames 목록에 없어서
# 좌표로 맞출 수 없다. 그쪽 표와 같은 순서로 적어 둔다.
$MANUAL_EN = [ordered]@{
    "괌|괌" = "Guam"; "일본|하코네" = "Hakone"; "일본|유후인" = "Yufuin"
    "일본|가마쿠라" = "Kamakura"; "스위스|인터라켄" = "Interlaken"; "스위스|체르마트" = "Zermatt"
    "오스트리아|할슈타트" = "Hallstatt"; "체코|체스키크룸로프" = "Cesky Krumlov"
    "베트남|호이안" = "Hoi An"; "태국|아유타야" = "Ayutthaya"; "필리핀|보라카이" = "Boracay"
    "대만|지우펀" = "Jiufen"; "인도네시아|우붓" = "Ubud"; "이탈리아|친퀘테레" = "Cinque Terre"
    "프랑스|몽생미셸" = "Mont-Saint-Michel"; "터키|카파도키아" = "Cappadocia"
}

if ($Source) {
    if (-not (Test-Path $Source)) { throw "원본을 찾을 수 없다: $Source" }

    # **좌표로 맞춘다.** 이름으로는 맞출 수 없다 — 한글로 덮어썼기 때문이다.
    # build-destinations.ps1이 좌표를 소수 넷째 자리로 반올림해 저장하므로 그대로 맞춘다.
    $byCoord = @{}
    foreach ($line in [System.IO.File]::ReadLines($Source, [System.Text.Encoding]::UTF8)) {
        $f = $line -split "`t"
        if ($f.Count -lt 6) { continue }
        $key = "{0},{1}" -f [Math]::Round([double]$f[4], 4), [Math]::Round([double]$f[5], 4)
        if (-not $byCoord.ContainsKey($key)) { $byCoord[$key] = $f[1] }
    }

    $table = [ordered]@{}
    $table["_주석"] = "도시 영문 이름. 키는 '나라(한글)|도시(한글)'. apply-english-names.ps1이 만든다."
    $table["_출처"] = "GeoNames cities15000 (CC BY 4.0) — 좌표로 맞춰 뽑았다. 목록에 없는 관광지는 스크립트의 MANUAL_EN 표."

    $missing = @()
    foreach ($c in $cities) {
        $key = "$($c.country)|$($c.city)"
        $coord = "{0},{1}" -f [Math]::Round([double]$c.lat, 4), [Math]::Round([double]$c.lon, 4)
        $en = if ($MANUAL_EN.Contains($key)) { $MANUAL_EN[$key] } else { $byCoord[$coord] }
        if ($en) { $table[$key] = $en } else { $missing += $key }
    }

    $json = $table | ConvertTo-Json -Depth 3
    [System.IO.File]::WriteAllText($namesPath, $json, (New-Object System.Text.UTF8Encoding($false)))
    Write-Output ("표 갱신: {0}개 / 못 찾음 {1}개" -f ($table.Count - 2), $missing.Count)
    if ($missing.Count -gt 0) { Write-Output ("  " + ($missing -join ", ")) }
}

if (-not (Test-Path $namesPath)) { throw "표가 없다. -Source로 원본을 주고 한 번 돌려라: $namesPath" }
$names = Get-Content $namesPath -Raw -Encoding UTF8 | ConvertFrom-Json

$filled = 0
$blank = 0
foreach ($c in $cities) {
    $key = "$($c.country)|$($c.city)"
    $cityEn = $names.PSObject.Properties[$key]
    # 영문을 못 찾으면 한글을 그대로 둔다. 빈 칸으로 두면 영어 화면에서 이름이 사라진다.
    $value = if ($cityEn) { $cityEn.Value } else { $blank++; $c.city }
    $c | Add-Member -NotePropertyName cityEn -NotePropertyValue $value -Force
    $countryEn = if ($COUNTRIES_EN.Contains($c.country)) { $COUNTRIES_EN[$c.country] } else { $c.country }
    $c | Add-Member -NotePropertyName countryEn -NotePropertyValue $countryEn -Force
    if ($cityEn) { $filled++ }
}

# BOM이 붙으면 안드로이드의 JSONArray가 첫 글자에서 파싱에 실패한다.
$json = $cities | ConvertTo-Json -Depth 4
[System.IO.File]::WriteAllText($dataPath, $json, (New-Object System.Text.UTF8Encoding($false)))

Write-Output ("영문 채움: {0}개 / 한글 그대로: {1}개 / 전체: {2}개" -f $filled, $blank, $cities.Count)
