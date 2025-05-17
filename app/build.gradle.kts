plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.app.happytails"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.app.happytails"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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
            sourceCompatibility = JavaVersion.VERSION_20
            targetCompatibility = JavaVersion.VERSION_20
        }

        packagingOptions {
            resources {
                excludes += "META-INF/DEPENDENCIES"
            }
        }
    }

    dependencies {
        // 1) Firebase BOM (manages versions for all Firebase libs)
        implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

        // 2) Firebase Core + Analytics (if you want analytics)
        // (optional, remove if you don't use Analytics)
        implementation("com.google.firebase:firebase-analytics")

        // 3) Firebase libraries
        implementation("com.google.firebase:firebase-auth")            // Auth
        implementation("com.google.firebase:firebase-firestore")     // Firestore
        implementation("com.google.firebase:firebase-database")        // Realtime Database
        implementation("com.google.firebase:firebase-storage")         // Storage
        implementation("com.google.firebase:firebase-messaging")       // Cloud Messaging
        implementation("com.google.firebase:firebase-functions")       // Cloud Functions client

        // 4) Firebase UI (optional UI bindings for Firestore)
        implementation("com.firebaseui:firebase-ui-firestore:8.0.2")

        // 5) OkHttp for low‑level HTTP
        implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
        implementation("com.squareup.okhttp3:okhttp")
        implementation("com.squareup.okhttp3:logging-interceptor")

        // 6) Retrofit + Gson (higher‑level HTTP + JSON)
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")
        implementation("com.google.code.gson:gson:2.10")

        // 7) Volley (alternative networking library)
        implementation("com.android.volley:volley:1.2.1")

        // 8) Google Auth library (OAuth2 helper, if you need it)
        implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

        // 9) UI & AndroidX
        implementation("androidx.appcompat:appcompat:1.7.0")
        implementation("com.google.android.material:material:1.10.0")
        implementation("androidx.recyclerview:recyclerview:1.3.2")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation("androidx.core:core-ktx:1.12.0")
        implementation("androidx.fragment:fragment-ktx:1.8.5")

        // 10) Image loading
        implementation("com.github.bumptech.glide:glide:4.16.0")
        annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
        implementation("de.hdodenhof:circleimageview:3.1.0")
        implementation("com.cloudinary:cloudinary-android:3.0.2")
        implementation("com.github.denzcoskun:ImageSlideshow:0.1.2")

        // 11) Permissions helper
        implementation("com.karumi:dexter:6.2.3")

        // 13) Google Sign‑In
        implementation("com.google.android.gms:play-services-auth:20.1.0")

        // 14) Testing
        testImplementation("junit:junit:4.13.2")
        androidTestImplementation("androidx.test.ext:junit:1.1.5")
        androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

        //notification
        implementation("me.pushy:sdk:1.0.121")
    }
}
dependencies {
    implementation(libs.work.runtime)
    implementation(libs.security.crypto)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
}
