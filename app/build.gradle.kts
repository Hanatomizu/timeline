plugins {
    id("com.android.application")
    // AGP 9.x 已内置 Kotlin 支持，无需显式 apply kotlin-android 插件
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "moe.hanatomizu.timeline"
    // compileSdk 34 用于兼容较新库版本，targetSdk 保持 33 确保 API 33 行为
    compileSdk = 34

    defaultConfig {
        applicationId = "moe.hanatomizu.timeline"
        minSdk = 23
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Android 核心
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Lifecycle + ViewModel（Compose 集成）
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Navigation（Compose）
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room（数据库 + KSP 编译处理器）
    // Room 2.7+ 已将 room-ktx 合并入 room-runtime，并原生支持 KSP2
    implementation("androidx.room:room-runtime:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    // Compose BOM — 统一管理 Compose 库版本
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Coil（图片加载）
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Debug 工具
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
