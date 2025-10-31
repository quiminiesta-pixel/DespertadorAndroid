plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.despertador1"
    compileSdk = 34 // He ajustado esto a 34, que es una versión más estable que 36 (aún en desarrollo)

    defaultConfig {
        applicationId = "com.example.despertador1"
        minSdk = 30
        targetSdk = 34 // Ajustado también a 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8 // Cambiado a 1.8, el estándar recomendado
        targetCompatibility = JavaVersion.VERSION_1_8 // Cambiado a 1.8
    }
    kotlinOptions {
        jvmTarget = "1.8" // Cambiado a 1.8
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- LÍNEA AÑADIDA PARA CORREGIR EL ERROR ---
    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation("com.google.code.gson:gson:2.10.1")


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
