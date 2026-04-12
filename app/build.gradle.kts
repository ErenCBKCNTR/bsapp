import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.blind.social"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.blind.social"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }

        fun getSecret(key: String): String {
            val envValue = System.getenv(key)
            if (!envValue.isNullOrBlank()) {
                return "\"${envValue.removePrefix("\"").removeSuffix("\"")}\""
            }

            val propValue = localProperties.getProperty(key)
            if (!propValue.isNullOrBlank()) {
                return "\"${propValue.removePrefix("\"").removeSuffix("\"")}\""
            }

            throw GradleException("Missing required secret: $key. Please set it in local.properties or as an environment variable.")
        }

        buildConfigField("String", "SUPABASE_URL", getSecret("SUPABASE_URL"))
        buildConfigField("String", "SUPABASE_KEY", getSecret("SUPABASE_KEY"))
        buildConfigField("String", "LIVEKIT_URL", getSecret("LIVEKIT_URL"))
        buildConfigField("String", "LIVEKIT_API_KEY", getSecret("LIVEKIT_API_KEY"))
        buildConfigField("String", "LIVEKIT_API_SECRET", getSecret("LIVEKIT_API_SECRET"))
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.9"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:2.5.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")

    // Ktor Client
    implementation("io.ktor:ktor-client-android:2.3.11")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // DataStore for Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // LiveKit
    implementation("io.livekit:livekit-android:2.4.1")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5") {
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-databind")
    }
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}