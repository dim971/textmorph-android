<div align="center">

# TextMorph for Jetpack Compose

**A native Compose port of [Torph](https://torph.lochie.me) by [Lochie Axon](https://github.com/lochie).**

Text continuity for native interfaces. When a value changes, the characters,
words and digits that survive the change move to their new place instead of
disappearing in a cross-fade.

[![CI](https://github.com/dim971/textmorph-android/actions/workflows/ci.yml/badge.svg)](https://github.com/dim971/textmorph-android/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-blueviolet.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-24%2B-lightgrey.svg)](#requirements)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-0.1.0-blue.svg)](#installation)
[![Licence](https://img.shields.io/badge/licence-MIT-blue.svg)](LICENSE)

<img src="docs/images/hero.png" width="320" alt="A balance of $1,204.00 in the showcase app">

</div>

A number is the case that makes the argument. 1,204 becoming 1,318 rolls the
hundreds and the tens and leaves the thousands alone, because a digit's
identity is its column rather than its position in the string. A cross-fade
cannot do that, and neither can a composable that animates a string.

```kotlin
TextMorph(
    value = total,
    options = TextMorphOptions(decimals = 2),
    font = TextMorphFont(fontSize = 40.sp, fontWeight = FontWeight.SemiBold),
)
```

Its twin is [textmorph-ios](https://github.com/dim971/textmorph-ios). The two
ports mirror each other file for file and replay the same fixtures.

## Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Quick start](#quick-start)
- [Options](#options)
- [Numbers](#numbers)
- [Motion and accessibility](#motion-and-accessibility)
- [Fidelity](#fidelity)
- [Showcase app](#showcase-app)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [Credits](#credits)
- [Licence](#licence)

## Requirements

API 24 upwards, JVM 17, Compose. No dependency beyond Compose itself.

## Installation

```kotlin
dependencies {
    implementation("io.github.dim971:textmorph-compose:0.1.0")
}
```

```kotlin
import io.github.dim971.textmorph.compose.TextMorph
```

## Quick start

```kotlin
@Composable
fun Balance() {
    var total by remember { mutableDoubleStateOf(1204.0) }

    Row(Modifier.clickable { total -= 86 }) {
        Text(
            text = "Total",
            modifier = Modifier.alignByBaseline(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextMorph(
            value = total,
            modifier = Modifier.alignByBaseline(),
            options = TextMorphOptions(decimals = 2),
            font = TextMorphFont(fontSize = 40.sp, fontWeight = FontWeight.SemiBold),
            colour = MaterialTheme.colorScheme.onSurface,
        )
    }
}
```

A morph shapes its own text and fills its own glyphs, so the face, the size and
the colour are parameters rather than values read out of a `LocalTextStyle`:
there is no text node to inherit through. Align on baselines rather than on
frames, because a morph's box is the font's own metrics with no font padding.

## Options

| Option | Default | What it does |
| --- | --- | --- |
| `duration` | `400.0` | How long a morph takes, in milliseconds. Ignored for a spring. |
| `ease` | `Default` | A cubic bezier or a spring. The default is a long ease out. |
| `scale` | `true` | Whether a leaving segment shrinks as it goes. |
| `numbers` | `true` | Whether a quantity morphs by place value. |
| `decimals` | `null` | Fraction digits, minimum and maximum. `null` means at most three. |
| `locale` | `en` | The decimal separator, and how a number is formatted. |
| `debug` | `false` | Outlines every segment by what it is doing. |
| `disabled` | `false` | The value arrives already in place. |
| `respectReducedMotion` | `true` | Whether the system animation scale does the same. |

Exactly one of `onComplete` and `onCancel` runs per morph, enforced by a
single-shot token rather than by care.

## Numbers

A quantity is recognised strictly, and on purpose: `$1,234.56`, `12%`, `(12)`
and `-5` are quantities, while `COVID-19`, `2024-01-01` and `v1.4.2` are not,
so a date or a version morphs character by character. Digits pair by column
from the decimal separator, arriving from above while the symbols around them
arrive from below.

A field being typed into wants a different answer, and gets one from
`cursorIndex`: both sides of the caret hold their identity and only the
keystroke is new.

<p align="center">
<img src="docs/images/wallet.png" width="230" alt="Wallet">
<img src="docs/images/ticker.png" width="230" alt="Ticker">
<img src="docs/images/field.png" width="230" alt="Number field">
</p>

See [docs/numbers.md](docs/numbers.md).

## Motion and accessibility

The clock is linear and every curve is applied by the library, because upstream
animates transforms with the author's curve and opacity linearly over a
fraction of the duration. `withFrameNanos` supplies elapsed milliseconds and
nothing else: no `tween`, no `Animatable`. A spring is solved here rather than
delegated, because no platform spring exposes its settling threshold and the
settling duration is what every fade window is a fraction of.

A system animation scale of zero turns the morphing off on its own. The plain
value is exposed as one semantics node, so a screen reader reads the value and
not sixty fragments of it. The font size is in scaled pixels and goes through
`Density.toPx`, so the system font scale applies and Android 14's non-linear
scaling is honoured rather than approximated.

One honest limitation: a morph draws its own glyphs, so **the value cannot be
selected**, in any mode. It is for values a reader watches change. Body copy
wants `BasicText`.

## Fidelity

The engine is pinned to the JavaScript original by 8770 cases generated from
the published `torph@0.1.3` package, plus the 2710 cases of Unicode's own UAX
#29 conformance suite. There is one tolerance in the whole suite and
[docs/fidelity.md](docs/fidelity.md) says exactly where it is and why.

The deviations are deliberate, and one of them is a fix: at a damping ratio of
exactly one, upstream's spring divides by zero and reports a duration of minus
zero, so `damping = 20.0` at the default stiffness animates nothing. This port
adds the analytic critical branch, identically on both platforms.

## Showcase app

Fifteen demos, each naming the one capability of the engine it exists to show,
plus a playground where an option can be watched on and off over the same
value.

<p align="center">
<img src="docs/images/catalog.png" width="230" alt="Catalog">
<img src="docs/images/playground.png" width="230" alt="Playground">
<img src="docs/images/reflow.png" width="230" alt="Reflow">
</p>

```sh
./gradlew :showcase:installDebug
```

## Documentation

| Document | What it covers |
| --- | --- |
| [getting-started.md](docs/getting-started.md) | Installing it, and the first few things to try |
| [api.md](docs/api.md) | Every option, both utilities, and what this is not |
| [numbers.md](docs/numbers.md) | How a quantity rolls, and what counts as one |
| [architecture.md](docs/architecture.md) | How a value reaches the screen |
| [fidelity.md](docs/fidelity.md) | How this is held to the original |
| [coding-style.md](docs/coding-style.md) | The style contract, and what is enforced |

## Contributing

Issues and pull requests are both welcome. See
[CONTRIBUTING.md](CONTRIBUTING.md) for how to build it and what a review looks
for, and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for how to take part.

## Credits

[Torph](https://torph.lochie.me) is by [Lochie Axon](https://github.com/lochie).
The idea, the timing, the place-value alignment and the strictness about what
counts as a quantity are all his; this repository is the port.

## Licence

MIT. See [LICENSE](LICENSE). Torph is MIT, copyright 2025 Lochie Axon; see
[NOTICE](NOTICE) for the attribution and for exactly which files are derived
from it.
