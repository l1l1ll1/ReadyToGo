package kr.ac.baekseok.readytogo.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kr.ac.baekseok.readytogo.BuildConfig
import kr.ac.baekseok.readytogo.ui.component.SectionHeader
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 정보 — 버전, 사용한 데이터·라이브러리 출처, 개인정보 처리 방침.
 *
 * 출처 표기는 선택이 아니다. 도시 목록은 OurAirports와 GeoNames(CC BY 4.0)에서 왔고,
 * 라이선스가 출처 표시를 요구한다. 입력 화면 하단에도 같은 표기가 있다.
 */
@Composable
fun AboutScreen() {
    LazyColumn(
        contentPadding = PaddingValues(
            start = Tokens.Space.screenHorizontal,
            end = Tokens.Space.screenHorizontal,
            top = Tokens.Space.lg,
            bottom = Tokens.Space.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
    ) {
        item { InfoRow(label = "앱 버전", value = BuildConfig.VERSION_NAME) }

        item {
            Column {
                HorizontalDivider(
                    color = Tokens.Palette.Divider,
                    modifier = Modifier.padding(vertical = Tokens.Space.lg),
                )
                SectionHeader(title = "데이터 출처")
            }
        }
        item {
            Paragraph(
                "도시·공항 목록: OurAirports (public domain), GeoNames (CC BY 4.0)\n" +
                    "장소·평점·가격대: Google Places API\n" +
                    "항공 가격: SerpApi (Google Flights)\n" +
                    "환율: open.er-api.com",
            )
        }

        item {
            Column {
                HorizontalDivider(
                    color = Tokens.Palette.Divider,
                    modifier = Modifier.padding(vertical = Tokens.Space.lg),
                )
                SectionHeader(title = "오픈소스 라이선스")
            }
        }
        item {
            Paragraph(
                "Jetpack Compose, Material 3, AndroidX Navigation — Apache License 2.0\n" +
                    "Google Maps SDK for Android, Places SDK — Google API 서비스 약관\n" +
                    "maps-compose — Apache License 2.0\n" +
                    "Kotlin Coroutines — Apache License 2.0",
            )
        }

        item {
            Column {
                HorizontalDivider(
                    color = Tokens.Palette.Divider,
                    modifier = Modifier.padding(vertical = Tokens.Space.lg),
                )
                SectionHeader(title = "개인정보 처리 방침")
            }
        }
        item {
            Paragraph(
                "이 앱은 일정을 만들기 위해 입력한 조건(출발지, 목적지, 날짜, 예산, 여행 목적)과 " +
                    "로그인한 계정의 이메일·이름을 저장합니다.\n\n" +
                    "저장된 일정과 즐겨찾기는 로그인한 계정에만 연결되며, 다른 사용자에게 공개되지 않습니다. " +
                    "회원 탈퇴 시 저장된 일정과 즐겨찾기는 모두 삭제됩니다.\n\n" +
                    "장소·항공 검색을 위해 입력한 도시와 날짜가 Google·SerpApi에 전달됩니다. " +
                    "위치 권한은 사용하지 않으며, 기기의 현재 위치를 수집하지 않습니다.",
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Tokens.Space.md),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Tokens.Palette.TextSecondary,
        )
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Tokens.Palette.TextSecondary,
        modifier = Modifier.padding(top = Tokens.Space.sm),
    )
}