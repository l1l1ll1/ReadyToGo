import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // google-services.json을 읽어 Firebase 설정을 빌드에 넣는다. 파일이 없으면 빌드가 실패한다.
    alias(libs.plugins.google.services)
}

/**
 * Maps 키는 local.properties에서 읽는다.
 * 키를 소스에 적지 않기 위함이며, local.properties는 형상관리에 올리지 않는다.
 * 키가 없어도 빌드는 되게 둔다 — 지도만 안 뜨고 나머지 화면은 그대로 확인할 수 있다.
 */
val localProperties: Properties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY", "")

/** SerpApi(항공권) 키. 없으면 Mock 항공 데이터로 동작한다. */
val serpApiKey: String = localProperties.getProperty("SERPAPI_KEY", "")

android {
    namespace = "kr.ac.baekseok.readytogo"
    // androidx.core 1.19.0 / lifecycle 2.11.0이 compileSdk 37 이상을 요구한다.
    // targetSdk는 런타임 동작이 바뀌므로 36에 그대로 둔다.
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "kr.ac.baekseok.readytogo"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AndroidManifest의 ${MAPS_API_KEY} 자리에 채워진다.
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey

        // Places SDK는 Manifest가 아니라 코드에서 키를 넘겨야 해서 BuildConfig로도 내보낸다.
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        buildConfigField("String", "SERPAPI_KEY", "\"$serpApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    /**
     * 개인정보 처리 방침 HTML을 저장소의 hosting/ 폴더에서 그대로 읽는다.
     *
     * **복사하지 않고 에셋 폴더로 추가하는 이유**: 같은 파일을 두 곳에 두면 반드시 한쪽만
     * 고치게 된다. hosting/은 나중에 Firebase Hosting에 그대로 올릴 폴더이고,
     * 앱은 같은 파일을 file:///android_asset/privacy.html로 띄운다.
     */
    sourceSets["main"].assets.srcDir(rootProject.file("hosting"))

    testOptions {
        unitTests {
            // 단위 테스트에는 android.util.Log 같은 프레임워크 구현이 없어 호출하면 예외가 난다.
            // 로그 한 줄 때문에 테스트가 깨지는 것을 막는다(기본값을 돌려주게 한다).
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // 앱이 첫 화면을 그릴 때까지 시스템 시작 화면을 붙잡아 둔다.
    // 없으면 Compose 스플래시가 한 프레임도 안 나오고 지나간다(SplashScreen.kt 주석).
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Composable 안에서 viewModel()로 ViewModel을 받기 위해 사용
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    // 화면 이동(입력 → 결과 → 지도)
    implementation(libs.androidx.navigation.compose)
    // Google 지도. maps-compose는 Compose에서 지도를 쓰기 위한 공식 래퍼다.
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)
    // 식당·관광지 검색. 7단계에서 MockPlaceRepository를 대체한다.
    implementation(libs.places)
    // Places 사진을 URL로 받아 그린다(로드맵 5). 캐시·지연 로드를 직접 짜지 않으려고 쓴다.
    // network-okhttp가 따로 있는 이유: coil-compose만으로는 http로 못 받는다.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // 타임라인 항목 끌기(로드맵 6). 자동 스크롤·이동 애니메이션을 직접 짜면 200줄이 넘고,
    // 끌기 중 목록이 재구성될 때 어긋나는 버그까지 떠안게 된다.
    implementation(libs.reorderable)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // 아이콘. material3에 딸려 오지 않아 따로 넣는다.
    // extended는 아이콘이 2천 개지만, R8이 실제로 쓰는 것만 남기므로 APK는 거의 늘지 않는다.
    implementation(libs.androidx.compose.material.icons.extended)

    // 로그인과 저장. BOM이 auth·firestore 버전을 맞춰주므로 개별 버전을 적지 않는다.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Google 계정 선택창. 예전 GoogleSignInClient는 deprecated라 Credential Manager를 쓴다.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}