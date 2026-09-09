package io.github.dim971.textmorph.showcase.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.Caption
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.ShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberReducedMotion
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont
import kotlinx.coroutines.delay
import java.util.Locale

// Fixed, so the value is the same every run. Upstream keeps it fixed so its
// server and its first client paint agree; here it is fixed so a screenshot is
// reproducible.
private const val DEPOSIT = 1204.42172398
private const val APY = 0.0418
private const val PER_SECOND = (DEPOSIT * APY) / 31_536_000

/** Eight fraction digits, because a per-second rate is invisible at two. */
private fun money(value: Double): String = String.format(Locale.US, "%,.8f", value)

/** A balance accruing interest every second. */
@Composable
fun EarnedDemo() {
    val settings = LocalShowcaseSettings.current
    var earned by remember { mutableDoubleStateOf(0.0) }
    val still = rememberReducedMotion()

    LaunchedEffect(still) {
        if (still) return@LaunchedEffect
        val started = System.currentTimeMillis()
        while (true) {
            delay(120)
            earned = ((System.currentTimeMillis() - started) / 1000.0) * PER_SECOND
        }
    }

    Stage {
        TextMorph(
            text = "${'$'}${money(DEPOSIT + earned)}",
            options = settings.optionsWith(ShowcaseSettings.UpstreamSpring),
            font = stageFont(size = 26.sp, mono = true),
            colour = showcaseColour(),
        )
        Caption("4.18% APY, accruing every second")
    }
}

val earnedDemo =
    Demo(
        id = "earned",
        name = "Earned",
        summary =
            "A deposit earning interest, repainted eight times a second at eight fraction " +
                "digits. Only the last few columns move, because only they changed, and the dollars " +
                "and the cents sit perfectly still through thousands of morphs. Every one of those " +
                "morphs interrupts the one before it.",
        capability = "place-value alignment under continuous interruption",
        code =
            """
            // repainted every 120ms against a 400ms morph
            TextMorph(text = "${'$'}${'$'}{money(deposit + earned)}")
            """.trimIndent(),
        content = { EarnedDemo() },
    )
