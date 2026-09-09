package io.github.dim971.textmorph.showcase.catalog

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
import io.github.dim971.textmorph.showcase.shared.CodeSnippet

/** One demo, with what it shows and the code it is. */
@Composable
fun DemoScreen(demo: Demo) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        demo.content()
        Text(demo.summary, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "Capability: ${demo.capability}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CodeSnippet(demo.code)
    }
}
