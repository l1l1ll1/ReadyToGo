# 국제공항 반경 200km 안의 도시 목록 생성
# 출처: OurAirports (airports.csv, public domain), GeoNames (cities15000, CC-BY 4.0)

$base = $PSScriptRoot
# 도시를 목록에 넣을 기준. 공항에서 차·기차로 2~3시간 거리를 근사한 값.
$RADIUS_KM = 200.0
# 그 도시의 공항으로 인정할 범위. 이보다 멀면 다른 도시의 공항이다.
$AIRPORT_ATTACH_RADIUS_KM = 100.0
$MIN_POP = 50000
$MAX_PER_COUNTRY = 40
$MAX_AIRPORTS_PER_CITY = 3

$COUNTRIES = [ordered]@{
    "JP" = "일본";   "VN" = "베트남"; "TH" = "태국";   "PH" = "필리핀"
    "TW" = "대만";   "CN" = "중국";   "HK" = "홍콩";   "MO" = "마카오"
    "SG" = "싱가포르"; "MY" = "말레이시아"; "ID" = "인도네시아"; "KH" = "캄보디아"
    "LA" = "라오스"; "MN" = "몽골";   "IN" = "인도";   "GU" = "괌"
    "AU" = "호주";   "NZ" = "뉴질랜드"; "US" = "미국"; "CA" = "캐나다"
    "FR" = "프랑스"; "IT" = "이탈리아"; "ES" = "스페인"; "GB" = "영국"
    "DE" = "독일";   "CH" = "스위스"; "CZ" = "체코";  "AT" = "오스트리아"
    "TR" = "터키";   "AE" = "아랍에미리트"
}

function Get-DistanceKm($lat1, $lon1, $lat2, $lon2) {
    $R = 6371.0
    $dLat = ($lat2 - $lat1) * [Math]::PI / 180.0
    $dLon = ($lon2 - $lon1) * [Math]::PI / 180.0
    $a = [Math]::Sin($dLat/2) * [Math]::Sin($dLat/2) +
         [Math]::Cos($lat1 * [Math]::PI/180.0) * [Math]::Cos($lat2 * [Math]::PI/180.0) *
         [Math]::Sin($dLon/2) * [Math]::Sin($dLon/2)
    return 2 * $R * [Math]::Asin([Math]::Min(1.0, [Math]::Sqrt($a)))
}

# ---- 1. 여객 공항만 (정기편 운항 + 중대형). 군기지·경비행장이 걸러진다. ----
$airportsByCc = @{}
$apCount = 0
Import-Csv "$base\ourairports.csv" -Encoding UTF8 | ForEach-Object {
    if (-not $COUNTRIES.Contains($_.iso_country)) { return }
    if ($_.scheduled_service -ne "yes") { return }
    if ($_.iata_code -notmatch '^[A-Z]{3}$') { return }
    if ($_.type -ne "large_airport" -and $_.type -ne "medium_airport") { return }

    $cc = $_.iso_country
    if (-not $airportsByCc.ContainsKey($cc)) { $airportsByCc[$cc] = New-Object System.Collections.ArrayList }
    [void]$airportsByCc[$cc].Add([PSCustomObject]@{
        iata = $_.iata_code
        lat = [double]$_.latitude_deg
        lon = [double]$_.longitude_deg
        big = ($_.type -eq "large_airport")
    })
    $script:apCount++
}
Write-Output ("여객 공항: {0:N0}" -f $apCount)

# ---- 2. 대상국 도시 ----
$cities = New-Object System.Collections.ArrayList
$reader = New-Object System.IO.StreamReader("$base\cities15000.txt", [System.Text.Encoding]::UTF8)
while ($null -ne ($line = $reader.ReadLine())) {
    $f = $line -split "`t"
    if ($f.Count -lt 15) { continue }
    $cc = $f[8]
    if (-not $COUNTRIES.Contains($cc)) { continue }

    # PPLX는 "도시의 일부"다. 도쿄의 아다치구, 파리의 11구 같은 행정구가 여기 들어간다.
    # 여행 목적지로는 도시 단위여야 하므로 제외한다.
    $featureCode = $f[7]
    if ($featureCode -eq "PPLX" -or $featureCode -eq "PPLS" -or $featureCode -eq "PPLL") { continue }

    $pop = 0; [void][int64]::TryParse($f[14], [ref]$pop)
    if ($pop -lt $MIN_POP) { continue }

    $korean = $null
    foreach ($alt in ($f[3] -split ',')) {
        if ($alt -match '^[가-힣][가-힣\s]*$') { $korean = $alt.Trim(); break }
    }
    # GeoNames 한글 별칭은 "요코하마 시"처럼 행정단위가 붙어 있다. 도시 이름만 남긴다.
    if ($korean) { $korean = $korean -replace '\s+(시|현|주|부)$', '' }

    $displayName = $f[1]
    if ($korean) { $displayName = $korean }

    [void]$cities.Add([PSCustomObject]@{
        cc = $cc; name = $displayName
        lat = [double]$f[4]; lon = [double]$f[5]; pop = $pop
        # PPLC = 수도. 병합할 때 인구보다 우선한다 — 마닐라가 인구 많은 케손시티에
        # 흡수돼 목록에서 사라지는 일을 막기 위함이다.
        isCapital = ($featureCode -eq "PPLC")
    })
}
$reader.Close()
Write-Output ("도시(인구 {0:N0}+): {1:N0}" -f $MIN_POP, $cities.Count)

