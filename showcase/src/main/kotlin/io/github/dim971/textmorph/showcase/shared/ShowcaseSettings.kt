package io.github.dim971.textmorph.showcase.shared

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.dim971.textmorph.core.TextMorphEase
import io.github.dim971.textmorph.core.TextMorphOptions
import io.github.dim971.textmorph.core.defaultMorphLocale
import java.util.Locale

/**
 * The options every demo runs under, so one switch changes the whole catalogue.
 *
 * A morph is a transition, so the way to understand an option is to watch the
 * same demo with it on and off. Putting the options in one place, reachable from
 * every screen, is the difference between a catalogue and a gallery.
 */
@Stable
class ShowcaseSettings {
    var duration by mutableDoubleStateOf(400.0)
    var useSpring by mutableStateOf(false)
    var stiffness by mutableDoubleStateOf(100.0)
    var damping by mutableDoubleStateOf(10.0)
    var scale by mutableStateOf(true)
    var numbers by mutableStateOf(true)
    var debug by mutableStateOf(false)
    var disabled by mutableStateOf(false)

    /** The options as the library takes them. */
    val options: TextMorphOptions
        get() =
            TextMorphOptions(
                duration = duration,
                ease =
                    if (useSpring) {
                        TextMorphEase.spring(stiffness = stiffness, damping = damping)
                    } else {
                        TextMorphEase.Default
                    },
                scale = scale,
                numbers = numbers,
                debug = debug,
                disabled = disabled,
            )

    /** The same, with a fraction length for a numeric value. */
    fun options(
        decimals: Int?,
        locale: Locale = defaultMorphLocale,
    ): TextMorphOptions = options.copy(decimals = decimals, locale = locale)
}

/** The settings, reachable from any demo without threading them through fifteen signatures. */
val LocalShowcaseSettings = compositionLocalOf { ShowcaseSettings() }

/**
 * Whether a demo drives itself instead of waiting to be tapped.
 *
 * Set in the catalogue, where a row's tap belongs to opening the demo and a demo
 * that only moved when tapped would be a screenshot.
 */
val LocalAutoAdvance = compositionLocalOf { false }
