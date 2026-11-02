package kr.ac.baekseok.readytogo.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import com.google.android.gms.maps.model.BitmapDescriptor
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
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.data.model.AirportStop
import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.Lodging
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.PickedPlaces
import kr.ac.baekseok.readytogo.data.model.TimelineEdits
import kr.ac.baekseok.readytogo.domain.plan.DayEnds
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import kr.ac.baekseok.readytogo.domain.plan.toTimeline
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
                title = { Text(stringResource(R.string.map_title, day.dayNumber)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LocalPalette.current.Background),
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.common_back)) }
                },
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
    edits: TimelineEdits = TimelineEdits(),
    picked: PickedPlaces = PickedPlaces(),
    /** 선이 시작하고 끝나는 곳. 보통 숙소, 도착일·출국일은 공항이다([kr.ac.baekseok.readytogo.domain.plan.dayEnds]). */
    ends: DayEnds = DayEnds(lodging?.location, lodging?.location),
    /** 계획의 공항. 이날 선의 시작이나 끝일 때만 마커를 찍는다. */
    airport: AirportStop? = null,
    /** 여행 중 모드에서 "했음"으로 체크한 항목 키. 마커만 흐리게 하고 번호와 선은 그대로 둔다. */
    doneKeys: Set<String> = emptySet(),
) {
    val texts = rememberMapSpotTexts()

    // **일정표 탭과 같은 타임라인을 쓴다.** 전에는 원본 계획을 그대로 찍어서 지운 곳이
    // 지도에 남고, 끌어서 바꾼 순서와 바꿔 끼운 장소가 반영되지 않았다.
    // 순서·교체·삭제 규칙은 toTimeline 한 곳에만 두어야 두 화면이 어긋나지 않는다.
    val items = remember(day, edits, picked) {
        day.toTimeline(
            deletedKeys = edits.deletedIn(day.dayNumber),
            pickedMeals = edits.pickedIn(day.dayNumber),
            customOrder = edits.orderIn(day.dayNumber),
            pickedAttractions = edits.pickedAttractionsIn(day.dayNumber),
            picked = picked,
        ).map { it.item }
    }
    // 공항은 이날 선이 거기서 시작하거나 끝날 때만 찍는다. 중간 날에 찍으면 가지도 않는 곳이 보인다.
    val airportToday = airport?.takeIf { it.location == ends.start || it.location == ends.end }
    val spots = remember(items, lodging, airportToday, texts, doneKeys) {
        // 공항은 펼치기에서 뺀다. 수십 km 떨어진 점이 끼면 겹침 기준(일정 폭의 비율)이 커져
        // 시내의 멀쩡한 마커까지 밀어낸다.
        items.toMapSpots(lodging, texts, doneKeys).spreadOverlapping() +
            listOfNotNull(airportToday?.toMapSpot(texts))
    }
    val route = remember(items, ends) {
        dayRoute(ends.start, ends.end, items.map { it.location })
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (spots.isEmpty()) {
            EmptyMapNotice()
        } else {
            DayMap(spots, route)
        }
    }
}

/**
 * 그날 동선 선이 지나갈 좌표. **시작점 → 일정표 순서대로 → 끝점**.
 * 시작·끝은 보통 숙소이고, 도착일은 공항에서 시작해 출국일은 공항에서 끝난다.
 *
 * - 좌표가 없는 항목(추천 못 찾은 끼니)은 건너뛰고 다음 항목으로 바로 잇는다.
 *   거기서 선을 끊으면 끼니 하나 비었다고 하루 동선이 두 동강 난다.
 * - 장소가 하나도 없으면 숙소(또는 공항)만 오가는 선이 되므로 아무것도 그리지 않는다.
 * - 마커를 펼치기 전의 원래 좌표·순서로 만든다. 펼치기는 묶음 단위로 목록을 다시 늘어놓아
 *   순서가 섞이기 때문이다(전에는 그 때문에 1→3→2로 이어지기도 했다).
 */
internal fun dayRoute(start: AppLatLng?, end: AppLatLng?, stops: List<AppLatLng?>): List<AppLatLng> {
    val places = stops.filterNotNull()
    if (places.isEmpty()) return emptyList()
    return listOfNotNull(start) + places + listOfNotNull(end)
}

