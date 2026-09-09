package io.github.dim971.textmorph.showcase.demos

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.Caption
import io.github.dim971.textmorph.showcase.shared.LocalInteractive
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.onTint
import io.github.dim971.textmorph.showcase.shared.rememberAutoplay
import io.github.dim971.textmorph.showcase.shared.stageFont

private val ZONES =
    listOf(
        "North End",
        "The Harbour",
        "Old Town",
        "Riverside",
        "The Docks",
        "Hillside",
    )

private const val COLUMNS = 3
private val CELL_W = 104.dp
private val CELL_H = 62.dp

/** A label that follows the cell it names. */
@Composable
fun TrailingTagDemo() {
    val settings = LocalShowcaseSettings.current
    val live = LocalInteractive.current
    var zone by remember { mutableIntStateOf(1) }
    val autoplay = rememberAutoplay(2000) { zone = (zone + 1) % ZONES.size }

    val column = zone % COLUMNS
    val row = zone / COLUMNS
    val x by animateDpAsState(
        CELL_W * column,
        spring(dampingRatio = 0.7f, stiffness = 220f),
        label = "x",
    )
    val y by animateDpAsState(
        CELL_H * row,
        spring(dampingRatio = 0.7f, stiffness = 220f),
        label = "y",
    )

    Stage {
        Box(contentAlignment = Alignment.TopStart) {
            Column {
                for (r in 0 until ZONES.size / COLUMNS) {
                    Row {
                        for (c in 0 until COLUMNS) {
                            val index = r * COLUMNS + c
                            Box(
                                modifier =
                                    Modifier
                                        .size(CELL_W, CELL_H)
                                        .then(
                                            if (!live) {
                                                Modifier
                                            } else {
                                                Modifier.clickable {
                                                    autoplay.takeOver()
                                                    zone = index
                                                }
                                            },
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = ZONES[index],
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            // The tag, riding to whichever cell is current. Upstream drifts it
            // on a lissajous so it visits every zone; here it walks them in
            // order, and a tap takes it over.
            Box(
                modifier =
                    Modifier
                        .offset(x = x, y = y)
                        .size(CELL_W, CELL_H),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .background(
                            settings.tint.colour,
                            RoundedCornerShape(8.dp),
                        ).padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    TextMorph(
                        text = ZONES[zone],
                        options = settings.options,
                        font = stageFont(size = 15.sp),
                        colour = onTint(settings.tint.colour),
                    )
                }
            }
        }
        Caption(if (autoplay.isPlaying) "tap a zone" else ZONES[zone])
    }
}

val trailingTagDemo =
    Demo(
        id = "tag",
        name = "Trailing tag",
        summary =
            "A tag that travels to the cell it names while the name itself morphs. Two " +
                "journeys at once again, and this one crosses in two directions: The Harbour to " +
                "Riverside moves the pill diagonally while the words underneath it have almost " +
                "nothing in common. The word The survives between three of the six, which is the " +
                "only thing holding those morphs together.",
        capability = "a morph inside a container travelling in two axes",
        code =
            """
            Box(Modifier.offset(x = x, y = y)) {
                TextMorph(text = zones[zone])
            }
            """.trimIndent(),
        content = { TrailingTagDemo() },
    )
