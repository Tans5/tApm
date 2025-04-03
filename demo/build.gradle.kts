plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.tans.tapm.demo"
    compileSdk = properties["ANDROID_COMPILE_SDK"].toString().toInt()

    defaultConfig {
        applicationId = "com.tans.tapm.demo"
        minSdk = properties["ANDROID_MIN_SDK"].toString().toInt()
        targetSdk = properties["ANDROID_COMPILE_SDK"].toString().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            packaging {
                jniLibs {
                    keepDebugSymbols += listOf("*/arm64-v8a/*.so", "*/armeabi-v7a/*.so", "*/x86/*.so", "*/x86_64/*.so")
                }
            }
        }
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding {
            enable = true
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.core.jvm)
    implementation(libs.coroutines.android)

    implementation(libs.tuiutils)
    implementation(libs.tlog)
    implementation(libs.tlrucatch)
    implementation(project(":tapm-core"))
    implementation(project(":tapm-breakpad"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}