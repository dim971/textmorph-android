package io.github.dim971.textmorph.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.engine.TextMorphFont
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.rememberCycle
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import kotlin.math.roundToInt

/**
 * Every option, over one value.
 *
 * A morph is a transition, so the only way to understand an option is to watch
 * the same change with it on and then off. That is what this screen is for, and
 * it is why the settings are global to the app rather than local to it: turning
 * debug on here turns it on in every demo.
 */
@Composable
fun PlaygroundScreen() {
    val settings = LocalShowcaseSettings.current
    val cycle =
        rememberCycle(
            "Total balance 1,204.00",
            "Total balance 1,318.45",
            "balance Total 980.10",
            "Total 1,204.00",
        )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextMorph(
            text = cycle.current,
            options = settings.options,
            font = TextMorphFont(fontSize = 26.sp, fontWeight = FontWeight.Medium),
            colour = showcaseColour(),
        )
        Button(onClick = cycle::advance) { Text("Change the value") }

        HorizontalDivider()

        Toggle("Spring instead of a bezier", settings.useSpring) { settings.useSpring = it }
        if (settings.useSpring) {
            Dial("Stiffness", settings.stiffness, 20.0, 400.0) { settings.stiffness = it }
            Dial("Damping", settings.damping, 2.0, 40.0) { settings.damping = it }
        } else {
            Dial("Duration, ms", settings.duration, 80.0, 2000.0) { settings.duration = it }
        }

        Toggle("Scale what leaves", settings.scale) { settings.scale = it }
        Toggle("Morph numbers by place value", settings.numbers) { settings.numbers = it }
        Toggle("Debug: outline every segment", settings.debug) { settings.debug = it }
        Toggle("Disabled: arrive in place", settings.disabled) { settings.disabled = it }

        Text(
            text =
                "Turning numbers off is the one to watch: the balance stops rolling by " +
                    "column and morphs character by character, which is what a version number or " +
                    "a product code wants and what a quantity does not.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Toggle(
    label: String,
    value: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun Dial(
    label: String,
    value: Double,
    from: Double,
    to: Double,
    onChange: (Double) -> Unit,
) {
    Column {
        Text("$label: ${value.roundToInt()}", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toDouble()) },
            valueRange = from.toFloat()..to.toFloat(),
        )
    }
}
