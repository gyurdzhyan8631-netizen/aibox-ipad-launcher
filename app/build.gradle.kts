plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Номер сборки передаёт GitHub Actions (-PciBuild=N); по нему приложение узнаёт о новых версиях.
val ciBuild = (project.findProperty("ciBuild") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "com.aibox.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aibox.launcher"
        minSdk = 31
        targetSdk = 35
        versionCode = ciBuild
        versionName = "0.6.0-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Постоянный ключ подписи debug-сборок: обновления ставятся поверх без удаления приложения.
    signingConfigs {
        getByName("debug") {
            val keystore = file("debug.keystore")
            if (keystore.exists()) {
                storeFile = keystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
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
    }
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-ui"))
    implementation(project(":launcher"))
    implementation(project(":desktop"))
    implementation(project(":dock"))
    implementation(project(":widgets"))
    implementation(project(":wallpaper"))
    implementation(project(":weather"))
    implementation(project(":control-center"))
    implementation(project(":app-drawer"))
    implementation(project(":settings"))
    implementation(project(":icons"))
    implementation(project(":gestures"))
    implementation(project(":system"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
}
