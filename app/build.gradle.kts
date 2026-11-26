plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Cada ejecución de GitHub Actions sube el número; en local usa 1
val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.reposteria.presupuesto"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.reposteria.presupuesto"
        minSdk = 24
        targetSdk = 34
        versionCode = runNumber
        versionName = "1.$runNumber"
    }

    // Misma llave en todas las compilaciones => Android permite actualizar encima
    signingConfigs {
        getByName("debug") {
            storeFile = file("ventas.keystore")
            storePassword = "ventas123"
            keyAlias = "ventas"
            keyPassword = "ventas123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
}
