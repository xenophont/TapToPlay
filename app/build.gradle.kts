import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun localProperty(name: String): String = localProperties.getProperty(name, "")
fun quotedBuildConfig(name: String): String =
    "\"${localProperty(name).replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.example.taptoplay"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.taptoplay"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "ADYEN_ENVIRONMENT", quotedBuildConfig("ADYEN_ENVIRONMENT"))
        buildConfigField("String", "ADYEN_PROFILE_NAME", quotedBuildConfig("ADYEN_PROFILE_NAME"))
        buildConfigField("String", "ADYEN_MERCHANT_ID", quotedBuildConfig("ADYEN_MERCHANT_ID"))
        buildConfigField("String", "ADYEN_STORE_ID", quotedBuildConfig("ADYEN_STORE_ID"))
        buildConfigField("String", "ADYEN_API_KEY", quotedBuildConfig("ADYEN_API_KEY"))
        buildConfigField("String", "ADYEN_CLIENT_KEY", quotedBuildConfig("ADYEN_CLIENT_KEY"))
        buildConfigField("String", "ADYEN_TERMINAL_KEY_IDENTIFIER", quotedBuildConfig("ADYEN_TERMINAL_KEY_IDENTIFIER"))
        buildConfigField("String", "ADYEN_TERMINAL_KEY_VERSION", quotedBuildConfig("ADYEN_TERMINAL_KEY_VERSION"))
        buildConfigField("String", "ADYEN_TERMINAL_PASSPHRASE", quotedBuildConfig("ADYEN_TERMINAL_PASSPHRASE"))
        buildConfigField("String", "ADYEN_CURRENCY", quotedBuildConfig("ADYEN_CURRENCY"))
        buildConfigField("String", "ADYEN_COUNTRY_CODE", quotedBuildConfig("ADYEN_COUNTRY_CODE"))
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
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.zxing.android.embedded)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
