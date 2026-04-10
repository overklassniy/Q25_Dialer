import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.overklassniy.q25.dialer"
    compileSdk = libs.versions.app.build.compileSDKVersion.get().toInt()

    defaultConfig {
        applicationId = "com.overklassniy.q25.dialer"
        minSdk = libs.versions.app.build.minimumSDK.get().toInt()
        targetSdk = libs.versions.app.build.targetSDK.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        // Read DSN from local.properties and XOR-encode for obfuscation
        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { stream -> localProps.load(stream) }
        }
        val dsn: String = localProps.getProperty("SENTRY_DSN") ?: ""
        val xorKey = "com.overklassniy.q25.dialer"
        val xorBytes = ByteArray(dsn.length) { i ->
            (dsn[i].code xor xorKey[i % xorKey.length].code).toByte()
        }
        val encoded: String = Base64.getEncoder().encodeToString(xorBytes)
        buildConfigField("String", "SENTRY_DSN_ENCODED", "\"$encoded\"")

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        val javaVersion = JavaVersion.valueOf(libs.versions.app.build.javaVersion.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.app.build.kotlinJVMTarget.get()))
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    debugImplementation(libs.compose.ui.tooling)

    // Compose
    implementation(libs.bundles.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.bundles.lifecycle)

    // Room
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // AndroidX
    implementation(libs.core.ktx)
    implementation(libs.appcompat)

    // Coil
    implementation(libs.coil.compose)

    // Libphonenumber
    implementation(libs.libphonenumber)

    // Sentry
    implementation(libs.sentry.android)

    // Kotlinx
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.collections.immutable)
}