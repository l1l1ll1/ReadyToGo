# 도시 목록 생성 도구

`app/src/main/assets/destinations.json`(769개 도시 / 30개국)을 만드는 스크립트다.
앱은 이 파일을 읽어 출발지·목적지 목록을 보여준다.

## 파일

| 파일 | 역할 |
|---|---|
| `build-destinations.ps1` | 원본 데이터에서 도시 목록을 처음부터 만든다 |
| `korean-city-names.json` | 영문 도시명 → 한글 표기 표 |
| `apply-korean-names.ps1` | 이미 만들어진 목록의 이름만 표대로 바꾼다 |

## 한글 표기 규칙

GeoNames가 한국어 별칭을 주는 도시는 그대로 쓰고, 없는 도시만 `korean-city-names.json`에
손으로 적는다. 적는 기준은 두 가지다.

- **한국에서 통용되는 표기가 있으면 한글만.** 예: `달랏`, `베르가모`, `산세바스티안`
- **애매한 소도시는 `한글(영문)` 병기.** 예: `까마우(Ca Mau)`, `우디네(Udine)`

병기하는 이유: 이름만 한글로 바꾸면 사용자가 그게 어디인지 알 수 없고, 검색해 보기도 어렵다.
괄호 안 영문은 발음기호를 뺀 형태로 적어 그대로 복사해 검색할 수 있게 한다.

## 이름만 고칠 때

`korean-city-names.json`에 항목을 추가하고 실행한다. 원본 데이터가 필요 없다.

```
powershell -ExecutionPolicy Bypass -File tools/geo/apply-korean-names.ps1
```

여러 번 돌려도 결과가 같다.

## 전체를 다시 만들 때

공항·도시 원본은 수십 MB라 저장소에 넣지 않았다. 아래 두 파일을 이 폴더에 내려받고 실행한다.

| 파일 | 출처 | 라이선스 |
|---|---|---|
| `airports.csv` | https://ourairports.com/data/ | public domain |
| `cities15000.txt` | https://download.geonames.org/export/dump/ | CC BY 4.0 |

```
powershell -ExecutionPolicy Bypass -File tools/geo/build-destinations.ps1
```

출처 표시는 앱의 입력 화면 하단과 설정 → 정보에 있다. CC BY 4.0이 요구하는 사항이라
도시 목록을 바꿔도 그 표기는 지우면 안 된다.

## 수록 기준

- 정기 여객편이 있는 중대형 공항 반경 **200km** 이내
- 그 도시의 공항으로 인정하는 범위는 **100km** (반둥 같은 다른 도시 공항이 섞이지 않게)
- 인구 5만 이상, 나라당 최대 40곳
- 25km 안의 도시끼리는 하나로 합치고 수도를 우선한다

## 주의

PowerShell 스크립트는 **UTF-8 BOM**으로 저장해야 한다. Windows PowerShell 5.1은 BOM이 없으면
파일을 ANSI로 읽어서 한글 주석과 문자열이 깨진다.
