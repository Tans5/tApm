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

    signingConfigs {
        val debugConfig = this.getByName("debug")
        with(debugConfig) {
            storeFile = File(projectDir, "debugkey${File.separator}debug.jks")
            storePassword = "123456"
            keyAlias = "key0"
            keyPassword = "123456"
        }
    }

//    packaging {
//        jniLibs {
//            keepDebugSymbols += listOf("*/arm64-v8a/*.so", "*/armeabi-v7a/*.so", "*/x86/*.so", "*/x86_64/*.so")
//        }
//    }

    buildTypes {
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.findByName("debug")
        }
        release {
            multiDexEnabled = true
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        jvmToolchain(11)
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

    implementation(libs.okhttp3)
    implementation(libs.okhttp3.logging.interceptor)

    implementation(libs.tuiutils)
    implementation(libs.tlog)
    implementation(libs.tlrucache)

    implementation(project(":tapm-core"))
    implementation(project(":tapm-autoinit")) {
        exclude("com.github.tans5", "tapm-core")
    }
    implementation(project(":tapm-log")) {
        exclude("com.github.tans5", "tapm-core")
    }
    implementation(project(":tapm-breakpad")) {
        exclude("com.github.tans5", "tapm-core")
        exclude("com.github.tans5", "tapm-autoinit")
    }

//    implementation(libs.tapm.core)
//    implementation(libs.tapm.autoinit)
//    implementation(libs.tapm.log)
//    implementation(libs.tapm.breakpad)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}