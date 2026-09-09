package io.github.dim971.textmorph.showcase.shared

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * What a morph is drawn in, here.
 *
 * A morph fills its own glyphs, so the colour is a parameter rather than
 * something inherited from a `LocalContentColor`: there is no text node to
 * inherit it. Every demo asks for the same one so that the catalogue reads as
 * one app rather than as thirty-five.
 */
@Composable
@ReadOnlyComposable
fun showcaseColour(): Color = MaterialTheme.colorScheme.onSurface
