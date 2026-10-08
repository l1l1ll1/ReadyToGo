# 도시 목록(destinations.json)의 영문 이름을 한글 표기로 바꾼다.
#
# 왜 따로 있는가: destinations.json을 처음부터 다시 만들려면 OurAirports와 GeoNames 원본
# (수십 MB)이 필요하다. 그 파일들은 저장소에 넣지 않으므로, 이름만 고칠 때는 전체를 다시
# 만들지 않고 이 스크립트로 덧씌운다. 같은 표를 build-destinations.ps1도 읽으므로
# 다음에 전체를 다시 만들어도 결과는 같다.
#
# 여러 번 돌려도 결과가 같다 — 이미 한글이면 표에 키가 없어 그냥 지나간다.
#
# 사용법:
#   pwsh tools/geo/apply-korean-names.ps1

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$namesPath = Join-Path $PSScriptRoot "korean-city-names.json"
$dataPath = Join-Path $root "app\src\main\assets\destinations.json"

$names = Get-Content $namesPath -Raw -Encoding UTF8 | ConvertFrom-Json
$cities = Get-Content $dataPath -Raw -Encoding UTF8 | ConvertFrom-Json

$before = ($cities | Where-Object { $_.city -match '[A-Za-z]' }).Count
$changed = 0

foreach ($city in $cities) {
    # 주석 키("_주석", "_규칙")는 도시 이름과 겹칠 일이 없어 따로 거르지 않는다.
    $korean = $names.PSObject.Properties[$city.city]
    if ($korean) {
        $city.city = $korean.Value
        $changed++
    }
}

$after = ($cities | Where-Object { $_.city -match '^[^가-힣]*$' }).Count

# ConvertTo-Json은 비ASCII를 \uXXXX로 바꾸지 않지만, 쓰기는 BOM 없는 UTF-8이어야 한다.
# BOM이 붙으면 안드로이드의 JSONArray가 첫 글자에서 파싱에 실패한다.
$json = $cities | ConvertTo-Json -Depth 4
[System.IO.File]::WriteAllText($dataPath, $json, (New-Object System.Text.UTF8Encoding($false)))

Write-Output ("바꾼 도시: {0}개" -f $changed)
Write-Output ("영문만 남은 도시: {0}개 → {1}개" -f $before, $after)
Write-Output ("전체: {0}개" -f $cities.Count)
