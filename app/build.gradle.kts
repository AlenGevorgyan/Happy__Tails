plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.app.happytails"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.app.happytails"
        minSdk = 27
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
        sourceCompatibility = JavaVersion.VERSION_20
        targetCompatibility = JavaVersion.VERSION_20
    }
}

dependencies {
    // Firebase BOM for version management
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    // Firebase libraries
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging:24.1.0")

    // Firebase UI
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")

    // Other dependencies
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.karumi:dexter:6.2.3")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Cloudinary
    implementation("com.cloudinary:cloudinary-android:3.0.2")

    // Image loading with Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.activity)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Fragment KTX
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // Unit Testing Libraries
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // CircleImageView for rounded images
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // 🔹 Stripe SDK for Payments
    implementation("com.stripe:stripe-android:20.34.0")

    // 🔹 Retrofit for API Calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // 🔹 JSON Handling (Gson)
    implementation("com.google.code.gson:gson:2.10")

    // OkHttp for making network requests
    implementation("com.squareup.okhttp3:okhttp:4.9.0")

    // Volley for Networking
    implementation("com.android.volley:volley:1.2.1")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.1.0")
}
