/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune

/**
 * Al posto del BuildConfig generato da Gradle nell'originale (app/build.gradle.kts).
 * Le chiavi private (Last.fm, Together, server dati) non sono nel repository e restano vuote.
 */
object BuildConfig {
    const val APPLICATION_ID = "com.justlorenzo.archivetunex"
    const val VERSION_NAME = "15.1.0"
    const val VERSION_CODE = 142
    const val DEBUG = false
    const val IS_NIGHTLY_BUILD = false
    const val NIGHTLY_BUILD_HASH = ""
    const val DISTRIBUTION = "testflight"
    const val UPDATER_AVAILABLE = false
    const val GATEKEEPER_ENABLED = false
    const val LEAK_CANARY_TOGGLE_AVAILABLE = false
    const val LASTFM_API_KEY = ""
    const val LASTFM_SECRET = ""
    const val TOGETHER_BEARER_TOKEN = ""
    const val DATA_SERVER_URL = ""
    const val API_BEARER_TOKEN = ""
    const val GITHUB_OWNER = "rukamori"
    const val GITHUB_REPO = "ArchiveTune"
    const val RELEASE_GITHUB_OWNER = "rukamori"
    const val RELEASE_GITHUB_REPO = "ArchiveTune"
    const val DISCORD_APPLICATION_ID = ""
    const val DISCORD_APPLICATION_ID_LONG = 0L
    const val DISCORD_REDIRECT_SCHEME = ""

    val ARCHITECTURE: String
        get() = platformArchitecture()
    val DEVICE: String
        get() = moe.rukamori.archivetune.compat.Build.MODEL
}

internal expect fun platformArchitecture(): String
