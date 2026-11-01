# 서드파티 서비스와 데이터 출처

Readytogo의 **코드**는 [MIT 라이선스](LICENSE)다. 아래 서비스와 데이터는 각 제공자의 약관을 따르며, MIT 라이선스가 적용되지 않는다. 앱 안에서는 설정 › 정보 › 데이터 출처·오픈소스 라이선스에 같은 내용을 표시한다.

## 외부 서비스 (API)

| 서비스 | 쓰는 곳 | 약관 |
|---|---|---|
| Google Maps SDK for Android | 지도, 경로선, 마커 | [Google Maps Platform 이용약관](https://cloud.google.com/maps-platform/terms) |
| Google Places API (New) | 관광지·식당·숙소 검색, 평점, 가격대, 영업시간, 사진 | 위와 같음. 결과는 기기에만 7일 캐시하고 서버에 저장하지 않는다. 사진은 출처(작성자)를 함께 표시한다 |
| SerpApi (Google Flights) | 항공권 최저가 | [SerpApi Terms](https://serpapi.com/legal) |
| open.er-api.com | 환율 | [ExchangeRate-API Terms](https://www.exchangerate-api.com/terms) |
| Open-Meteo | 일별 날씨 예보 | 데이터 [CC BY 4.0](https://open-meteo.com/en/license), 비상업 무료 이용. 일정표에 "날씨: Open-Meteo"를 표시한다 |
| Anthropic Claude (API) | 말로 일정 고치기, 일정 검토 설명 | [Anthropic 약관·정책](https://www.anthropic.com/legal). Cloud Functions를 거쳐서만 호출하고 키는 앱에 없다 |
| Firebase (Auth, Firestore, Cloud Functions) | 로그인, 일정 저장, 서버 함수 | [Firebase Terms](https://firebase.google.com/terms) |

## 내장 데이터

| 데이터 | 파일 | 출처 |
|---|---|---|
| 공항 목록·좌표 | `app/src/main/assets/airports.json` | [OurAirports](https://ourairports.com/data/) (Public Domain) |
| 도시 목록·좌표·별칭 | `app/src/main/assets/destinations.json` | [GeoNames](https://www.geonames.org/) ([CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)) — 한국어·영어 이름 일부는 직접 보충(`tools/geo/`) |

## 오픈소스 라이브러리

Jetpack Compose·Material 3, AndroidX(Core, Lifecycle, Activity, Navigation, Credentials), Kotlin·Coroutines, maps-compose, Firebase Android SDK, Google Identity, Coil, reorderable 등 — Apache License 2.0. 전체 목록은 `gradle/libs.versions.toml`, 앱의 오픈소스 라이선스 화면에 있다.

## 이미지

앱 아이콘·로고·온보딩 그림(`design/`)은 이 프로젝트를 위해 만든 것이며 코드와 같은 MIT 라이선스로 공개한다.
