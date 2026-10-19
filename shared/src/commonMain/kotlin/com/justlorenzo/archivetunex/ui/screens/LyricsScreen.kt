@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.justlorenzo.archivetunex.ui.screens

import com.justlorenzo.archivetunex.ui.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.viewmodels.LyricsRenderScreenState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.justlorenzo.archivetunex.ui.LocalActions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import moe.rukamori.archivetune.compat.Timber
import moe.rukamori.archivetune.compat.media3.C
import moe.rukamori.archivetune.constants.LyricsMode
import moe.rukamori.archivetune.constants.LyricsModeKey
import moe.rukamori.archivetune.lyrics.LyricsHelper
import moe.rukamori.archivetune.ui.component.LyricsEnhanced
import moe.rukamori.archivetune.ui.component.LyricsV2
import moe.rukamori.archivetune.utils.rememberEnumPreference
import moe.rukamori.archivetune.viewmodels.LyricsRenderViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.mp.KoinPlatform

/**
 * Testi del brano con il renderer originale di ArchiveTune (sincronizzati, parola per parola,
 * tocca una riga per saltarci). Si usa dentro il player, al posto della copertina grande.
 */
@Composable
fun PlayerLyrics(
    textColor: Color,
    modifier: Modifier = Modifier,
    viewModel: LyricsRenderViewModel = koinViewModel(),
) {
    val actions = LocalActions.current
    val connection = actions.player
    val metadata by connection.mediaMetadata.collectAsState()
    val song = metadata ?: return
    val state by viewModel.state.collectAsState()
    val lyricsMode by rememberEnumPreference(LyricsModeKey, LyricsMode.ENHANCED)

    // Se il testo non è ancora nel database lo cerca nei siti dei testi (come ArchiveTune)
    LaunchedEffect(song.id) {
        if (actions.database.lyrics(song.id).first() != null) return@LaunchedEffect
        try {
            val lyrics = KoinPlatform.getKoin().get<LyricsHelper>().getLyrics(song)
            actions.database.query { insertLyricsIfAbsent(id = song.id, lyrics = lyrics) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag("Lyrics").w(e, "Testo non trovato")
        }
    }

    // Il renderer vuole la durata: aspetta che il player la conosca
    LaunchedEffect(song.id) {
        var duration = C.TIME_UNSET
        for (attempt in 0 until 20) {
            duration = connection.player.duration
            if (duration != C.TIME_UNSET && duration > 0) break
            delay(250)
        }
        val known = duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: (song.duration.toLong() * 1000L).coerceAtLeast(0L)
        viewModel.bind(song.id, known)
    }

    when (val current = state) {
        // Caricamento: indicatore Expressive al centro, non le barre grigie attaccate al titolo
        LyricsRenderScreenState.Loading ->
            LyricsMessage(modifier, textColor) {
                LoadingIndicator(color = textColor, modifier = Modifier.size(56.dp))
                Text(tr("Cerco il testo…"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.8f))
            }
        LyricsRenderScreenState.Empty ->
            LyricsMessage(modifier, textColor) {
                Text(tr("Nessun testo per questo brano"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.8f))
            }
        is LyricsRenderScreenState.Success ->
            // Sfumatura in alto e in basso: le righe entrano ed escono dolcemente
            Box(
                modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.08f to Color.Black,
                                0.88f to Color.Black,
                                1f to Color.Transparent,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            ) {
                when (lyricsMode) {
                    LyricsMode.V2 ->
                        // V2: animazioni a molla e bagliore
                        LyricsV2(
                            lyricsState = current,
                            sliderPositionProvider = { null },
                            lyricsSyncOffset = 0,
                            modifier = Modifier.fillMaxSize(),
                            textColorOverride = textColor,
                        )
                    else ->
                        LyricsEnhanced(
                            lyricsState = current,
                            sliderPositionProvider = { null },
                            lyricsSyncOffset = 0,
                            modifier = Modifier.fillMaxSize(),
                            textColorOverride = textColor,
                        )
                }
            }
        else ->
            LyricsMessage(modifier, textColor) {
                Text(tr("Testo non disponibile"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.8f))
            }
    }
}

@Composable
private fun LyricsMessage(
    modifier: Modifier,
    textColor: Color,
    content: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxSize().padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) { content() }
}
