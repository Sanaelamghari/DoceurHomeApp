    plugins {
        id("com.android.application")
        id("org.jetbrains.kotlin.android")
        id("com.google.gms.google-services")
}

tasks.whenTaskAdded {
    if (name == "mergeDebugResources") {
        dependsOn("processDebugGoogleServices")
    }
}

android {
    namespace = "com.example.doceurhomeapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.doceurhomeapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
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
}

    dependencies {
        // AndroidX Core
        implementation ("androidx.core:core-ktx:1.12.0")
        implementation ("androidx.appcompat:appcompat:1.6.1")

        // UI Components
        implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation ("androidx.recyclerview:recyclerview:1.3.1")
        implementation ("androidx.viewpager2:viewpager2:1.0.0")
        implementation ("androidx.gridlayout:gridlayout:1.0.0")
        implementation ("androidx.fragment:fragment-ktx:1.6.2")
        implementation ("androidx.customview:customview-poolingcontainer:1.0.0")

        // Material Design (une seule déclaration)
        implementation ("com.google.android.material:material:1.9.0")

        // Firebase (avec BOM)
        implementation ("com.google.firebase:firebase-auth:21.0.1")
        implementation ("com.google.firebase:firebase-firestore:24.0.0")
        implementation (platform("com.google.firebase:firebase-bom:32.7.0"))
        implementation ("com.google.firebase:firebase-auth-ktx")
        implementation ("com.google.firebase:firebase-firestore-ktx")
        implementation ("com.google.firebase:firebase-storage-ktx")
        implementation ("com.google.firebase:firebase-appcheck-playintegrity")
        implementation ("com.google.firebase:firebase-appcheck-ktx")

        // Google Play Services
        implementation ("com.google.android.gms:play-services-auth:20.7.0")
        implementation ("com.google.android.gms:play-services-base:18.2.0")

        // HTTP Client
        implementation ("com.squareup.okhttp3:okhttp:4.9.3")

        // Image Loading
        implementation ("com.github.bumptech.glide:glide:4.16.0")
        annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

        // Database
        implementation ("androidx.room:room-runtime:2.5.2")
        annotationProcessor ("androidx.room:room-compiler:2.5.2")

        // Testing
        testImplementation ("junit:junit:4.13.2")
        androidTestImplementation ("androidx.test.ext:junit:1.1.5")
        androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")
    }