@Composable
private fun EmptyMapNotice() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.map_empty),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DayMap(spots: List<MapSpot>, route: List<AppLatLng>) {
    val cameraPositionState = rememberCameraPositionState()
    val density = LocalDensity.current.density

    // 범례 카드의 실제 높이. 지도 아래쪽을 이만큼 비워 두면 카메라가 그 위 영역에만 장소를 담는다.
    // 고정값으로 두지 않는 이유: 글자 크기 설정(접근성)과 언어에 따라 카드 높이가 달라진다.
    var legendHeightPx by remember { mutableIntStateOf(0) }
    val legendSpace = with(LocalDensity.current) { legendHeightPx.toDp() } + LEGEND_MARGIN

    // 장소가 정해지면 전부 화면에 들어오도록 카메라를 맞춘다.
    // 한 곳뿐이면 감쌀 영역이 없어서 bounds 대신 고정 줌을 쓴다.
    // 범례 높이가 재어지면 다시 맞춘다 — 첫 프레임에는 높이가 0이라 마커가 범례 밑에 깔린다.
    //
    // 공항은 카메라 범위에서 뺀다. 나리타는 도심에서 60km라 넣으면 도착일·출국일 지도가 도시 전체로
    // 줄어 정작 그날 동선이 안 보인다. 선은 화면 밖 공항 쪽으로 이어지므로 방향은 알 수 있다.
    LaunchedEffect(spots, legendHeightPx) {
        val positions = spots
            .filter { it.kind != SpotKind.AIRPORT }
            .ifEmpty { spots }
            .map { it.position.toGms() }
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
            // 범례가 마커를 가리던 문제(1일차 식당 두 곳이 범례 밑에 숨었다).
            contentPadding = PaddingValues(bottom = legendSpace),
        ) {
            // 두 점 이상이어야 선이 된다. 숙소가 있으면 관광지 한 곳뿐인 도착일에도 왕복 선이 생긴다.
            if (route.size >= 2) {
                Polyline(points = route.map { it.toGms() }, width = 8f)
            }

            spots.forEach { spot ->
                Marker(
                    // 겹친 마커는 살짝 옮겨 찍는다. 선과 카메라는 위에서 원래 좌표를 썼다.
                    state = MarkerState(position = spot.displayPosition.toGms()),
                    title = spot.title,
                    snippet = spot.snippet,
                    // 관광지는 방문 순서 번호를 마커 안에 쓴다. 범례에 "방문 순서"라 적어 놓고 번호가 없으면
                    // 선만 보고 순서를 짐작해야 했다. 끼니·숙소·공항은 순서가 정해진 자리라 기본 핀 그대로.
                    icon = spot.number?.let { numberedPin(it, density) }
                        ?: BitmapDescriptorFactory.defaultMarker(spot.kind.markerHue),
                    alpha = if (spot.done) DONE_MARKER_ALPHA else 1f,
                )
            }
        }

        // 공항이 지금 화면에 보이는지. 카메라에서 공항을 빼서 대개 화면 밖인데, 범례에 "공항"만 있고
        // 마커가 안 보이면 빠뜨린 것처럼 보인다. 하네다처럼 시내에 가까우면 화면 안에 들어오기도 하고,
        // 사용자가 지도를 끌어 공항을 보면 표시가 사라져야 하므로 고정값이 아니라 보이는 영역으로 판단한다.
        val airportSpot = spots.firstOrNull { it.kind == SpotKind.AIRPORT }
        // projection은 Compose 상태가 아니라 읽기만 해서는 지도를 움직여도 다시 계산되지 않는다
        // (실제로 끌어서 나리타를 화면에 넣어도 "화면 밖"이 남았다). 상태인 position·isMoving을 키로 삼아
        // 카메라가 움직이거나 멈출 때마다 보이는 영역을 다시 읽는다.
        val cameraPosition = cameraPositionState.position
        val cameraMoving = cameraPositionState.isMoving
        val visibleBounds = remember(cameraPosition, cameraMoving) {
            cameraPositionState.projection?.visibleRegion?.latLngBounds
        }
        val airportOffScreen = airportSpot != null &&
            visibleBounds?.contains(airportSpot.position.toGms()) != true

        MapLegend(
            showAirport = airportSpot != null,
            airportOffScreen = airportOffScreen,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(LEGEND_MARGIN)
                .onSizeChanged { legendHeightPx = it.height },
        )
    }
}

/** 범례 카드와 지도 가장자리 사이 여백. 지도 아래쪽을 비울 때도 같은 값을 더한다. */
private val LEGEND_MARGIN = 12.dp

