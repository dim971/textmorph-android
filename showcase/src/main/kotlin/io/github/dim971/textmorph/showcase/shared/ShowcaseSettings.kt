package io.github.dim971.textmorph.showcase.shared

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.dim971.textmorph.core.CubicBezier
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

    /**
     * The same, with the ease a particular demo asks for.
     *
     * Several of upstream's demos name their own ease rather than taking the
     * default, and the choice is part of what the demo shows. Overriding through
     * the settings rather than around them keeps the playground's switches
     * working: debug, disabled and numbers still apply.
     *
     * The spring switch in the playground wins, so a reader can hear the whole
     * catalogue on one spring if they want to.
     */
    fun optionsWith(
        ease: TextMorphEase,
        decimals: Int? = null,
    ): TextMorphOptions =
        if (useSpring) {
            options.copy(decimals = decimals)
        } else {
            options.copy(ease = ease, decimals = decimals)
        }

    companion object {
        /**
         * The spring nine of upstream's demos ask for by name.
         *
         * Softer and slower than the default bezier, and it overshoots, which is
         * why those demos are the ones where a value visibly settles rather than
         * arriving.
         */
        val UpstreamSpring: TextMorphEase =
            TextMorphEase.spring(stiffness = 150.0, damping = 19.0, mass = 1.2)

        /**
         * The curve `ExampleAction` names.
         *
         * Its control points rise past one, so it overshoots and comes back.
         * Upstream writes it as the CSS string `cubic-bezier(0.41, 1.03, 0.6,
         * 1.03)`; the typed API takes the four numbers, which is the same curve.
         */
        val ActionCurve: TextMorphEase =
            TextMorphEase.Bezier(CubicBezier(0.41, 1.03, 0.6, 1.03))
    }
}

/** The settings, reachable from any demo without threading them through thirty-five signatures. */
val LocalShowcaseSettings = compositionLocalOf { ShowcaseSettings() }

/**
 * Whether a demo drives itself instead of waiting to be tapped.
 *
 * Set in the catalogue, where a row's tap belongs to opening the demo and a demo
 * that only moved when tapped would be a screenshot.
 */
val LocalAutoAdvance = compositionLocalOf { false }

/**
 * Whether a demo should accept a gesture.
 *
 * False in a catalogue row, and it has to be: a row is inside a scrolling list,
 * and a demo that grabs a drag there eats the scroll. So the previews run
 * themselves and answer to nothing, and a demo is only draggable on its own
 * screen. The alternative, letting each demo decide, is the same rule written
 * eleven times.
 */
val LocalInteractive = compositionLocalOf { true }
