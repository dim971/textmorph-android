package io.github.dim971.textmorph.showcase.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * One entry in the catalogue.
 *
 * Each demo names the capability it exists to show, because a catalogue of
 * pretty animations teaches nothing: the point is to be able to look up "how
 * does a number roll" and find the one screen that answers it.
 */
@Immutable
class Demo(
    val id: String,
    val name: String,
    /** What it is, in a line. */
    val summary: String,
    /** The one capability of the engine it exercises. */
    val capability: String,
    /** The code it is, near enough to paste. */
    val code: String,
    /** The demo itself. */
    val content: @Composable () -> Unit,
)
