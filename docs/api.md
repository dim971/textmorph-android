# API

One composable, two utilities, and a set of options. Upstream has one component
and this has one composable for the same reason: the thirty-three entries on
its examples page are demos built with it, not parts of it.

## TextMorph

```kotlin
TextMorph(text: String, modifier, options, font, colour, textAlign, cursorIndex, callbacks)
TextMorph(value: Double, modifier, options, font, colour, textAlign, cursorIndex, callbacks)
TextMorph(value: MorphValue, modifier, options, font, colour, textAlign, cursorIndex, callbacks)
```

A number is kept as a number rather than formatted by the caller, because the
formatting is part of the morph: the locale decides the decimal separator, and
the decimal separator is the pivot every digit alignment is measured from.

`font` and `colour` are parameters rather than inherited values. A morph fills
its own glyphs and shapes its own text, so there is no text node to read a
`LocalTextStyle` or a `LocalContentColor` through, and a `TextStyle` carries
more than a shaper can use.

## Options

| Option | Default | What it does |
| --- | --- | --- |
| `duration` | `400.0` | How long a morph takes, in milliseconds. Ignored when `ease` is a spring. |
| `ease` | `TextMorphEase.Default` | `Bezier(_)` or `spring(stiffness, damping, mass, precision)`. The default is a long ease out, so most of a morph happens in its first quarter. |
| `scale` | `true` | Whether a leaving segment shrinks as it goes. Only affects what leaves; an arriving segment scales either way. |
| `numbers` | `true` | Whether a numeric word morphs by place value. Off falls back to the character morph, which is what a version number wants. |
| `decimals` | `null` | Fraction digits for a numeric value, setting both the minimum and the maximum, so `2` pads as well as truncates. `null` means at least none and at most three, which is Intl's own default. |
| `locale` | `en` | Decides the decimal separator and how a numeric value is formatted. Deliberately not the device's: taking it from there would make the same value morph differently on two phones. |
| `debug` | `false` | Outlines every segment: blue for one that stays, green for one arriving, red for one leaving. |
| `disabled` | `false` | The value arrives already in place. |
| `respectReducedMotion` | `true` | Whether a system animation scale of zero does the same. |

## TextMorphFont

```kotlin
TextMorphFont(
    fontFamily = FontFamily.Monospace,
    fontSize = 26.sp,
    fontWeight = FontWeight.SemiBold,
    fontStyle = FontStyle.Normal,
    letterSpacing = 0.02.em,
)
```

The family is resolved through the composition's own `FontFamily.Resolver`, so
a font the app provided is found; asking the platform for a typeface by name
would quietly ignore it. The size is converted through `Density.toPx`, so the
system font scale applies and Android 14's non-linear scaling is honoured.

Letter spacing goes on the paint rather than being added afterwards, so it is
part of the shaping and therefore part of every offset the layout is built
from.

## Callbacks

```kotlin
TextMorph(
    value = amount,
    callbacks = MorphCallbacks(
        onStart = { },
        onComplete = { },
        onCancel = { },
    ),
)
```

`onStart` never fires for the first value a composable shows, and never for an
update whose formatted value equals the one already on screen.

**Exactly one of `onComplete` and `onCancel` runs per morph.** That is enforced
by a single-shot token rather than by care. A morph replaced before it finished
is cancelled and never completed. Switching the morphing off fires neither: the
morph did not fail.

The callbacks are deliberately not part of `TextMorphOptions`, so a composable
that passes a fresh lambda on every recomposition does not restart a morph in
flight.

## Layout direction and alignment

`textAlign` decides which edge the lines of a multi-line value line up on, and
null follows the layout direction: leading in a left-to-right layout, trailing in
a right-to-left one. It matters only for a value holding a line break, because
there is no automatic line breaking: a line exists only where the value put one.

`TextAlign.Justify` has no meaning here and reads as start. Justifying needs a
measured width to stretch a line to, and this library never wraps, so every line
is already its natural width.

## Utilities

```kotlin
TextSegmenter.segmentText(value: String, locale, numbers): List<Segment>
SegmentDiff.diffSegments(oldSegments: List<Segment>, newText: String, locale, options): DiffResult
```

Upstream exports these and so does this. They are the engine with the drawing
taken away: `segmentText` cuts a value into the pieces a morph is expressed in,
and `diffSegments` says which of them survive a change of value.

Identities are only comparable between calls that share a minter, which the
public overloads do not. Pass values through one `TextMorph` if you need them
to line up.

## What it is not

A morph draws its own glyphs, so **the value cannot be selected**, in any mode.
It is meant for values a reader watches change: counters, prices, labels,
statuses. Body copy wants `BasicText`.

Right-to-left values render and morph correctly. Mixed-direction values render
correctly and morph approximately: a segment is a contiguous logical range, and
under bidi its visual extent can be split, so the perceived motion does not
always read as sensible movement. Per-character morphing of a joining script is
a non-goal.
