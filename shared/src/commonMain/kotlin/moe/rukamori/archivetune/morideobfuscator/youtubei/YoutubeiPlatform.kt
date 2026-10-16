/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * ArchiveTuneX: versione Kotlin Multiplatform (Android + iOS) del modulo morideobfuscator.
 */
package moe.rukamori.archivetune.morideobfuscator.youtubei

import kotlinx.coroutines.CoroutineDispatcher
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Thread dedicato a QuickJS, con stack grande: youtubei.js analizza il JavaScript
 * del player di YouTube con funzioni molto ricorsive.
 */
internal expect fun createYoutubeiDispatcher(
    name: String,
    stackSizeBytes: Long,
): CoroutineDispatcher

/** Cartella per la cache di youtubei.js (player di YouTube già scaricati, ecc.). */
internal expect fun youtubeiCacheDirectory(): String

/** Fuso orario del telefono, es. "Europe/Rome". */
expect fun currentTimeZoneId(): String

@OptIn(ExperimentalTime::class)
internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * quickjs-kt espone evaluate<T>() come estensione su Android e come metodo della classe su iOS:
 * queste due funzioni nascondono la differenza.
 */
internal expect suspend fun com.dokar.quickjs.QuickJs.evaluateString(
    code: String,
    filename: String,
): String

internal expect suspend fun com.dokar.quickjs.QuickJs.evaluateUnit(
    code: String,
    filename: String,
)
