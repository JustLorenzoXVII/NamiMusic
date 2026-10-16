import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Modulo condiviso con il client InnerTube di YouTube Music.
// Deriva dal modulo "core" di ArchiveTune (github.com/rukamori/core, GPL-3.0).
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "com.justlorenzo.archivetunex.innertube"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    compilerOptions {
        optIn.add("kotlin.io.encoding.ExperimentalEncodingApi")
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.json)
            api(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.encoding)
            implementation(libs.okhttp.dnsoverhttps)
            implementation(libs.newpipe.extractor)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
