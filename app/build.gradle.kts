plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 30
    buildToolsVersion = "30.0.3"

    defaultConfig {
        applicationId = "com.diebietse.webpage.downloader.example"
        minSdk = 21
        targetSdk = 30
        versionCode = 2
        versionName = "0.2.1"
        resConfigs("en")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
    lintOptions {
        isAbortOnError = true
        isWarningsAsErrors = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("com.google.android.material:material:1.3.0")

    implementation("androidx.constraintlayout:constraintlayout:2.0.4")

    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.lifecycle:lifecycle-common:2.3.1")

    implementation("com.afollestad.material-dialogs:input:3.3.0")
    implementation("me.zhanghai.android.materialprogressbar:library:1.6.1")

    implementation(project(":library"))
}