# ---- 3. 공항 반경 안만 남기기 ----
$result = New-Object System.Collections.ArrayList
foreach ($c in $cities) {
    $near = $airportsByCc[$c.cc]
    if (-not $near) { continue }
    $matched = New-Object System.Collections.ArrayList
    foreach ($a in $near) {
        if ([Math]::Abs($a.lat - $c.lat) -gt 2.0) { continue }
        if ([Math]::Abs($a.lon - $c.lon) -gt 3.0) { continue }
        $d = Get-DistanceKm $c.lat $c.lon $a.lat $a.lon
        if ($d -le $RADIUS_KM) { [void]$matched.Add([PSCustomObject]@{ iata = $a.iata; d = $d; big = $a.big }) }
    }
    if ($matched.Count -eq 0) { continue }

    # 도시 포함 여부는 200km(차·기차 2~3시간)로 보지만, **그 도시의 공항**으로 붙일 범위는
    # 더 좁게 잡는다. 200km를 그대로 쓰면 자카르타에 반둥 공항(BDO)이 붙어,
    # 항공 검색에서 120km 떨어진 다른 도시행이 최저가로 뜰 수 있다.
    $own = @($matched | Where-Object { $_.d -le $AIRPORT_ATTACH_RADIUS_KM })
    # 100km 안에 공항이 없으면 항공 검색이 불가능해지므로 가장 가까운 것 하나는 남긴다.
    if ($own.Count -eq 0) { $own = @($matched | Sort-Object d | Select-Object -First 1) }

    # 큰 공항을 앞에 둔다. 도쿄에서 하네다·나리타가 소형 공항에 밀리지 않게.
    $codes = ($own | Sort-Object @{E={-not $_.big}}, d | Select-Object -First $MAX_AIRPORTS_PER_CITY).iata
    [void]$result.Add([PSCustomObject]@{
        cc = $c.cc; name = $c.name; lat = [Math]::Round($c.lat,4); lon = [Math]::Round($c.lon,4)
        pop = $c.pop; airports = @($codes); isCapital = $c.isCapital
    })
}

# ---- 4. 가까운 도시 합치기 ----
# GeoNames는 방콕의 짜뚜짝구, 파리의 15구, 도쿄의 오타구를 별도 도시로 넣어둔다.
# PPLX 코드로 일부는 걸렀지만 PPLA2/PPL로 분류된 구역이 많이 남는다.
# 같은 나라 안에서 반경 25km 안에 여럿 있으면 인구가 가장 큰 곳만 남겨 큰 도시로 흡수시킨다.
# 25km인 이유: 도쿄-요코하마(27km), 오사카-고베(30km)처럼 여행지로 구분되는 곳은 살아남아야 한다.
$MERGE_RADIUS_KM = 25.0
$merged = New-Object System.Collections.ArrayList
$before = $result.Count
foreach ($cc in $COUNTRIES.Keys) {
    $kept = New-Object System.Collections.ArrayList
    # 수도 먼저, 그다음 인구순. 먼저 자리를 잡은 도시가 주변 구역을 흡수한다.
    $ordered = $result | Where-Object { $_.cc -eq $cc } |
        Sort-Object @{E={-not $_.isCapital}}, @{E="pop"; Descending=$true}
    foreach ($c in $ordered) {
        $absorbed = $false
        foreach ($k in $kept) {
            if ((Get-DistanceKm $c.lat $c.lon $k.lat $k.lon) -le $MERGE_RADIUS_KM) { $absorbed = $true; break }
        }
        if (-not $absorbed) { [void]$kept.Add($c) }
    }
    foreach ($k in $kept) { [void]$merged.Add($k) }
}
Write-Output ("{0}km 병합: {1:N0} -> {2:N0}" -f $MERGE_RADIUS_KM, $before, $merged.Count)
$result = $merged

