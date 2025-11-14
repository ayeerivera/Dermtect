plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.dermtect"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dermtect"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Trim locales (size win)
        resourceConfigurations += listOf("en", "fil")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk { debugSymbolLevel = "none" }
        }
        debug {
            ndk { debugSymbolLevel = "none" }
        }
    }

    // ✅ Use splits to generate small per-ABI/density APKs
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")   // add "armeabi-v7a" only if you truly need it
            isUniversalApk = false
        }
        // Disable density splits → one APK for this ABI
        density {
            isEnable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    androidResources { noCompress += "tflite" }

    packaging {
        jniLibs { useLegacyPackaging = false }
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    // Firebase BoM (Bill of Materials) to unify Firebase versions
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    // optional push
    // implementation("com.google.firebase:firebase-messaging-ktx")

    //scans
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")


    //Camera
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation ("com.google.guava:guava:32.1.3-jre")

    // Coil
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.5.0")

    // Compose & lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.coil.compose)

    // Google sign-in
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.androidx.foundation)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Core TFLite runtime
    implementation("org.tensorflow:tensorflow-lite:2.17.0")

    // (Optional) NNAPI / GPU delegates – use one that works on your target devices
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.14.0")
    // implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")

    //OSMdroid MAPS
    implementation ("org.osmdroid:osmdroid-android:6.1.18")
    implementation ("com.google.android.gms:play-services-location:21.3.0") // for user location
    implementation ("com.squareup.okhttp3:okhttp:4.12.0") // for Overpass API calls
    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1") // parse JSON
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation ("androidx.datastore:datastore-preferences:1.1.1")

    implementation ("androidx.compose.runtime:runtime-livedata:1.7.0")// or your Compose version


}
