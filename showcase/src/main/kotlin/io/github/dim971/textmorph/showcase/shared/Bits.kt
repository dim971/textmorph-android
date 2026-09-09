package io.github.dim971.textmorph.showcase.shared

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.engine.TextMorphFont
import kotlinx.coroutines.delay

// The furniture the demos share.
//
// Every one of them is a value that changes inside a little piece of interface,
// so the interface is here and each demo is left saying only what changes and
// when. A demo that has to spell out its own padding is a demo whose point is
// buried.

/** The size most demos show a value at. */
fun stageFont(
    size: TextUnit = 26.sp,
    weight: FontWeight = FontWeight.Medium,
    mono: Boolean = false,
): TextMorphFont =
    TextMorphFont(
        fontFamily = if (mono) FontFamily.Monospace else null,
        fontSize = size,
        fontWeight = weight,
    )

/** A centred value with an optional caption under it. */
@Composable
fun Stage(
    caption: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
        if (caption != null) Caption(caption)
    }
}

/** The small grey line under a value. */
@Composable
fun Caption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * A pill whose width follows its text.
 *
 * The container animates its own size, so the pill grows and shrinks with the
 * value rather than jumping. That is the whole reason several of these demos are
 * pills.
 */
@Composable
fun Chip(content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    RoundedCornerShape(percent = 50),
                ).padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Two values side by side, which is how upstream shows a contrast. */
@Composable
fun SplitRow(
    separator: String? = null,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        left()
        if (separator != null) {
            Text(
                text = separator,
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Box(Modifier.padding(horizontal = 14.dp))
        }
        right()
    }
}

/**
 * Steps a value on a timer until someone touches the demo, then hands over.
 *
 * Upstream's phrase for it, and the behaviour is worth copying: a demo that
 * keeps animating under a finger fights the finger, and a demo that never moves
 * is a screenshot. Returns whether autoplay is still running, so a demo can show
 * a hint only while it has not been taken over.
 */
@Composable
fun rememberAutoplay(
    intervalMs: Long,
    step: () -> Unit,
): Autoplay {
    val latest by rememberUpdatedState(step)
    val autoplay = remember { Autoplay() }
    LaunchedEffect(autoplay, intervalMs) {
        while (autoplay.isPlaying) {
            delay(intervalMs)
            if (autoplay.isPlaying) latest()
        }
    }
    return autoplay
}

/** The handle an autoplaying demo hands over with. */
class Autoplay {
    var isPlaying: Boolean by mutableStateOf(true)
        private set

    /** Called on the first touch. */
    fun takeOver() {
        isPlaying = false
    }
}

/**
 * A counter that walks a list on a timer, for the many demos that are a cycle.
 *
 * Separate from [Cycle] because these take their interval from upstream rather
 * than from the catalogue, and because a demo in a catalogue row and the same
 * demo on its own screen should tick at the same rate. The rate is the demo's,
 * not the surface's.
 */
@Composable
fun rememberTicker(
    count: Int,
    intervalMs: Long,
): Int {
    var index by remember { mutableIntStateOf(0) }
    val still = rememberReducedMotion()
    LaunchedEffect(count, intervalMs, still) {
        if (still) return@LaunchedEffect
        while (true) {
            delay(intervalMs)
            index = (index + 1) % count
        }
    }
    return index
}

/**
 * Whether the system asks for less motion.
 *
 * The library already honours this by not animating. The demos honour it a
 * second time by not cycling at all, which is upstream's behaviour and the right
 * one: a value that jumps between four states on a timer is still motion, even
 * when each jump is instant.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

/**
 * Breaks a passage into lines of at most [maxChars], greedily.
 *
 * The library never wraps: a line exists only where the value put one, which is
 * upstream's design and the reason a value can be diffed at all. So a demo that
 * wants prose on several lines has to say where the breaks go, and this is
 * upstream's own helper, transcribed.
 */
fun wrap(
    text: String,
    maxChars: Int,
): String {
    val lines = ArrayList<String>()
    var line = ""
    for (word in text.split(" ")) {
        val next = if (line.isEmpty()) word else "$line $word"
        if (line.isNotEmpty() && next.length > maxChars) {
            lines.add(line)
            line = word
        } else {
            line = next
        }
    }
    if (line.isNotEmpty()) lines.add(line)
    return lines.joinToString("\n")
}