# ---- 5. 나라별 인구 상위 N ----
$final = New-Object System.Collections.ArrayList
foreach ($cc in $COUNTRIES.Keys) {
    $result | Where-Object { $_.cc -eq $cc } | Sort-Object pop -Descending |
        Select-Object -First $MAX_PER_COUNTRY | ForEach-Object {
            [void]$final.Add([PSCustomObject]@{
                country = $COUNTRIES[$cc]; city = $_.name
                lat = $_.lat; lon = $_.lon; airports = $_.airports; pop = $_.pop
            })
        }
}

# ---- 5. 인구 기준에 걸려 빠지는 유명 관광지 수동 추가 ----
$manual = @(
    @("괌","괌",13.4443,144.7937,@("GUM")),
    @("일본","하코네",35.2324,139.1069,@("HND","NRT")),
    @("일본","유후인",33.2646,131.3536,@("OIT","FUK")),
    @("일본","가마쿠라",35.3192,139.5466,@("HND","NRT")),
    @("스위스","인터라켄",46.6863,7.8632,@("BRN","ZRH")),
    @("스위스","체르마트",46.0207,7.7491,@("GVA","ZRH")),
    @("오스트리아","할슈타트",47.5622,13.6493,@("SZG")),
    @("체코","체스키크룸로프",48.8127,14.3175,@("PRG")),
    @("베트남","호이안",15.8801,108.3380,@("DAD")),
    @("태국","아유타야",14.3532,100.5689,@("DMK","BKK")),
    @("필리핀","보라카이",11.9674,121.9248,@("MPH","KLO")),
    @("대만","지우펀",25.1097,121.8445,@("TSA","TPE")),
    @("인도네시아","우붓",-8.5069,115.2625,@("DPS")),
    @("이탈리아","친퀘테레",44.1461,9.6439,@("PSA","GOA")),
    @("프랑스","몽생미셸",48.6361,-1.5115,@("RNS")),
    @("터키","카파도키아",38.6431,34.8289,@("NAV","ASR"))
)
$added = 0
foreach ($m in $manual) {
    $dup = $final | Where-Object { $_.country -eq $m[0] -and $_.city -eq $m[1] }
    if ($dup) { continue }
    # 인구 기준에 걸려 빠진 곳이라 인구는 0으로 두되, 목록 맨 뒤로 밀리지 않게 중간값을 준다.
    [void]$final.Add([PSCustomObject]@{ country = $m[0]; city = $m[1]; lat = $m[2]; lon = $m[3]; airports = $m[4]; pop = 60000 })
    $added++
}
Write-Output ("수동 추가: {0}" -f $added)

# 나라 안에서는 인구순. 사용자가 고를 법한 큰 도시가 위에 오게 한다.
# ---- 6. 한글 이름 보완 ----
# GeoNames에 한국어 별칭이 없는 도시를 손으로 채운다.
# 표를 따로 둔 이유: 이름만 고칠 때는 전체를 다시 만들 필요가 없다.
# apply-korean-names.ps1이 같은 표를 읽어 destinations.json에 덧씌운다.
$KOREAN_NAMES = Get-Content "$PSScriptRoot\korean-city-names.json" -Raw -Encoding UTF8 | ConvertFrom-Json
foreach ($f in $final) {
    $korean = $KOREAN_NAMES.PSObject.Properties[$f.city]
    if ($korean) { $f.city = $korean.Value }
}
# 병합 반경 밖이라 살아남았지만 실제로는 큰 도시의 일부인 곳들.
$EXCLUDE = @("Khlong Sam Wa", "Tin Shui Wai", "Ban Khoan", "Budta", "Malingao",
             "Eisenzicken", "Ban I Chang", "Lahbab", "Warīsān", "Musaffah")
$final = $final | Where-Object { $EXCLUDE -notcontains $_.city }

$sorted = $final | Sort-Object country, @{E="pop"; Descending=$true} |
    Select-Object country, city, lat, lon, airports
$json = $sorted | ConvertTo-Json -Depth 4
[System.IO.File]::WriteAllText("$base\destinations.json", $json, (New-Object System.Text.UTF8Encoding($false)))
Write-Output ("최종 {0:N0}개 도시 / {1}개국" -f $sorted.Count, ($sorted.country | Select-Object -Unique).Count)
Write-Output ("한글 이름: {0:N0}" -f ($sorted | Where-Object { $_.city -match '[가-힣]' }).Count)
