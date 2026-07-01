import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val properties = Properties()

properties.load(rootProject.file("local.properties").inputStream())


android {
    namespace = "com.example.quanlychitieu"
    compileSdk = 35 // NÂNG LÊN 36 ĐỂ SỬA LỖI COMPILER

    defaultConfig {
        applicationId = "com.example.quanlychitieu"
        minSdk = 24
        targetSdk = 35 // NÂNG LÊN 36 ĐỂ ĐỒNG BỘ
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${properties.getProperty("GEMINI_API_KEY", "")}\""
        )
    }

    buildFeatures {
        buildConfig = true
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

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.mlkit.text.recognition.common)
    implementation(libs.material)
    implementation(libs.firebase.database)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Thư viện Firebase đám mây
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    /// QR Scanner
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Thư viện vẽ biểu đồ MPAndroidChart
    implementation(libs.mpandroidchart)

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

}