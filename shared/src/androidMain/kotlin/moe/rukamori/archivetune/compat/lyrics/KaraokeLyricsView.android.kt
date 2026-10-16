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

@Composable
actual fun KaraokeLyricsView(
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
) {
    com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView(
        listState = listState,
        lyrics = lyrics,
        currentPosition = currentPosition,
        onLineClicked = onLineClicked,
        onLinePressed = onLinePressed,
        textColor = textColor,
        normalLineTextStyle = normalLineTextStyle,
        accompanimentLineTextStyle = accompanimentLineTextStyle,
        phoneticTextStyle = phoneticTextStyle,
        blendMode = blendMode,
        useBlurEffect = useBlurEffect,
        showTranslation = showTranslation,
        showPhonetic = showPhonetic,
        offset = offset,
        keepAliveZone = keepAliveZone,
        modifier = modifier,
    )
}
