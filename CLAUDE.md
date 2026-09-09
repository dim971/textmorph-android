# TextMorph for Jetpack Compose: working notes

A Jetpack Compose port of the web library [Torph](https://torph.lochie.me)
(MIT, copyright 2025 Lochie Axon). Its twin is
[textmorph-ios](https://github.com/dim971/textmorph-ios); the two are kept in
step deliberately.

Upstream has one public component, `<TextMorph />`. The 33 entries on its
examples page are demos built with it, not components of it. This port has one
public composable for the same reason.

## The one rule that matters

**The engine's output is pinned to the JavaScript original.**

`textmorph/src/test/resources/goldens.json` is generated from the published npm
package by `Tools/gen-goldens.mjs`, and is byte identical to the iOS twin's
copy. The tests rebuild each case through the Kotlin engine and compare.

There is exactly one tolerance in the whole suite, and it is worth knowing
where. The spring's *position* is compared to within the ulp of one, because
V8's `Math` is a port of fdlibm rather than fdlibm itself: `StrictMath.cos` and
V8's `Math.cos` disagree in the last bit at exactly one of the 357 sampled
points. The bound is tied to one rather than to the value because every branch
computes `1 - something`. Nothing else has any tolerance, and in particular the
spring's *duration* does not: that number is a fraction of every fade window in
the library, and it agrees exactly across the whole parameter matrix.

If a golden fails, the port has drifted. Find out why. Do not widen anything,
do not regenerate the fixtures to make a failure go away. Regenerating is only
correct when deliberately tracking a new upstream version, and the diff should
be explainable.

## Deliberate deviations from upstream

They are listed in full in `docs/fidelity.md`, with their own fixture sections
marked as deviating.

- **Minted numeric ids.** Upstream mints them from a module-global counter,
  which is not reproducible even in JavaScript, since the counter climbs for
  the life of the process. The port passes a `MintedIds` instance, owned by the
  composable's engine. Upstream only requires uniqueness against the ids
  already in play, so the behaviour is the same. Fixtures canonicalise any id
  starting with U+0000 to `#0`, `#1` and so on, in order of first appearance.
- **Character splitting.** Upstream uses `String.prototype.split("")`, which
  cuts on UTF-16 code units and therefore halves an astral character into two
  surrogates. The iOS twin cannot hold an unpaired surrogate, so both ports
  split on extended grapheme clusters. This changes ids and the character diff
  only for words containing an astral character, and it is the one place where
  this port does *not* simply do what the JVM makes easy.
- **Critical damping.** Upstream's spring is analytic, and at a damping ratio
  of exactly 1 its overdamped branch divides by zero: `springPosition` returns
  NaN, and because `NaN > precision` is false, `computeDuration` concludes the
  spring settled at t = 0 and returns a duration of `-0` ms. The port adds the
  analytic critical branch. Deviating identically on both platforms is the
  requirement; following upstream here would ship a broken option.
- **U+2019 is a separator between digits.** CLDR groups Swiss German with
  U+0027 in one ICU version and U+2019 in another, and the device's OS version
  decides which. Without the addition the same number would roll by place value
  on one OS version and morph character by character on the next.
- **Two CLDR gaps are recorded rather than papered over.** `java.text` does not
  expose CLDR's minimum grouping digits, so es-ES, it-IT and pl-PL leave a
  four-digit number ungrouped where `Intl` groups it, and it does not do
  non-uniform grouping, so hi-IN gives 1,234,567 where `Intl` gives 12,34,567.
  `NumberFormattingTest` asserts that set exactly, so a locale joining or
  leaving it is a failure rather than a silent change.

## What the rendering probe established

Measured, not assumed, before any of this was written. `docs/architecture.md`
carries the numbers.

- **Positions and glyphs must come from the same `Paint`.** Taking positions
  from a `TextLayoutResult` and glyphs from a `TextPaint` built from the same
  `TextStyle` loses 2.35px on a tracked value: the two do not resolve the font
  and the letter spacing identically. So this port measures with
  `Paint.getRunAdvance` and draws with `Canvas.drawTextRun`, both given the
  whole line as shaping context and the segment as the range, which is the same
  guarantee CoreText gives the twin.
- **Ligatures are disabled** on the paint. Unlike the system font on Darwin,
  Android's default font does form them, and a ligature straddling a segment
  boundary would be drawn twice or not at all.
- **Splitting the value into segments then costs nothing**: a value drawn one
  run per segment is identical to the same line drawn in one call.

## Layout

```
textmorph/src/main/kotlin/io/github/dim971/textmorph/
  core/      pure logic, no UI type in any signature. Segmentation, the diff,
             number place alignment, the spring, the bezier, the morph plan.
  engine/    text shaping, the layout cache, the clock, the state machine.
  compose/   the public TextMorph composable, its options, the canvas.
textmorph/src/test/     goldens, plan samples, layout drift, interruption
showcase/               the catalogue app
Tools/                  gen-unicode-tables.mjs, gen-goldens.mjs: Node, by hand
```

`docs/architecture.md` explains how a value reaches the screen.

## Conventions worth keeping

- **Zero warnings.** Library and showcase, with warnings as errors.
- **One linear clock.** The platform supplies elapsed time and nothing else:
  `withFrameNanos` writing into a `MutableFloatState` that is read only inside
  the draw lambda. Every curve is applied by us. Upstream animates transforms
  with the author's easing and opacity linearly over a fraction of the
  duration; if the clock itself were eased, recovering linear time would need
  the curve's inverse and every fade window would be wrong.
- **`JsMath` is `StrictMath`.** Never `kotlin.math` in `core`. V8 pins fdlibm,
  and `StrictMath` is fdlibm; `kotlin.math` is whatever the JVM's intrinsic
  does on the device.
- **Segmentation lives in `core`.** UAX #29 rules over generated tables, not
  `android.icu`. Foundation and Android's ICU disagree with each other and with
  `Intl.Segmenter`. Using each platform's ICU would guarantee a divergence
  rather than avoid one.
- **`UnicodeBreakTables.kt` is generated.** Never edit it by hand. It encodes
  its ranges as hex strings decoded at class init, because an `intArrayOf` of
  eight thousand elements does not fit the JVM's 64KB static initialiser.
- Comments explain why, not what. If a constant looks arbitrary, say where it
  came from.
- **No em dash (U+2014), and no en dash (U+2013).** Not in code, comments,
  docs, commit messages or issue and pull request text. Use a comma, a colon, a
  semicolon, parentheses or a full stop. CI fails the build if either character
  reappears.

## Verifying

```sh
./gradlew :textmorph:testDebugUnitTest      # goldens
./gradlew ktlintCheck lint                  # style
./gradlew :showcase:installDebug            # build and run
```

Screenshots: `adb exec-out screencap -p > out.png`. A morph is a transition, so
a still frame proves little; prefer `adb shell screenrecord`, or assert on
`MorphPlan.frame(elapsed)`, which is pure.

## Staying in step with iOS

A change to shared behaviour (segmentation, the diff, number alignment, the
timing of a morph, the shape of `MorphPlan`) should land in both repositories.
The Swift port mirrors `core` file for file, `Tools/` is byte identical, and
both replay the same `goldens.json`.
