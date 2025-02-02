plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.closets"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.closets"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation (libs.material.v170)
    implementation(libs.colorpickerview)
    implementation(libs.vanniktech.android.image.cropper)
    implementation (libs.removebg)
    implementation (libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.navigation.testing)
    implementation (libs.androidx.navigation.fragment.ktx.v274)
    implementation (libs.androidx.navigation.ui.ktx.v274)
    testImplementation(libs.junit)
    testImplementation (libs.mockito.core)
    testImplementation (libs.mockito.inline)
    testImplementation (libs.androidx.junit.v113)
    testImplementation(libs.androidx.rules.v140)
    testImplementation (libs.robolectric.v49)
    testImplementation (libs.androidx.core.v150)
    testImplementation (libs.androidx.espresso.core.v351)
    testImplementation (libs.junit)
    testImplementation (libs.robolectric.v4141)
    testImplementation (libs.kotlin.test)
    testImplementation (libs.mockito.inline.v400)
    testImplementation (libs.mockk.v1137)
    testImplementation (libs.byte.buddy)
    testImplementation (libs.byte.buddy.agent)
    androidTestImplementation (libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation (libs.androidx.espresso.core.v340)
    androidTestImplementation (libs.androidx.navigation.testing.v250)
    androidTestImplementation (libs.androidx.junit)
    androidTestImplementation (libs.androidx.rules.v140)
    androidTestImplementation (libs.androidx.runner.v110)
    androidTestImplementation (libs.androidx.navigation.testing.v277)
    androidTestImplementation (libs.androidx.core)
    androidTestImplementation (libs.androidx.fragment.testing)
    androidTestImplementation (libs.androidx.fragment.testing.v150)
    androidTestImplementation (libs.androidx.fragment.testing.v136)
    androidTestUtil (libs.androidx.orchestrator)
}