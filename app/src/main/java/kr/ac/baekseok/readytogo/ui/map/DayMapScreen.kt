package kr.ac.baekseok.readytogo.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.Lodging
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.Restaurant
import com.google.android.gms.maps.model.LatLng as GmsLatLng
import kr.ac.baekseok.readytogo.data.model.LatLng as AppLatLng

/** 지도가 장소들을 다 담고도 가장자리에 숨 쉴 틈을 주는 여백(px). */
private const val MAP_PADDING_PX = 120

/** 장소가 하나뿐이면 감쌀 영역이 없어 줌 레벨을 직접 정해야 한다. */
private const val SINGLE_PLACE_ZOOM = 14f

/**
 * 로드맵 6단계 — 하루 동선을 지도에 띄운다.
 *
 * 글자 목록으로는 "센소지 다음 스카이트리"가 얼마나 가까운지 알 수 없다.
 * 마커를 순서대로 잇는 선까지 그려서 그날 이동이 합리적인지 눈으로 보게 한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayMapScreen(
    day: DayPlan,
    lodging: Lodging?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("${day.dayNumber}일차 동선") },
                navigationIcon = { TextButton(onClick = onBack) { Text("뒤로") } },
            )
        },
    ) { innerPadding ->
        DayMapContent(
            day = day,
            lodging = lodging,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * 제목 줄 없이 지도만 그린다.
 *
 * 결과 화면의 "지도" 탭이 이걸 그대로 쓴다 — 탭 안에서는 TopAppBar와 뒤로 가기가 이미 있어서
 * [DayMapScreen]을 쓰면 제목이 두 줄로 겹친다. 지도를 그리는 코드는 한 군데만 두어야
 * 마커 색이나 범례를 고칠 때 한쪽만 바뀌는 일이 없다.
 */
@Composable
fun DayMapContent(
    day: DayPlan,
    lodging: Lodging?,
    modifier: Modifier = Modifier,
) {
    val spots = remember(day, lodging) { day.toMapSpots(lodging) }

    Box(modifier = modifier.fillMaxSize()) {
        if (spots.isEmpty()) {
            EmptyMapNotice()
        } else {
            DayMap(spots)
        }
    }
}

@Composable
private fun EmptyMapNotice() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("이 날은 지도에 표시할 장소가 없습니다", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DayMap(spots: List<MapSpot>) {
    val cameraPositionState = rememberCameraPositionState()

    // 장소가 정해지면 전부 화면에 들어오도록 카메라를 맞춘다.
    // 한 곳뿐이면 감쌀 영역이 없어서 bounds 대신 고정 줌을 쓴다.
    LaunchedEffect(spots) {
        val positions = spots.map { it.position.toGms() }
        if (positions.size == 1) {
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(positions.first(), SINGLE_PLACE_ZOOM)
            )
        } else {
            val bounds = LatLngBounds.builder().apply { positions.forEach { include(it) } }.build()
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, MAP_PADDING_PX))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
        ) {
            // 관광지만 순서대로 잇는다. 숙소·식당까지 이으면 선이 뒤엉켜 동선이 안 보인다.
            val routePoints = spots.filter { it.kind == SpotKind.ATTRACTION }.map { it.position.toGms() }
            if (routePoints.size >= 2) {
                Polyline(points = routePoints, width = 8f)
            }

            spots.forEach { spot ->
                Marker(
                    state = MarkerState(position = spot.position.toGms()),
                    title = spot.title,
                    snippet = spot.snippet,
                    icon = BitmapDescriptorFactory.defaultMarker(spot.kind.markerHue),
                )
            }
        }

        MapLegend(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
    }
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("마커 색", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            LegendRow(MARKER_RED, "관광지 (방문 순서)")
            LegendRow(MARKER_AZURE, "숙소")
            LegendRow(MARKER_ORANGE, "식당")
        }
    }
}

/** 지도 마커 색과 맞춘 범례 색. 마커는 색상(hue)만 지정할 수 있어 여기서 같은 톤으로 맞춰 적는다. */
private val MARKER_RED = Color(0xFFD32F2F)
private val MARKER_AZURE = Color(0xFF1976D2)
private val MARKER_ORANGE = Color(0xFFF9A825)

@Composable
private fun LegendRow(color: Color, label: String) {
    Text(
        text = "● $label",
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

/** 지도에 찍을 지점 하나. 모델을 그대로 쓰지 않는 이유는 관광지·숙소·식당을 한 목록으로 다루기 위함이다. */
private data class MapSpot(
    val position: AppLatLng,
    val title: String,
    val snippet: String,
    val kind: SpotKind,
)

private enum class SpotKind(val markerHue: Float) {
    ATTRACTION(BitmapDescriptorFactory.HUE_RED),
    LODGING(BitmapDescriptorFactory.HUE_AZURE),
    RESTAURANT(BitmapDescriptorFactory.HUE_ORANGE),
}

private fun AppLatLng.toGms() = GmsLatLng(latitude, longitude)

/**
 * 하루치 계획을 지도 지점 목록으로 바꾼다.
 *
 * 식당은 "추천"(첫 번째 선택지)만 찍는다. 두 선택지를 다 찍으면 한 끼에 마커가 둘씩,
 * 하루에 여섯 개가 되어 관광지 동선이 묻힌다.
 */
private fun DayPlan.toMapSpots(lodging: Lodging?): List<MapSpot> {
    val attractionSpots = attractions.mapIndexed { index, attraction ->
        MapSpot(
            position = attraction.location,
            title = "${index + 1}. ${attraction.name}",
            snippet = "${attraction.stayMinutes}분 · ★${attraction.rating}",
            kind = SpotKind.ATTRACTION,
        )
    }

    val mealSpots = MealTime.entries.mapNotNull { meal ->
        val restaurant: Restaurant = meals[meal]?.qualityFirst?.item ?: return@mapNotNull null
        MapSpot(
            position = restaurant.location,
            title = "${meal.label} · ${restaurant.name}",
            snippet = "${restaurant.category} · ${"%,d".format(restaurant.priceWon)}원",
            kind = SpotKind.RESTAURANT,
        )
    }

    val lodgingSpot = lodging?.let {
        MapSpot(
            position = it.location,
            title = "숙소 · ${it.name}",
            snippet = "${"%,d".format(it.priceWon)}원 / 1박",
            kind = SpotKind.LODGING,
        )
    }

    return attractionSpots + mealSpots + listOfNotNull(lodgingSpot)
}