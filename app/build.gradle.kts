// app/build.gradle (do seu módulo de aplicativo)

plugins {
    alias(libs.plugins.android.application)
    // Plugin do Google Services (Firebase)
    id("com.google.gms.google-services")
    // Se estiver usando Kotlin, adicione:
    // id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.busdb"
    compileSdk = 35 // Verifique se o SDK 35 está realmente instalado e é a versão que você deseja usar.
    // O mais comum atualmente é 34.

    defaultConfig {
        applicationId = "com.example.busdb"
        minSdk = 24
        targetSdk = 35 // Verifique se o SDK 35 está realmente instalado.
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
        // As versões Java 1.8 são amplamente compatíveis.
        // Se estiver usando Kotlin e um plugin mais recente, você pode considerar JavaVersion.VERSION_11
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))

    implementation(libs.appcompat)
    implementation(libs.material)

    implementation(libs.play.services.location)

    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core:1.12.0")

    implementation ("com.google.firebase:firebase-auth:22.3.0")

}