/**
 * @param showAirport 공항 마커가 있는 날(도착일·출국일)만 범례에 공항을 넣는다.
 * @param airportOffScreen 공항이 지금 화면 밖이면 "화면 밖"을 붙인다. 선이 그쪽으로 나가는 이유가 된다.
 */
@Composable
private fun MapLegend(showAirport: Boolean, airportOffScreen: Boolean, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.map_legend_title),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            LegendRow(MARKER_RED, stringResource(R.string.map_legend_attraction))
            LegendRow(MARKER_AZURE, stringResource(R.string.map_legend_lodging))
            LegendRow(MARKER_ORANGE, stringResource(R.string.map_legend_restaurant))
            if (showAirport) {
                LegendRow(
                    MARKER_VIOLET,
                    stringResource(
                        if (airportOffScreen) R.string.map_legend_airport_off_screen else R.string.map_legend_airport,
                    ),
                )
            }
        }
    }
}

/** 지도 마커 색과 맞춘 범례 색. 마커는 색상(hue)만 지정할 수 있어 여기서 같은 톤으로 맞춰 적는다. */
private val MARKER_RED = Color(0xFFD32F2F)
private val MARKER_AZURE = Color(0xFF1976D2)
private val MARKER_ORANGE = Color(0xFFF9A825)
private val MARKER_VIOLET = Color(0xFF7B1FA2)

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
    /** 관광지의 그날 방문 순서(1부터). 일정표 순서를 따르므로 끌어서 바꾸면 번호도 바뀐다. 다른 지점은 null. */
    val number: Int? = null,
    /** 여행 중 모드에서 체크한 곳. 흐리게 찍는다. */
    val done: Boolean = false,
)

/** 체크한 곳의 마커 투명도. 지워진 듯 사라지면 안 되고, 아직 갈 곳과는 한눈에 갈려야 한다. */
private const val DONE_MARKER_ALPHA = 0.5f

/** 번호 핀의 크기(dp). 기본 핀과 비슷하게 맞춰 섞여 있어도 튀지 않게 한다. */
private const val PIN_WIDTH_DP = 30f
private const val PIN_HEIGHT_DP = 40f
private const val PIN_TEXT_DP = 14f
private const val PIN_STROKE_DP = 2f

/**
 * 번호를 쓴 관광지 핀. 기본 핀은 색만 바꿀 수 있어서 직접 그린다 — 원 머리 + 아래로 뾰족한 꼬리.
 *
 * 하루 관광지가 많아야 4~5곳이라 그때그때 그려도 되지만, 화면이 다시 그려질 때마다 비트맵을
 * 새로 만들지 않도록 (번호, 밀도)로 캐시한다. 흐리게는 Marker의 alpha가 맡아 비트맵은 하나다.
 */
private fun numberedPin(number: Int, density: Float): BitmapDescriptor =
    pinCache.getOrPut(number to density) {
        val width = (PIN_WIDTH_DP * density).toInt()
        val height = (PIN_HEIGHT_DP * density).toInt()
        val stroke = PIN_STROKE_DP * density
        val radius = width / 2f - stroke
        val cx = width / 2f
        val cy = radius + stroke

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // 머리 원과 꼬리 삼각형을 한 경로로 — 따로 칠하면 이음매에 테두리 선이 비친다.
        val path = Path().apply {
            addCircle(cx, cy, radius, Path.Direction.CW)
            moveTo(cx - radius * PIN_TAIL_RATIO, cy + radius * PIN_TAIL_RATIO)
            lineTo(cx, height - stroke)
            lineTo(cx + radius * PIN_TAIL_RATIO, cy + radius * PIN_TAIL_RATIO)
            close()
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MARKER_RED.toArgb() }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = android.graphics.Color.WHITE
        }
        canvas.drawPath(path, fill)
        canvas.drawPath(path, border)

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = PIN_TEXT_DP * density
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        // 글자 높이의 가운데를 원 중심에 맞춘다(기준선은 글자 아래쪽이라 그대로 두면 위로 뜬다).
        val baseline = cy - (text.descent() + text.ascent()) / 2
        canvas.drawText(number.toString(), cx, baseline, text)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    }

/** 꼬리가 원에서 갈라지는 위치(반지름 대비). 0.7이면 원 아래쪽 옆구리에서 꼬리가 시작된다. */
private const val PIN_TAIL_RATIO = 0.7f

