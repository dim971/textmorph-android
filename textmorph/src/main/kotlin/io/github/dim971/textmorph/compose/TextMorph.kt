package io.github.dim971.textmorph.compose

// The one public composable, mirroring upstream's one public component.

import android.graphics.Typeface
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import io.github.dim971.textmorph.core.MorphAlignment
import io.github.dim971.textmorph.core.MorphCallbacks
import io.github.dim971.textmorph.core.MorphValue
import io.github.dim971.textmorph.core.TextMorphOptions
import io.github.dim971.textmorph.engine.LayoutStore
import io.github.dim971.textmorph.engine.MorphEngine
import io.github.dim971.textmorph.engine.TextMorphFont
import kotlin.math.roundToInt

/**
 * Text that keeps its continuity when it changes.
 *
 * When the value changes, the characters, words and digits that survive the
 * change move to their new place instead of disappearing in a cross-fade. A
 * number is a special case: its digits slide along the block axis by place
 * value, so 1,204 becoming 1,318 rolls the hundreds and the tens and leaves the
 * thousands alone.
 *
 * ```kotlin
 * TextMorph(
 *     value = total,
 *     options = TextMorphOptions(decimals = 2),
 *     font = TextMorphFont(fontSize = 32.sp, fontWeight = FontWeight.SemiBold),
 * )
 * ```
 *
 * This is not a replacement for `BasicText`. It draws its own glyphs, so the
 * value cannot be selected, in any mode, and it is meant for values a reader
 * watches change: counters, prices, labels, statuses. Body copy wants a text
 * composable.
 *
 * A morph publishes its first baseline, so `Modifier.alignByBaseline()` in a
 * `Row` puts a morph and a text on the same line. Its box is the font's own
 * metrics rather than the one Compose gives a text node.
 */
@Composable
public fun TextMorph(
    text: String,
    modifier: Modifier = Modifier,
    options: TextMorphOptions = TextMorphOptions.Default,
    font: TextMorphFont = TextMorphFont.Default,
    colour: Color = Color.Black,
    cursorIndex: Int? = null,
    callbacks: MorphCallbacks = MorphCallbacks(),
) {
    TextMorph(MorphValue.Text(text), modifier, options, font, colour, cursorIndex, callbacks)
}

/** Shows a number, formatted by the options. */
@Composable
public fun TextMorph(
    value: Double,
    modifier: Modifier = Modifier,
    options: TextMorphOptions = TextMorphOptions.Default,
    font: TextMorphFont = TextMorphFont.Default,
    colour: Color = Color.Black,
    cursorIndex: Int? = null,
    callbacks: MorphCallbacks = MorphCallbacks(),
) {
    TextMorph(MorphValue.Number(value), modifier, options, font, colour, cursorIndex, callbacks)
}

/** Shows a value. */
@Composable
public fun TextMorph(
    value: MorphValue,
    modifier: Modifier = Modifier,
    options: TextMorphOptions = TextMorphOptions.Default,
    font: TextMorphFont = TextMorphFont.Default,
    colour: Color = Color.Black,
    cursorIndex: Int? = null,
    callbacks: MorphCallbacks = MorphCallbacks(),
) {
    val density = LocalDensity.current
    val resolver = LocalFontFamilyResolver.current
    val direction = LocalLayoutDirection.current
    val reduceMotion = rememberReducedMotion()

    val host = remember { MorphHost() }

    // Resolving the face needs both the composition and a density, so it is done
    // here and handed over rather than described in the engine.
    val typeface = remember(font, resolver) { font.typeface(resolver) }
    val textSize = remember(font, density) { font.pixelSize(density) }
    val letterSpacing = remember(font) { font.letterSpacingEm() }
    val alignment =
        if (direction == LayoutDirection.Rtl) {
            MorphAlignment.TRAILING
        } else {
            MorphAlignment.LEADING
        }

    host.configure(
        typeface = typeface,
        textSize = textSize,
        letterSpacing = letterSpacing,
        options = options,
        alignment = alignment,
        reduceMotion = reduceMotion,
    )

    LaunchedEffect(host, value, cursorIndex) {
        host.show(value, cursorIndex, callbacks)
    }

    // The clock, and the only thing the platform contributes to the motion.
    // Every curve is applied by the plan, so an eased clock would put every
    // opacity window at the wrong time.
    LaunchedEffect(host, host.runId) {
        while (host.isRunning) {
            withFrameNanos { host.tick(it / 1_000_000.0) }
        }
    }

    val engine = host.engine
    val store = host.store
    val label = options.formatted(value)

    Box(modifier.semantics { this.text = AnnotatedString(label) }) {
        Layout(
            modifier =
                Modifier
                    .clearAndSetSemantics { }
                    .drawBehind {
                        val render = engine?.render(host.nowMs) ?: return@drawBehind
                        if (store == null) return@drawBehind
                        drawMorph(
                            render.plan,
                            render.frame,
                            render.ghosts,
                            store,
                            colour,
                            options.debug,
                        )
                    },
            measurePolicy = { _, _ ->
                val size = engine?.size(host.nowMs)
                val width = (size?.width ?: 0.0).roundToInt().coerceAtLeast(0)
                val height = (size?.height ?: 0.0).roundToInt().coerceAtLeast(0)
                val baseline = (engine?.firstBaseline ?: 0.0).roundToInt()
                layout(width, height, mapOf(FirstBaseline to baseline)) {}
            },
        )
    }
}

