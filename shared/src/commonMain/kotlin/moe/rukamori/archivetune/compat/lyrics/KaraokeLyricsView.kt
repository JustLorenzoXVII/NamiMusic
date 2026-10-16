package moe.rukamori.archivetune.compat.lyrics

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics

/**
 * Vista karaoke dei testi. Su Android è quella della libreria Accompanist Lyrics usata da
 * ArchiveTune (com.mocharealm.accompanist.lyrics.ui); su iPhone quella libreria non esiste
 * nella versione usata, quindi c'è una vista equivalente in Compose.
 */
@Composable
expect fun KaraokeLyricsView(
    listState: LazyListState,
    lyrics: SyncedLyrics,
    currentPosition: () -> Int,
    onLineClicked: (ISyncedLine) -> Unit,
    onLinePressed: (ISyncedLine) -> Unit,
    textColor: Color,
    normalLineTextStyle: TextStyle,
    accompanimentLineTextStyle: TextStyle,
    phoneticTextStyle: TextStyle,
    blendMode: BlendMode,
    useBlurEffect: Boolean,
    showTranslation: Boolean,
    showPhonetic: Boolean,
    offset: Dp,
    keepAliveZone: Dp,
    modifier: Modifier,
)
