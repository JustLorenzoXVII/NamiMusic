package moe.rukamori.archivetune.compat.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlinx.coroutines.delay

private fun ISyncedLine.displayText(): String =
    when (this) {
        is KaraokeLine -> syllables.joinToString("") { it.content }
        is SyncedLine -> content
        else -> toString()
    }

private fun ISyncedLine.translationText(): String? =
    when (this) {
        is KaraokeLine -> translation
        is SyncedLine -> translation
        else -> null
    }

@OptIn(ExperimentalFoundationApi::class)
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
    var activeIndex by remember(lyrics) { mutableIntStateOf(-1) }
    LaunchedEffect(lyrics) {
        while (true) {
            val position = currentPosition()
            val index = lyrics.lines.indexOfLast { it.start <= position }
            if (index != activeIndex) {
                activeIndex = index
                if (index >= 0) listState.animateScrollToItem(index)
            }
            delay(50)
        }
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = offset, bottom = offset * 2),
        modifier = modifier,
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val active = index == activeIndex
            val color by animateColorAsState(if (active) textColor else textColor.copy(alpha = 0.35f))
            Column(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = { onLineClicked(line) }, onLongClick = { onLinePressed(line) })
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text(line.displayText(), style = normalLineTextStyle, color = color)
                if (showTranslation) {
                    line.translationText()?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = phoneticTextStyle, color = color.copy(alpha = color.alpha * 0.8f))
                    }
                }
            }
        }
    }
}
