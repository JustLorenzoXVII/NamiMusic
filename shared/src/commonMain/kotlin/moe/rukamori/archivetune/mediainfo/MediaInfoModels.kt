/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.mediainfo

import archivetunex.shared.generated.resources.*

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import androidx.compose.runtime.Immutable
import moe.rukamori.archivetune.compat.ImmutableList

@Immutable
sealed interface MediaInfoState<out T> {
    data object Loading : MediaInfoState<Nothing>

    @Immutable
    data class Success<T>(val data: T) : MediaInfoState<T>

    data object Empty : MediaInfoState<Nothing>

    @Immutable
    data class Error(val reason: MediaInfoError) : MediaInfoState<Nothing>
}

enum class MediaInfoError {
    Network,
    Unavailable,
}

@Immutable
data class LocalMediaInfo(
    val title: String?,
    val artists: String?,
    val artwork: String?,
    val isLocal: Boolean,
    val format: MediaInfoFormat?,
)

@Immutable
data class MediaInfoFormat(
    val itag: Int,
    val mimeType: String,
    val codecs: String,
    val bitrate: Int,
    val sampleRate: Int?,
    val loudnessDb: Double?,
    val contentLength: Long,
)

@Immutable
data class MediaInfoMetadata(
    val title: String?,
    val author: String?,
    val artwork: String?,
    val description: String?,
    val subscribers: String?,
)

@Immutable
data class MediaInfoStatistics(
    val views: Int?,
    val likes: Int?,
    val dislikes: Int?,
)

@Immutable
data class MediaInfoData(
    val local: MediaInfoState<LocalMediaInfo>,
    val metadata: MediaInfoState<MediaInfoMetadata>,
    val statistics: MediaInfoState<MediaInfoStatistics>,
)

enum class MediaInfoTab(val labelRes: StringResource) {
    Information(Res.string.information),
    Details(Res.string.details),
    Numbers(Res.string.numbers),
}

@Immutable
data class MediaInfoQuickFact(val iconRes: DrawableResource, val text: String)

@Immutable
data class MediaInfoDetail(val labelRes: StringResource, val value: String?)

@Immutable
data class MediaInfoMetric(val labelRes: StringResource, val value: String?)

@Immutable
data class MediaInfoUiModel(
    val videoId: String,
    val title: String,
    val subtitle: String?,
    val artwork: String?,
    val metadata: MediaInfoState<MediaInfoMetadata>,
    val descriptionState: MediaInfoState<String>,
    val technicalDetails: MediaInfoState<ImmutableList<MediaInfoDetail>>,
    val statistics: MediaInfoState<MediaInfoStatistics>,
    val overview: ImmutableList<MediaInfoDetail>,
    val quickFacts: ImmutableList<MediaInfoQuickFact>,
    val metrics: ImmutableList<MediaInfoMetric>,
)

sealed interface MediaInfoEvent {
    data class Copy(val value: String) : MediaInfoEvent
    data class Share(val url: String) : MediaInfoEvent
    data object Close : MediaInfoEvent
}
