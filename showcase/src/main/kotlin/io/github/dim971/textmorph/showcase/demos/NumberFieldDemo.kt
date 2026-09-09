package io.github.dim971.textmorph.showcase.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.engine.TextMorphFont
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.showcaseColour

/** A field being typed into, where the caret says where the edit was. */
@Composable
fun NumberFieldDemo() {
    val settings = LocalShowcaseSettings.current
    var entry by remember { mutableStateOf("1204") }
    var caret by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextMorph(
            value = entry.toDoubleOrNull() ?: 0.0,
            options = settings.options(decimals = 0),
            font = TextMorphFont(fontSize = 40.sp, fontWeight = FontWeight.SemiBold),
            colour = showcaseColour(),
            cursorIndex = caret,
        )
        OutlinedTextField(
            value = entry,
            onValueChange = { new ->
                // Where the edit was. Without it the digits realign by column,
                // which is the wrong answer for a field: the reader is watching
                // the keystroke, not the magnitude.
                entry = new.filter { it.isDigit() }.take(9)
                caret = entry.length
            },
            label = { Text("Amount") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(200.dp),
        )
    }
}

val numberFieldDemo =
    Demo(
        id = "field",
        name = "Number field",
        summary =
            "Type into it. With a caret the digits either side of the edit hold their " +
                "identity and only the keystroke is new; without one they realign by column, which is " +
                "right for a magnitude and wrong for a field. Carrying 123 to 1,234 is a two-character " +
                "delta of which the reader typed one.",
        capability = "caret matching instead of place matching",
        code =
            """
            TextMorph(value = amount, cursorIndex = caret)

            OutlinedTextField(
                value = entry,
                onValueChange = { entry = it; caret = it.length },
            )
            """.trimIndent(),
        content = { NumberFieldDemo() },
    )
