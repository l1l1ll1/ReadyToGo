package kr.ac.baekseok.readytogo.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kr.ac.baekseok.readytogo.BuildConfig
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.ui.component.SectionHeader
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 정보 — 버전, 사용한 데이터·라이브러리 출처, 개인정보 처리 방침.
 *
 * 출처 표기는 선택이 아니다. 도시 목록은 OurAirports와 GeoNames(CC BY 4.0)에서 왔고,
 * 라이선스가 출처 표시를 요구한다. 입력 화면 하단에도 같은 표기가 있다.
 */
@Composable
fun AboutScreen(onOpenPrivacy: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = Tokens.Space.screenHorizontal,
            end = Tokens.Space.screenHorizontal,
            top = Tokens.Space.lg,
            bottom = Tokens.Space.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
    ) {
        item {
            InfoRow(
                label = stringResource(R.string.about_version),
                value = BuildConfig.VERSION_NAME,
            )
        }

        item {
            Column {
                HorizontalDivider(
                    color = LocalPalette.current.Divider,
                    modifier = Modifier.padding(vertical = Tokens.Space.lg),
                )
                SectionHeader(title = stringResource(R.string.about_data_sources))
            }
        }
        item { Paragraph(stringResource(R.string.about_data_sources_body)) }

        item {
            Column {
                HorizontalDivider(
                    color = LocalPalette.current.Divider,
                    modifier = Modifier.padding(vertical = Tokens.Space.lg),
                )
                SectionHeader(title = stringResource(R.string.about_licenses))
            }
        }
        items(LIBRARIES) { library -> LicenseRow(library) }
        item { Paragraph(stringResource(R.string.about_licenses_note)) }

        item {
            Column {
                HorizontalDivider(
                    color = LocalPalette.current.Divider,
                    modifier = Modifier.padding(vertical = Tokens.Space.lg),
                )
            }
        }
        item {
            NavigationRow(
                label = stringResource(R.string.about_privacy),
                onClick = onOpenPrivacy,
            )
        }
    }
}

/**
 * 쓰고 있는 오픈소스와 그 라이선스.
 *
 * **직접 적는 이유**: 목록을 자동으로 만들어 주는 플러그인(oss-licenses 등)이 있지만,
 * 플러그인 하나를 더 들이는 대신 열 줄을 손으로 적는 쪽이 싸다. 의존성을 추가할 때
 * 여기 한 줄을 같이 더하면 된다 — libs.versions.toml과 짝이다.
 *
 * @param license 라이선스 이름. 전문은 [url]에서 본다.
 */
private data class Library(val name: String, @StringRes val licenseRes: Int, val url: String)

private val APACHE_2 = R.string.about_license_apache
private const val APACHE_2_URL = "https://www.apache.org/licenses/LICENSE-2.0"

private const val GOOGLE_TERMS_URL = "https://developers.google.com/terms"

private val LIBRARIES = listOf(
    Library("Jetpack Compose · Material 3", APACHE_2, APACHE_2_URL),
    Library("AndroidX Core · Lifecycle · Activity", APACHE_2, APACHE_2_URL),
    Library("AndroidX Navigation Compose", APACHE_2, APACHE_2_URL),
    Library("AndroidX Credentials", APACHE_2, APACHE_2_URL),
    Library("Kotlin · Kotlin Coroutines", APACHE_2, APACHE_2_URL),
    Library("maps-compose", APACHE_2, APACHE_2_URL),
    Library("Firebase Android SDK (Auth · Firestore)", APACHE_2, APACHE_2_URL),
    Library("Google Identity (googleid)", APACHE_2, APACHE_2_URL),
    Library("Google Maps SDK for Android", R.string.about_google_terms, GOOGLE_TERMS_URL),
    Library("Google Places SDK for Android", R.string.about_google_terms, GOOGLE_TERMS_URL),
)

/** 라이브러리 한 줄. 라이선스 이름을 누르면 전문을 브라우저로 연다. */
@Composable
private fun LicenseRow(library: Library) {
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // 브라우저가 없는 기기도 있다. 예외를 던지면 앱이 죽는다.
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(library.url)))
                }
            }
            .padding(vertical = Tokens.Space.sm),
    ) {
        Text(text = library.name, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(library.licenseRes),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalPalette.current.Point,
        )
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
            color = LocalPalette.current.TextSecondary,
        )
    }
}

/** 눌러서 들어가는 줄. 설정 화면의 같은 모양을 여기서도 쓴다. */
@Composable
private fun NavigationRow(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Tokens.Space.lg),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = LocalPalette.current.TextSecondary,
        modifier = Modifier.padding(top = Tokens.Space.sm),
    )
}