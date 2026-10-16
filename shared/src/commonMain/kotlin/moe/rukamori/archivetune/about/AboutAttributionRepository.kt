/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.about

import moe.rukamori.archivetune.compat.Charsets

import archivetunex.shared.generated.resources.*

import kotlinx.coroutines.IO

import moe.rukamori.archivetune.compat.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.compat.JSONArray
import moe.rukamori.archivetune.compat.Locale
import moe.rukamori.archivetune.compat.uppercase
import moe.rukamori.archivetune.compat.lowercase

@Immutable
data class AboutTranslationContributor(
    val language: String,
    val contributors: AboutTranslationContributorNameCollection,
)

@Immutable
data class AboutTranslationContributorCollection private constructor(
    private val values: List<AboutTranslationContributor>,
) {
    val isEmpty: Boolean get() = values.isEmpty()
    val size: Int get() = values.size

    operator fun get(index: Int): AboutTranslationContributor = values[index]

    companion object {
        fun from(values: List<AboutTranslationContributor>): AboutTranslationContributorCollection =
            AboutTranslationContributorCollection(values.toList())
    }
}

@Immutable
data class AboutTranslationContributorNameCollection private constructor(
    private val values: List<String>,
) {
    val isEmpty: Boolean get() = values.isEmpty()

    fun joinToString(): String = values.joinToString(separator = ", ")

    fun forEach(action: (String) -> Unit) {
        values.forEach(action)
    }

    companion object {
        fun from(values: List<String>): AboutTranslationContributorNameCollection =
            AboutTranslationContributorNameCollection(values.toList())
    }
}

@Immutable
data class AboutDependencyLicense(
    val name: String,
    val version: String?,
    val licenses: String?,
)

@Immutable
data class AboutDependencyLicenseCollection private constructor(
    private val values: List<AboutDependencyLicense>,
) {
    val isEmpty: Boolean get() = values.isEmpty()
    val size: Int get() = values.size

    operator fun get(index: Int): AboutDependencyLicense = values[index]

    companion object {
        fun from(values: List<AboutDependencyLicense>): AboutDependencyLicenseCollection = AboutDependencyLicenseCollection(values.toList())
    }
}

class FetchAboutTranslationContributorsUseCase constructor(
        private val repository: AboutAttributionRepository,
    ) {
        suspend operator fun invoke(): Result<AboutTranslationContributorCollection> = repository.translationContributors()
    }

class FetchAboutDependencyLicensesUseCase constructor(
        private val repository: AboutAttributionRepository,
    ) {
        suspend operator fun invoke(): Result<AboutDependencyLicenseCollection> = repository.dependencyLicenses()
    }

class AboutAttributionRepository constructor(
        private val context: Context,
    ) {
        suspend fun translationContributors(): Result<AboutTranslationContributorCollection> =
            withContext(Dispatchers.IO) {
                try {
                    val json = Res.readBytes("files/translation_contributors.json").decodeToString()
                    Result.success(parseTranslationContributors(json))
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Result.failure(exception)
                }
            }

        suspend fun dependencyLicenses(): Result<AboutDependencyLicenseCollection> =
            withContext(Dispatchers.IO) {
                try {
                    // L'originale leggeva le licenze generate dal plugin AboutLibraries (solo Android).
                    val licenses = emptyList<AboutDependencyLicense>()
                    val collection = AboutDependencyLicenseCollection.from(licenses)
                    if (collection.isEmpty) {
                        Result.failure(IllegalStateException("No dependency licenses found"))
                    } else {
                        Result.success(collection)
                    }
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    Result.failure(throwable)
                }
            }

        private fun parseTranslationContributors(json: String): AboutTranslationContributorCollection {
            val entries = JSONArray(json)
            val contributors = ArrayList<AboutTranslationContributor>(entries.length())
            for (index in 0 until entries.length()) {
                val entry = entries.getJSONObject(index)
                val qualifier = entry.getString("qualifier")
                require(qualifier.isNotBlank()) { "Missing translation language" }
                val names = entry.getJSONArray("contributors")
                val contributorNames = ArrayList<String>(names.length())
                for (nameIndex in 0 until names.length()) {
                    val name = names.getString(nameIndex).trim()
                    require(name.isNotBlank()) { "Missing translation contributor" }
                    contributorNames.add(name)
                }
                require(contributorNames.isNotEmpty()) { "Missing translation contributors" }
                contributors.add(
                    AboutTranslationContributor(
                        language = qualifier.toLanguageDisplayName(),
                        contributors = AboutTranslationContributorNameCollection.from(contributorNames.distinct()),
                    ),
                )
            }
            return AboutTranslationContributorCollection.from(
                contributors.sortedBy { contributor -> contributor.language.lowercase(Locale.ROOT) },
            )
        }

        private fun String.toLanguageDisplayName(): String {
            val languageTag = toLanguageTag()
            val displayName =
                Locale
                    .forLanguageTag(languageTag)
                    .getDisplayName(Locale.ENGLISH)
                    .trim()
            return displayName
                .takeIf { name -> name.isNotBlank() && !name.equals(languageTag, ignoreCase = true) }
                ?: this
        }

        private fun String.toLanguageTag(): String {
            if (startsWith(Bcp47ResourceQualifierPrefix)) {
                return removePrefix(Bcp47ResourceQualifierPrefix).replace('+', '-')
            }
            val segments = split('-').filter(String::isNotBlank)
            if (segments.isEmpty()) return this
            val tagSegments = ArrayList<String>(segments.size)
            tagSegments.add(segments.first().toModernLanguageCode())
            for (segment in segments.drop(1)) {
                tagSegments.add(
                    if (segment.startsWith(RegionQualifierPrefix) && segment.length > 1) {
                        segment.drop(1)
                    } else {
                        segment
                    },
                )
            }
            return tagSegments.joinToString(separator = "-")
        }

        private fun String.toModernLanguageCode(): String =
            when (this) {
                LegacyIndonesianLanguageCode -> IndonesianLanguageCode
                LegacyHebrewLanguageCode -> HebrewLanguageCode
                LegacyYiddishLanguageCode -> YiddishLanguageCode
                else -> this
            }

        private companion object {
            const val Bcp47ResourceQualifierPrefix = "b+"
            const val RegionQualifierPrefix = "r"
            const val LegacyIndonesianLanguageCode = "in"
            const val IndonesianLanguageCode = "id"
            const val LegacyHebrewLanguageCode = "iw"
            const val HebrewLanguageCode = "he"
            const val LegacyYiddishLanguageCode = "ji"
            const val YiddishLanguageCode = "yi"
        }
    }
