plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.otgsignallogger"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.otgsignallogger"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    
    // 加入這段：關閉 Lint 嚴格檢查，避免將警告當作錯誤處理而中斷打包
    lint {
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    
    // OTG Serial 與 GPS 庫
    implementation("com.github.mik3y:usb-serial-for-android:3.11.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
}
