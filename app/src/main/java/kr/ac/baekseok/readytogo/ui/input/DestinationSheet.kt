package kr.ac.baekseok.readytogo.ui.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import kr.ac.baekseok.readytogo.data.model.Destination
import kr.ac.baekseok.readytogo.ui.component.EmptyState
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/** 시트가 무엇을 고르는 중인지. 제목과 목록이 갈린다. */
enum class DestinationTarget(val title: String, val hint: String) {
    ORIGIN("어디서 출발하세요?", "공항 이름으로 찾기"),
    DESTINATION("어디로 가세요?", "도시·나라·공항 코드로 찾기"),
}

/**
 * 출발지·목적지를 고르는 바닥 시트.
 *
 * **검색과 둘러보기를 같이 둔다.** 가고 싶은 도시를 이미 아는 사람은 검색이 빠르고,
 * 아직 안 정한 사람은 나라별 목록을 훑어야 한다. 한쪽만 두면 다른 쪽이 막힌다.
 *
 * 출발지는 한국 공항 일곱 개뿐이라 검색창 없이 목록만 보여준다.
 *
 * @param onCountrySelected 나라를 눌렀을 때. 그 나라의 도시 목록을 채워야 한다.
 * @param selectedCountry 지금 펼쳐 둔 나라. null이면 나라 목록을 보여준다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationSheet(
    target: DestinationTarget,
    options: InputOptions,
    search: DestinationSearchState,
    selectedCountry: String?,
    onQueryChange: (String) -> Unit,
    onCountrySelected: (String) -> Unit,
    onBackToCountries: () -> Unit,
    onPicked: (Destination) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Tokens.Palette.Background,
        shape = Tokens.Radius.sheet,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Tokens.Space.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = target.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "닫기")
                }
            }

            if (target == DestinationTarget.ORIGIN) {
                DestinationList(
                    destinations = options.origins,
                    icon = Icons.Outlined.FlightTakeoff,
                    onPicked = onPicked,
                )
                return@Column
            }

            OutlinedTextField(
                value = search.query,
                onValueChange = onQueryChange,
                label = { Text(target.hint) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "검색어 지우기")
                        }
                    }
                },
                singleLine = true,
                shape = Tokens.Radius.button,
                modifier = Modifier.fillMaxWidth(),
            )

            when {
                // 검색 중이면 결과만 보여준다. 둘러보기 목록이 같이 있으면 뭐가 결과인지 모른다.
                search.query.isNotBlank() -> {
                    if (search.results.isEmpty()) {
                        EmptyState(
                            title = "찾는 곳이 없습니다",
                            description = "도시 이름이나 공항 코드(예: HND)로 다시 찾아보세요.",
                        )
                    } else {
                        DestinationList(
                            destinations = search.results,
                            icon = Icons.Outlined.LocationCity,
                            showCountry = true,
                            query = search.query,
                            onPicked = onPicked,
                        )
                    }
                }

                // 나라를 고르면 그 나라 도시로, 아니면 나라 목록으로.
                selectedCountry != null -> Column(
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
                ) {
                    TextButton(onClick = onBackToCountries) { Text("← 나라 다시 고르기") }
                    DestinationList(
                        destinations = options.cities,
                        icon = Icons.Outlined.LocationCity,
                        onPicked = onPicked,
                    )
                }

                else -> CountryList(
                    countries = options.countries,
                    onPicked = onCountrySelected,
                )
            }
        }
    }
}

@Composable
private fun DestinationList(
    destinations: List<Destination>,
    icon: ImageVector,
    showCountry: Boolean = false,
    /** 검색어. 공항 코드로 걸린 결과에 **그 코드**를 보여주기 위해 쓴다. */
    query: String = "",
    onPicked: (Destination) -> Unit,
) {
    LazyColumn(
        // 시트가 화면을 다 덮지 않도록 최대 높이를 둔다. 아래가 비면 시트가 떠 있는 게 보인다.
        modifier = Modifier.heightIn(max = Tokens.Size.sheetListMaxHeight),
        contentPadding = PaddingValues(bottom = Tokens.Space.xxl),
    ) {
        items(destinations, key = { "${it.country}-${it.city}" }) { destination ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Space.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPicked(destination) }
                    .padding(vertical = Tokens.Space.md),
            ) {
                Icon(icon, contentDescription = null, tint = Tokens.Palette.TextSecondary)
                Column(modifier = Modifier.weight(1f)) {
                    // 도시 이름만 큰 줄에 둔다. 공항 코드를 뒤에 붙이면
                    // "코타쿠알라무다(Kota Kuala Muda) (PEN)"처럼 괄호가 두 번 겹친다
                    // — 한글 표기가 애매한 도시는 이름 안에 이미 영문이 병기돼 있다.
                    Text(
                        text = destination.city,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // 공항 코드는 아랫줄로. 검색 결과에서는 나라도 같이 보여줘야
                    // 동명 도시를 구분할 수 있다.
                    Text(
                        text = destination.subtitleFor(query, showCountry),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Tokens.Palette.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * 도시 이름 아랫줄에 적을 것.
 *
 * 공항 코드로 검색했으면 **그 코드를 앞세운다.** 가장 가까운 공항을 그대로 쓰면
 * "HND"를 찾았는데 "NRT"가 나와 왜 걸렸는지 알 수 없다 — 가시와는 두 공항을 다 쓴다.
 */
private fun Destination.subtitleFor(query: String, showCountry: Boolean): String {
    val matched = airportCodes.firstOrNull { it.equals(query.trim(), ignoreCase = true) }
    val code = matched ?: airportCodes.firstOrNull()

    return listOfNotNull(country.takeIf { showCountry }, code).joinToString(" · ")
}

@Composable
private fun CountryList(countries: List<String>, onPicked: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.heightIn(max = Tokens.Size.sheetListMaxHeight),
        contentPadding = PaddingValues(bottom = Tokens.Space.xxl),
    ) {
        items(countries, key = { it }) { country ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPicked(country) }
                    .padding(vertical = Tokens.Space.lg),
            ) {
                Text(
                    text = country,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Tokens.Palette.TextSecondary,
                )
            }
        }
    }
}
