# Getting started

## Install

```kotlin
dependencies {
    implementation("io.github.dim971:textmorph-compose:0.1.0")
}
```

```kotlin
import io.github.dim971.textmorph.compose.TextMorph
```

Compose and nothing else. API 24 upwards.

## Your first morph

```kotlin
@Composable
fun Balance() {
    var total by remember { mutableDoubleStateOf(1204.0) }

    TextMorph(
        value = total,
        options = TextMorphOptions(decimals = 2),
        font = TextMorphFont(fontSize = 40.sp, fontWeight = FontWeight.SemiBold),
    )
}
```

Change `total` and the digits that survive the change roll to their new place.
1,204 becoming 1,318 rolls the hundreds and the tens and leaves the thousands
alone, because a digit's identity is its column rather than its position in the
string.

## Saying what it looks like

A morph shapes its own text, so the face and the size are parameters rather
than something read out of a `LocalTextStyle`: there is no text node to inherit
from, and a `TextStyle` carries more than a shaper can use.

```kotlin
TextMorph(
    text = status,
    font = TextMorphFont(
        fontFamily = FontFamily.Monospace,
        fontSize = 22.sp,
        fontWeight = FontWeight.Medium,
    ),
    colour = MaterialTheme.colorScheme.onSurface,
)
```

The size is in scaled pixels and is converted through the composition's
`Density`, so the system font scale applies and Android 14's non-linear scaling
is honoured rather than approximated.

The colour has no default worth inheriting either, so it is a parameter. Pass
one from the theme; the default is black, which is right in a light theme and
wrong in a dark one.

## Lining it up with other text

A morph publishes its first baseline, so `Modifier.alignByBaseline()` in a
`Row` puts it on the same line as a text node beside it:

```kotlin
Row {
    Text(
        text = "Total",
        modifier = Modifier.alignByBaseline(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TextMorph(
        value = total,
        modifier = Modifier.alignByBaseline(),
        options = TextMorphOptions(decimals = 2),
    )
}
```

Its box is not the same shape as a text node's: a morph uses the font's own
metrics, with no font padding. Align on baselines, not on frames.

## A value that is not a number

```kotlin
TextMorph(text = status)
```

A value holding a space or a line break is cut into words, so the words that
survive a change move and the rest arrive and leave. A value without one is cut
into grapheme clusters and morphs letter by letter, which is what a code, a
label or a price wants.

## A field being typed into

Pass the caret, and both sides of the edit hold their identity:

```kotlin
var entry by remember { mutableStateOf("") }
var caret by remember { mutableStateOf<Int?>(null) }

TextMorph(value = entry.toDoubleOrNull() ?: 0.0, cursorIndex = caret)

OutlinedTextField(
    value = entry,
    onValueChange = { entry = it; caret = it.length },
)
```

Without a caret the digits realign by column, which is the right answer for a
magnitude and the wrong one for a field: carrying 123 to 1,234 is a
two-character delta of which the reader typed one, and the two are not
adjacent.

## Motion

```kotlin
TextMorph(value = amount, options = TextMorphOptions(duration = 600.0))

TextMorph(
    value = amount,
    options = TextMorphOptions(
        ease = TextMorphEase.spring(stiffness = 150.0, damping = 19.0, mass = 1.2),
    ),
)
```

A spring settles on its own physics and ignores `duration`.

## When it should not move

```kotlin
TextMorph(value = amount, options = TextMorphOptions(disabled = true))
```

The system's animation scale being zero does the same thing on its own, unless
`respectReducedMotion` is turned off. In either case the value arrives already
in place and no callback fires: the morph did not fail, it was switched off.

## Where to go next

- [api.md](api.md) for every option.
- [numbers.md](numbers.md) for how a quantity rolls, and what counts as one.
- [architecture.md](architecture.md) for how a value reaches the screen.
- [fidelity.md](fidelity.md) for how this is held to the original.
