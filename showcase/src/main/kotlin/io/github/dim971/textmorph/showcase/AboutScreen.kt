package io.github.dim971.textmorph.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Where this came from, and what it is held to. */
@Composable
fun AboutScreen() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("TextMorph for Jetpack Compose", style = MaterialTheme.typography.headlineSmall)
        Paragraph(
            "A native port of Torph, a dependency-free animated text component for the web by " +
                "Lochie Axon. torph.lochie.me, MIT, copyright 2025.",
        )
        Paragraph(
            "The idea, the timing, the place-value alignment and the strictness about what " +
                "counts as a quantity are all upstream's. This app is a catalogue of the " +
                "engine's capabilities, one screen per capability, so that a question about " +
                "the library has one screen that answers it.",
        )
        Paragraph(
            "Thirty-three of the screens are upstream's own examples, in its order and with " +
                "its values, so the two catalogues can be read side by side. Where the motion " +
                "is the demo, as on the two bubble sliders, upstream's own physics is here " +
                "too. Elsewhere the interface around the morph is written for this platform " +
                "rather than reproduced, and the catalogue says which is which.",
        )
        Text("How it is held to the original", style = MaterialTheme.typography.titleMedium)
        Paragraph(
            "The engine is pinned by fixtures generated from the published npm package, plus " +
                "the 2710 cases of Unicode's own UAX #29 conformance suite. The iOS twin " +
                "replays byte-identical fixtures, so a difference between the two ports is a " +
                "red test rather than a discovery.",
        )
        Paragraph(
            "There is one tolerance in the whole suite, on the spring's position, and it is " +
                "one ulp: StrictMath is fdlibm and V8's Math is a port of fdlibm, and they " +
                "disagree in the last bit of one cosine. The settling duration, which every " +
                "fade window is a fraction of, is compared exactly.",
        )
        Text("The honest limitation", style = MaterialTheme.typography.titleMedium)
        Paragraph(
            "A morph draws its own glyphs, so the value cannot be selected, in any mode. It is " +
                "for values a reader watches change: counters, prices, labels, statuses. Body " +
                "copy wants a text composable.",
        )
        Paragraph("github.com/dim971/textmorph-android")
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}
