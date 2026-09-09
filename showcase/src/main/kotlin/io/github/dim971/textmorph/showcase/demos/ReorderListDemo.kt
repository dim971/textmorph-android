package io.github.dim971.textmorph.showcase.demos

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.Caption
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberAutoplay
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont

private val TRACKS = listOf("Ambient loops", "Field recordings", "Tape hiss")

private val ROW_H = 46.dp

/** A list whose numbers change when its rows do. */
@Composable
fun ReorderListDemo() {
    val settings = LocalShowcaseSettings.current
    var order by remember { mutableStateOf(TRACKS.indices.toList()) }
    rememberAutoplay(2600) {
        // One swap at a time, so exactly two numbers change and the third is
        // visibly the one that did not.
        val a = (0 until TRACKS.size - 1).random()
        order =
            order.toMutableList().also {
                it[a] = order[a + 1]
                it[a + 1] = order[a]
            }
    }

    Stage {
        Box(Modifier.width(240.dp).height(ROW_H * TRACKS.size)) {
            for (track in TRACKS.indices) {
                val slot = order.indexOf(track)
                val y by animateDpAsState(
                    ROW_H * slot,
                    spring(dampingRatio = 0.75f, stiffness = 300f),
                    label = "row",
                )
                Row(
                    modifier =
                        Modifier
                            .offset(y = y)
                            .fillMaxWidth()
                            .height(ROW_H)
                            .padding(vertical = 3.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                RoundedCornerShape(10.dp),
                            ).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextMorph(
                        text = "${slot + 1}",
                        options = settings.options,
                        font = stageFont(size = 16.sp),
                        colour = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = TRACKS[track],
                        style = MaterialTheme.typography.bodyMedium,
                        color = showcaseColour(),
                    )
                }
            }
        }
        Caption("the numbers morph, the rows slide")
    }
}

val reorderListDemo =
    Demo(
        id = "reorder",
        name = "Reorder list",
        summary =
            "Two rows swap and their numbers swap with them. The rows slide, which the " +
                "layout does, and the numbers morph, which the library does, and the two happen at " +
                "once without knowing about each other. The row that did not move keeps its number " +
                "perfectly still, which is how you can tell the difference.",
        capability = "many small morphs, one per row",
        code =
            """
            TextMorph(text = "${'$'}{position + 1}")
            """.trimIndent(),
        content = { ReorderListDemo() },
    )
