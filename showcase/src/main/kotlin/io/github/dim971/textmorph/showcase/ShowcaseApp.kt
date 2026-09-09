package io.github.dim971.textmorph.showcase

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.dim971.textmorph.showcase.catalog.CatalogScreen
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.catalog.DemoScreen
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.ShowcaseSettings

private enum class Tab(
    val label: String,
) {
    Catalog("Demos"),
    Playground("Playground"),
    About("About"),
}

/**
 * The whole app.
 *
 * Three tabs and one detail screen, kept in plain state rather than in a
 * navigation graph: the app has four destinations, and a graph would be more
 * machinery than the thing it navigates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowcaseApp() {
    val settings = remember { ShowcaseSettings() }
    var tab by remember { mutableStateOf(Tab.Catalog) }
    var open by remember { mutableStateOf<Demo?>(null) }

    // Light and dark, and no dynamic colour: that needs API 31 and this app
    // supports 24, which is the library's floor and therefore the floor the
    // showcase should be seen to work at.
    val colours = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colours) {
        CompositionLocalProvider(LocalShowcaseSettings provides settings) {
            BackHandler(enabled = open != null) { open = null }

            Scaffold(
                topBar = { TopAppBar(title = { Text(open?.name ?: "TextMorph") }) },
                bottomBar = {
                    if (open != null) return@Scaffold
                    NavigationBar {
                        for (entry in Tab.entries) {
                            NavigationBarItem(
                                selected = tab == entry,
                                onClick = { tab = entry },
                                icon = {},
                                label = { Text(entry.label) },
                            )
                        }
                    }
                },
            ) { padding ->
                val demo = open
                Box(Modifier.padding(padding)) {
                    when {
                        demo != null -> DemoScreen(demo)
                        tab == Tab.Catalog -> CatalogScreen(onOpen = { open = it })
                        tab == Tab.Playground -> PlaygroundScreen()
                        else -> AboutScreen()
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ShowcasePreview() {
    ShowcaseApp()
}