private val pinCache = mutableMapOf<Pair<Int, Float>, BitmapDescriptor>()

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
    AIRPORT(BitmapDescriptorFactory.HUE_VIOLET),
}

private fun AppLatLng.toGms() = GmsLatLng(latitude, longitude)

/**
 * 편집이 반영된 하루 타임라인을 지도 지점 목록으로 바꾼다.
 *
 * 식당은 지금 일정표에 보이는 곳(고른 곳, 없으면 추천)만 찍는다. 두 선택지를 다 찍으면
 * 한 끼에 마커가 둘씩 생겨 동선이 묻힌다. 관광지 번호는 일정표에 나오는 차례를 따른다.
 */
private fun List<TimelineItem>.toMapSpots(
    lodging: Lodging?,
    texts: MapSpotTexts,
    doneKeys: Set<String>,
): List<MapSpot> {
    var visitNumber = 0
    val itemSpots = mapNotNull { item ->
        when (item) {
            is TimelineItem.Visit -> {
                visitNumber++
                val attraction = item.attraction
                MapSpot(
                    position = attraction.location,
                    title = "$visitNumber. ${attraction.name}",
                    snippet = texts.attractionSnippet
                        .format(attraction.stayMinutes, attraction.rating.toString()),
                    kind = SpotKind.ATTRACTION,
                    number = visitNumber,
                    done = item.key in doneKeys,
                )
            }
            is TimelineItem.Meal -> {
                val restaurant = item.restaurant
                MapSpot(
                    position = restaurant.location,
                    title = "${texts.mealLabels[item.meal]} · ${restaurant.name}",
                    snippet = texts.restaurantSnippet
                        .format(restaurant.category, texts.money(restaurant.priceWon)),
                    kind = SpotKind.RESTAURANT,
                    done = item.key in doneKeys,
                )
            }
            // 장소가 아니라 찍을 좌표가 없다.
            is TimelineItem.NoMeal -> null
        }
    }

    val lodgingSpot = lodging?.let {
        MapSpot(
            position = it.location,
            title = texts.lodgingTitle.format(it.name),
            snippet = texts.lodgingSnippet.format(texts.money(it.priceWon)),
            kind = SpotKind.LODGING,
        )
    }

    return itemSpots + listOfNotNull(lodgingSpot)
}

private fun AirportStop.toMapSpot(texts: MapSpotTexts) = MapSpot(
    position = location,
    title = texts.airportTitle.format(code),
    snippet = "",
    kind = SpotKind.AIRPORT,
)

/**
 * 마커 말풍선에 들어갈 글자.
 *
 * **미리 풀어서 들고 다니는 이유**: 지점 목록은 `remember` 안에서 만드는데, 그 블록은
 * Composable이 아니라 `stringResource`를 부를 수 없다. 패턴만 먼저 꺼내 두고
 * 숫자를 끼우는 일은 목록을 만들 때 한다.
 */
private data class MapSpotTexts(
    val mealLabels: Map<MealTime, String>,
    val attractionSnippet: String,
    val restaurantSnippet: String,
    val lodgingTitle: String,
    val lodgingSnippet: String,
    val airportTitle: String,
    /** 금액 표기도 언어마다 다르다("12,000원" / "KRW 12,000"). */
    val money: (Long) -> String,
)

@Composable
private fun rememberMapSpotTexts(): MapSpotTexts {
    val mealLabels = MealTime.entries.associateWith { stringResource(it.labelRes) }
    val attractionSnippet = stringResource(R.string.map_attraction_snippet)
    val restaurantSnippet = stringResource(R.string.map_restaurant_snippet)
    val lodgingTitle = stringResource(R.string.map_lodging_title)
    val lodgingSnippet = stringResource(R.string.map_lodging_snippet)
    val airportTitle = stringResource(R.string.map_airport_title)
    val moneyPattern = stringResource(R.string.fmt_money)

    return remember(
        mealLabels,
        attractionSnippet,
        restaurantSnippet,
        lodgingTitle,
        lodgingSnippet,
        airportTitle,
        moneyPattern,
    ) {
        MapSpotTexts(
            mealLabels = mealLabels,
            attractionSnippet = attractionSnippet,
            restaurantSnippet = restaurantSnippet,
            lodgingTitle = lodgingTitle,
            lodgingSnippet = lodgingSnippet,
            airportTitle = airportTitle,
            money = { moneyPattern.format("%,d".format(it)) },
        )
    }
}