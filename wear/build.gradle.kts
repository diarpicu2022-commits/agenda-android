// CampusWatch: el acompañante de Agenda en Wear OS (sistema de diseño, 03-campuswatch.md).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dpinta.agenda.wear"
    compileSdk = 37

    defaultConfig {
        // Mismo applicationId que el teléfono: la capa de datos de Wear OS solo une apps con el mismo paquete y firma.
        applicationId = "com.dpinta.agenda"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures { compose = true }

    lint {
        abortOnError = true
    }
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.androidx.wear)

    implementation(libs.play.services.wearable)
    implementation(libs.coroutines.play.services)

    implementation(libs.wear.tiles)
    implementation("androidx.concurrent:concurrent-futures:1.3.0")
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.expression)
    implementation(libs.wear.complications.data.source)

    testImplementation(libs.junit)
}