// MARK: what the composable owns

/**
 * The engine and its store, kept across recompositions.
 *
 * The clock is one field, read by the measure policy and by the draw lambda and
 * written by exactly one place. Nothing else in the composable reads it, so a
 * frame invalidates this node's measure and draw and nothing outside it.
 */
@Stable
internal class MorphHost {
    var store: LayoutStore? = null
        private set
    var engine: MorphEngine? = null
        private set

    /** Now, in milliseconds on the frame clock's own base. */
    var nowMs: Double by mutableDoubleStateOf(0.0)

    /** Whether the frame loop should be asking for frames. */
    var isRunning: Boolean by mutableStateOf(false)
        private set

    /** Bumped when a morph starts, to restart the frame loop. */
    var runId: Int by mutableIntStateOf(0)
        private set

    private var settledAtMs = 0.0

    fun configure(
        typeface: Typeface,
        textSize: Float,
        letterSpacing: Float,
        options: TextMorphOptions,
        alignment: MorphAlignment,
        reduceMotion: Boolean,
    ) {
        val store = store ?: LayoutStore(typeface, textSize, letterSpacing).also { this.store = it }
        val resized =
            store.shapingPaint.textSize != textSize ||
                store.shapingPaint.typeface != typeface ||
                store.shapingPaint.letterSpacing != letterSpacing
        store.use(typeface, textSize, letterSpacing)

        val engine = engine
        if (engine == null) {
            this.engine = MorphEngine(store, options, alignment, reduceMotion)
            return
        }
        engine.configure(options, alignment, reduceMotion)
        // A face or a size that changed relays the value out and does not
        // animate it: a size category is not a morph.
        if (resized) engine.relayout()
    }

    /** Shows a value, and runs the clock for as long as it takes. */
    fun show(
        value: MorphValue,
        cursorIndex: Int?,
        callbacks: MorphCallbacks,
    ) {
        val engine = engine ?: return
        val now = System.nanoTime() / 1_000_000.0
        nowMs = now
        if (!engine.update(value, cursorIndex, callbacks, now)) return

        val render = engine.render(now)
        if (render == null || render.isSettled) {
            isRunning = false
            return
        }

        // A generous tail: a ghost from an interrupted morph can outlast the one
        // that replaced it, and the cost of a few extra frames is nothing next to
        // a morph that stops half way.
        settledAtMs = now + render.plan.durationMs + TAIL_MS
        isRunning = true
        runId += 1
    }

    /** Called by the frame loop, which stops when nothing is moving. */
    fun tick(nowMs: Double) {
        this.nowMs = nowMs
        if (nowMs >= settledAtMs) isRunning = false
    }

    private companion object {
        const val TAIL_MS = 100.0
    }
}

// MARK: the platform's own settings

/**
 * Whether the system asks for less motion.
 *
 * Android has no reduce-motion switch of its own; what it has is a developer and
 * accessibility setting that scales every animator's duration, and zero is what
 * both the accessibility guidance and the platform's own code treat as "do not
 * animate".
 */
@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

// MARK: resolving the description

/** The size in pixels, through the density so Android 14's non-linear scaling applies. */
internal fun TextMorphFont.pixelSize(density: Density): Float =
    with(density) {
        if (fontSize.type == TextUnitType.Em) {
            TextMorphFont.DEFAULT_SIZE.toPx() * fontSize.value
        } else {
            fontSize.toPx()
        }
    }

/** The extra space between characters, in ems, which is what a paint takes. */
internal fun TextMorphFont.letterSpacingEm(): Float =
    if (letterSpacing == TextUnit.Unspecified) 0f else letterSpacing.value

/**
 * The face, resolved through Compose's own resolver.
 *
 * A `FontFamily` is a description, not a face: it can name a resource, a set of
 * files, or the system default, and only the resolver knows how the composition
 * has been configured to find them. Going around it and asking the platform for
 * a typeface by name would quietly ignore a font the app had provided.
 *
 * The resolver returns `Any` because the same interface serves every platform;
 * on Android it is always a `Typeface`, and anything else means the resolver
 * changed under this port, which the fallback makes visible rather than fatal.
 */
internal fun TextMorphFont.typeface(resolver: FontFamily.Resolver): Typeface {
    val resolved = resolver.resolve(fontFamily, fontWeight, fontStyle, fontSynthesis).value
    return resolved as? Typeface ?: Typeface.DEFAULT
}
