# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0]

First release. A Jetpack Compose port of [Torph](https://torph.lochie.me) 0.1.3.

### Added

- `TextMorph`, one composable mirroring upstream's one component, with every
  option upstream takes: duration, easing as a cubic bezier or a spring, scale,
  place-value number morphing, fraction digits, locale, debug, disabled and
  reduce-motion.
- Place-value alignment for quantities, with caret matching for a field being
  typed into.
- The two utilities upstream exports, `segmentText` and `diffSegments`.
- UAX #29 grapheme and word segmentation, ported over generated tables rather
  than delegated to `android.icu`, so the two ports cannot diverge with an OS
  version. All 766 grapheme and 1944 word cases of Unicode's own conformance
  suite pass.
- Fixtures generated from the published npm package, 8770 cases, replayed by
  the test suite and byte identical to the iOS twin's copies. One tolerance in
  the whole suite, on the spring's position, documented in `docs/fidelity.md`.
- A showcase catalogue of fifteen demos and a playground.
- Six documents under `docs/`.

### Changed from upstream

- **Critical damping is fixed.** At a damping ratio of exactly one, upstream's
  spring divides by zero, returns NaN for every time, and then reports a
  settling duration of minus zero, so `damping = 20.0` at the default stiffness
  and mass animates nothing at all. This port adds the analytic critical
  branch.
- **Minted numeric identities are owned rather than global.** Upstream's
  counter is a module-global that climbs for the life of the process, which
  makes the same call return different identities depending on what ran before
  it. Behaviour is unchanged, because uniqueness is what upstream actually
  requires.
- **Characters are split on grapheme clusters**, not UTF-16 code units, because
  the iOS twin cannot hold an unpaired surrogate and deviating identically on
  both platforms is worth more than being closer to upstream on one of them.
  This changes identities only for words holding an astral character.
  Everything else stays on UTF-16 offsets.
- **Dictionary word breaking is not implemented.** A run of CJK letters in a
  value that holds a space elsewhere stays one word where ICU cuts it into
  lexical words. Upstream already segments those languages by grapheme whenever
  the value has no space, so the difference is confined to spaced CJK.
- **A morph is never selectable.** It draws its own glyphs, and the
  `BasicText` that would restore selection when nothing is moving does not land
  in the same place, so there is one rendering path rather than a visible jump.
- **U+2019 is a separator between digits, and is not upstream.** CLDR groups
  Swiss German with U+0027 in one ICU version and U+2019 in another, and the
  device's OS decides which; without the addition, the same number would roll
  by place value on one OS version and morph character by character on the
  next.
- **Two CLDR gaps in `java.text` are recorded rather than papered over.**
  Minimum grouping digits leave a four-digit number ungrouped in es-ES, it-IT
  and pl-PL where Intl groups it, and hi-IN groups by lakh where this groups by
  thousand. `NumberFormattingTest` asserts that set exactly, so a locale
  joining or leaving it is a failure rather than a silent change.
- **The font and the colour are parameters rather than inherited.** A morph
  shapes its own text and fills its own glyphs, so there is no text node to
  read a `LocalTextStyle` or a `LocalContentColor` through.

[Unreleased]: https://github.com/dim971/textmorph-android/compare/0.1.0...HEAD
[0.1.0]: https://github.com/dim971/textmorph-android/releases/tag/0.1.0
