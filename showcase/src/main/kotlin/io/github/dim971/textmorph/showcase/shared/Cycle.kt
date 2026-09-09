package io.github.dim971.textmorph.showcase.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Steps through a list of values, on a tap or on a timer.
 *
 * Every demo needs the same thing: something that changes. Sharing it keeps each
 * demo about the one capability it is there to show.
 */
@Stable
class Cycle<T>(
    private val values: List<T>,
) {
    var index by mutableIntStateOf(0)
        private set

    val current: T get() = values[index]

    fun advance() {
        index = (index + 1) % values.size
    }
}

@Composable
fun <T> rememberCycle(vararg values: T): Cycle<T> = remember { Cycle(values.toList()) }

/** A demo that advances when it is tapped, or on its own in the catalogue. */
@Composable
fun Tappable(
    hint: String,
    advance: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (LocalAutoAdvance.current) {
        Ticking(intervalMs = 1600, advance = advance, content = content)
        return
    }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = advance)
                .semantics {
                    onClick(label = hint) {
                        advance()
                        true
                    }
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content()
        Text(
            text = hint,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A demo that advances on its own. */
@Composable
fun Ticking(
    intervalMs: Long,
    advance: () -> Unit,
    content: @Composable () -> Unit,
) {
    val latest by rememberUpdatedState(advance)
    LaunchedEffect(intervalMs) {
        while (true) {
            delay(intervalMs)
            latest()
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}
