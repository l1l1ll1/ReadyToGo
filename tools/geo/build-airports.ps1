# 목적지 목록에 나오는 공항의 좌표표 생성 → app/src/main/assets/airports.json
# 출처: OurAirports (airports.csv, public domain)
#
# 왜 destinations.json에 넣지 않고 따로 두나: 그 파일은 한글·영문 이름 적용까지 여러 단계를
# 거쳐 만들어져 다시 생성하면 전체가 바뀐다. 공항 좌표는 코드만 있으면 붙일 수 있어 따로 둔다.
#
# 사용: ourairports.csv를 이 폴더에 받은 뒤 실행한다(build-destinations.ps1과 같은 위치).
#   https://davidmegginson.github.io/ourairports-data/airports.csv

param(
    [string]$CsvPath = "$PSScriptRoot\ourairports.csv"
)

$root = Resolve-Path "$PSScriptRoot\..\.."
$destinations = Get-Content "$root\app\src\main\assets\destinations.json" -Raw -Encoding UTF8 | ConvertFrom-Json

# 앱에 나오는 공항만 담는다. 전 세계 공항을 다 넣으면 수 MB가 되는데 쓰는 것은 일부다.
$wanted = @{}
foreach ($d in $destinations) { foreach ($code in $d.airports) { $wanted[$code] = $true } }

$found = [ordered]@{}
Import-Csv $CsvPath -Encoding UTF8 | ForEach-Object {
    $code = $_.iata_code
    if (-not $wanted.ContainsKey($code)) { return }
    # 같은 IATA 코드가 폐쇄된 옛 공항에도 남아 있는 경우가 있다. 운영 중인 쪽을 쓴다.
    if ($_.type -eq "closed") { return }
    if ($found.Contains($code)) { return }
    $found[$code] = @([Math]::Round([double]$_.latitude_deg, 4), [Math]::Round([double]$_.longitude_deg, 4))
}

$missing = $wanted.Keys | Where-Object { -not $found.Contains($_) } | Sort-Object
Write-Output ("공항 {0}곳 중 {1}곳 좌표 찾음" -f $wanted.Count, $found.Count)
if ($missing) { Write-Output ("못 찾음: " + ($missing -join ", ")) }

# 코드 순으로 정렬해 다시 만들어도 diff가 작게 한다.
$sorted = [ordered]@{}
foreach ($k in ($found.Keys | Sort-Object)) { $sorted[$k] = $found[$k] }
$json = $sorted | ConvertTo-Json -Compress
[System.IO.File]::WriteAllText("$root\app\src\main\assets\airports.json", $json, (New-Object System.Text.UTF8Encoding $false))
