plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 33
    defaultConfig {
        applicationId = "com.vungle.ironsource.app"
        minSdk = 21
        targetSdk = 33
        versionCode = 70000
        versionName = "7.0.0"
        versionNameSuffix = ""

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
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
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        quiet = false
        abortOnError = false

        xmlReport = false

        htmlReport = true
        htmlOutput = file("${project.buildDir}/reports/lint/lint-report.html")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("com.ironsource.sdk:mediationsdk:7.3.1")
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")

    implementation(project(":levelplay-android-adapters"))
//    implementation("com.github.vungle:levelplay-android-adapters:25358ed561")

    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
