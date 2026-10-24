import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// local.properties (লোকাল বিল্ড) -> Gradle -P/gradle.properties -> Environment variable (CI)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun cfg(key: String): String =
    localProps.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: project.findProperty(key)?.toString()?.takeIf { it.isNotBlank() }
        ?: System.getenv(key)?.takeIf { it.isNotBlank() }
        ?: ""

android {
    namespace = "com.srizwan.task"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.srizwan.task"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SB_URL", "\"${cfg("SB_URL")}\"")
        buildConfigField("String", "SB_KEY", "\"${cfg("SB_KEY")}\"")
        buildConfigField("String", "ADMIN_PHONE", "\"${cfg("ADMIN_PHONE")}\"")
        buildConfigField("String", "ADMIN_WA", "\"${cfg("ADMIN_WA")}\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    jvmToolchain(17)
}

// SB_URL/SB_KEY ছাড়া রিলিজ বিল্ড করলে অ্যাপে "no protocol" error আসে — তাই বিল্ডেই থামিয়ে দিই
gradle.taskGraph.whenReady {
    if (allTasks.any { it.name.contains("Release") } && (cfg("SB_URL").isBlank() || cfg("SB_KEY").isBlank())) {
        throw GradleException("SB_URL / SB_KEY সেট করা নেই। GitHub Secrets এ SB_URL, SB_KEY, ADMIN_PHONE, ADMIN_WA যোগ করুন।")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.9.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
