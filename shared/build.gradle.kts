import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    android {
       namespace = "com.justlorenzo.archivetunex.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    compilerOptions {
        // API sperimentali usate dall'interfaccia di ArchiveTune (nell'originale con @OptIn sparsi)
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.material.ExperimentalMaterialApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "androidx.compose.animation.ExperimentalAnimationApi",
            "androidx.compose.animation.ExperimentalSharedTransitionApi",
            "androidx.compose.ui.ExperimentalComposeUiApi",
            "org.jetbrains.compose.resources.ExperimentalResourceApi",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.FlowPreview",
            "kotlinx.coroutines.DelicateCoroutinesApi",
            "kotlinx.serialization.ExperimentalSerializationApi",
            "kotlin.time.ExperimentalTime",
            "kotlin.uuid.ExperimentalUuidApi",
            "kotlin.io.encoding.ExperimentalEncodingApi",
            "kotlinx.cinterop.ExperimentalForeignApi",
            "kotlinx.cinterop.BetaInteropApi",
        )
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.accompanist.lyrics.ui)
            implementation(libs.kuromoji.ipadic)
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.session)
            implementation(libs.androidx.core.ktx)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            api(project(":innertube"))
            implementation(libs.quickjs.kt)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.material.kolor)
            implementation(libs.datastore.preferences.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.room.runtime)
            implementation(libs.material.icons.extended)
            implementation(libs.sqlite.bundled)
            implementation(libs.coil.compose)
            // Vetro (Liquid Glass) disegnato in Compose: pulsanti, menu e fogli del tema iOS
            implementation(libs.haze.glass)
            implementation(libs.coil.network.ktor)
            implementation(libs.coil.svg)
            implementation(libs.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.shimmer)
            implementation(libs.reorderable)
            implementation(libs.haze)
            implementation(libs.haze.blur)
            implementation(libs.material3.adaptive)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.encoding)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.websockets)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.window.core)
            implementation(libs.accompanist.lyrics.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

// Database Room multipiattaforma: KSP genera il codice per Android e per iPhone.
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
