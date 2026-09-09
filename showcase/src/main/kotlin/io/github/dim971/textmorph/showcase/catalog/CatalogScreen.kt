package io.github.dim971.textmorph.showcase.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dim971.textmorph.showcase.shared.LocalAutoAdvance
import io.github.dim971.textmorph.showcase.shared.LocalInteractive

/**
 * The catalogue, with a live preview in every row.
 *
 * The previews run themselves rather than waiting for a tap, because a row's tap
 * opens the demo and a still morph is a screenshot.
 */
@Composable
fun CatalogScreen(onOpen: (Demo) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(catalog, key = { it.id }) { demo ->
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(demo) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CompositionLocalProvider(
                        LocalAutoAdvance provides true,
                        LocalInteractive provides false,
                    ) {
                        demo.content()
                    }
                    Column(Modifier.fillMaxWidth()) {
                        Text(demo.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = demo.capability,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
