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
import androidx.compose.ui.res.stringResource
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
import kr.ac.baekseok.readytogo.domain.recommend.distanceKm
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
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
    // 끼니 이름은 지금 언어로 미리 풀어서 넘긴다 — remember 블록 안에서는 리소스를 읽을 수 없다.
    val mealLabels = MealTime.entries.associateWith { stringResource(it.labelRes) }
    val spots = remember(day, lodging, mealLabels) {
        day.toMapSpots(lodging, mealLabels).spreadOverlapping()
    }

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
                    // 겹친 마커는 살짝 옮겨 찍는다. 선과 카메라는 위에서 원래 좌표를 썼다.
                    state = MarkerState(position = spot.displayPosition.toGms()),
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

/**
 * 지도에 찍을 지점 하나. 모델을 그대로 쓰지 않는 이유는 관광지·숙소·식당을 한 목록으로 다루기 위함이다.
 *
 * @param position 실제 좌표. 동선 선과 카메라 범위는 이 값을 쓴다.
 * @param displayPosition 마커를 실제로 찍을 좌표. 겹치는 마커를 펼칠 때만 [position]과 달라진다.
 */
internal data class MapSpot(
    val position: AppLatLng,
    val title: String,
    val snippet: String,
    val kind: SpotKind,
    val displayPosition: AppLatLng = position,
)

/**
 * 겹치는 마커를 작은 원으로 펼친다.
 *
 * **왜 필요한가**: 식당과 관광지가 같은 건물이거나 100m 안에 있으면 마커가 완전히 포개져
 * 하나만 보인다. 실제로 우에노에서 주황 마커 둘이 겹쳐 하나가 사라졌다.
 * Google Maps SDK는 겹친 마커를 알아서 떨어뜨려 주지 않는다.
 *
 * **왜 클러스터링이 아닌가**: 하루 마커가 많아야 7개(관광지 3~4 + 식당 3 + 숙소 1)다.
 * 클러스터링은 수십·수백 개를 묶을 때 쓰는 것이고, 7개에 쓰면 오히려 묶음 안에 숨어
 * 무엇이 있는지 더 안 보인다. 라이브러리도 하나 늘어난다.
 *
 * **기준 거리를 그날 일정 크기에서 뽑는 이유**: 카메라가 그날 장소를 다 담도록 맞춰지므로
 * 확대 비율이 날마다 다르다. 고정 미터값을 쓰면 좁은 날에는 멀쩡한 마커까지 밀어내고,
 * 넓은 날에는 겹친 걸 못 잡는다. 일정 전체 폭의 일정 비율로 잡아야 어느 날이든 비슷하게 보인다.
 *
 * 마커를 실제 위치에서 옮기는 것이므로 오차가 생긴다. 그래서 펼치는 반경을 겹침 기준의
 * 절반 남짓으로 작게 두고, 동선 선과 카메라는 원래 좌표를 그대로 쓴다.
 */
internal fun List<MapSpot>.spreadOverlapping(): List<MapSpot> {
    if (size < 2) return this

    // 그날 일정이 얼마나 넓게 퍼져 있는지. 가장 먼 두 지점 사이 거리를 폭으로 본다.
    val span = flatMap { a -> map { b -> distanceKm(a.position, b.position) } }.max()
    val thresholdKm = (span * OVERLAP_SPAN_RATIO)
        .coerceIn(MIN_OVERLAP_KM, MAX_OVERLAP_KM)

    // 기준 거리 안에 있는 것끼리 묶는다. 첫 지점이 그 묶음의 중심이 된다.
    val groups = mutableListOf<MutableList<MapSpot>>()
    forEach { spot ->
        val group = groups.firstOrNull {
            distanceKm(it.first().position, spot.position) <= thresholdKm
        }
        if (group != null) group += spot else groups += mutableListOf(spot)
    }

    return groups.flatMap { group ->
        if (group.size == 1) return@flatMap group

        // 묶음 중심을 기준으로 원 위에 고르게 배치한다.
        val centerLat = group.map { it.position.latitude }.average()
        val centerLon = group.map { it.position.longitude }.average()
        val radiusKm = thresholdKm * SPREAD_RADIUS_RATIO

        group.mapIndexed { index, spot ->
            val angle = 2 * PI * index / group.size
            spot.copy(
                displayPosition = offsetBy(
                    latitude = centerLat,
                    longitude = centerLon,
                    northKm = radiusKm * cos(angle),
                    eastKm = radiusKm * sin(angle),
                ),
            )
        }
    }
}

/**
 * 좌표를 북쪽·동쪽으로 몇 km 옮긴다.
 *
 * 경도 1도의 거리는 위도에 따라 달라진다(적도에서 111km, 극지방에서 0). 그래서 경도 변환에만
 * cos(위도)를 곱한다. 수백 미터 범위라 지구를 평면으로 봐도 오차가 보이지 않는다.
 */
private fun offsetBy(
    latitude: Double,
    longitude: Double,
    northKm: Double,
    eastKm: Double,
): AppLatLng {
    val latDegrees = northKm / KM_PER_DEGREE
    val lonDegrees = eastKm / (KM_PER_DEGREE * cos(Math.toRadians(latitude)))
    return AppLatLng(latitude + latDegrees, longitude + lonDegrees)
}

/** 일정 폭의 이 비율 안에 있으면 겹친 것으로 본다. */
private const val OVERLAP_SPAN_RATIO = 0.04

/**
 * 겹침 기준의 하한·상한.
 * 일정이 한 건물에 모인 날에도 최소한은 떨어뜨리고, 하루에 수십 km를 도는 날에
 * 1km 떨어진 곳까지 겹쳤다고 보지 않게 한다.
 */
private const val MIN_OVERLAP_KM = 0.05
private const val MAX_OVERLAP_KM = 0.4

/** 펼치는 반경은 겹침 기준보다 작아야 한다. 그러지 않으면 펼친 것들이 다시 겹친다. */
private const val SPREAD_RADIUS_RATIO = 0.6

/** 위도 1도의 거리(km). 경도는 여기에 cos(위도)를 곱한다. */
private const val KM_PER_DEGREE = 111.32

internal enum class SpotKind(val markerHue: Float) {
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
private fun DayPlan.toMapSpots(
    lodging: Lodging?,
    mealLabels: Map<MealTime, String>,
): List<MapSpot> {
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
            title = "${mealLabels[meal]} · ${restaurant.name}",
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