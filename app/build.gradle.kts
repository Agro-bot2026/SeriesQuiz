plugins {
    id("com.android.application")
}

android {
    namespace = "com.seriesquiz"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.seriesquiz"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.webkit:webkit:1.9.0")
    implementation("com.google.android.gms:play-services-ads:23.0.0")
}
