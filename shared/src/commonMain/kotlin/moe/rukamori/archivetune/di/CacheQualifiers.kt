/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.di

/** Qualificatori delle due cache, come nell'originale (di/AppModule.kt con Hilt). */
@Retention(AnnotationRetention.BINARY)
annotation class PlayerCache

@Retention(AnnotationRetention.BINARY)
annotation class DownloadCache
