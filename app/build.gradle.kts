import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ai.seroai.goldentime"
    compileSdk = 35

    defaultConfig {
        applicationId = "ai.seroai.goldentime"
        minSdk = 28 // 차량 보안 요건 (Pleos 문서)
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        // 차량(arm64)·에뮬레이터(x86_64)만 — 지도 네이티브 라이브러리 용량 절감
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        // Pleos Playground 클라이언트 (서버 음성용, local.properties — 저장소에 올리지 않음). 없으면 차량 내 엔진만
        val local = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        buildConfigField("String", "PLEOS_CLIENT_ID", "\"${local.getProperty("pleos.client.id", "")}\"")
        buildConfigField("String", "PLEOS_CLIENT_SECRET", "\"${local.getProperty("pleos.client.secret", "")}\"")
        // 공공데이터포털 서비스키 (응급실 실시간 병상). 없으면 시연 데이터
        buildConfigField("String", "DATA_GO_KEY", "\"${local.getProperty("datago.key", "")}\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // release 서명: 루트 keystore.properties (저장소에 올리지 않음)
    val ksFile = rootProject.file("keystore.properties")
    if (ksFile.exists()) {
        val ks = Properties().apply { ksFile.inputStream().use { load(it) } }
        signingConfigs {
            create("release") {
                storeFile = file(ks.getProperty("storeFile"))
                storePassword = ks.getProperty("storePassword")
                keyAlias = ks.getProperty("keyAlias")
                keyPassword = ks.getProperty("keyPassword")
            }
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
    }
}

dependencies {
    // Pleos SDK (data/vehicle, data/navi의 Pleos* 구현에서 사용)
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    implementation("ai.pleos.playground:FusedLocation:1.1.3")
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
    implementation("ai.pleos.playground:SpeechToText:3.3.1")

    // 실제 지도 (키 불필요 OpenFreeMap 타일). Kotlin 2.0 호환 버전
    implementation("org.maplibre.gl:android-sdk:12.3.1")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
