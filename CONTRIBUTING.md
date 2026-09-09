# Contributing

Thanks for taking a look. Issues and pull requests are both welcome.

## Getting set up

```sh
git clone https://github.com/dim971/textmorph-android
cd textmorph-android
./gradlew :textmorph:testDebugUnitTest    # the engine goldens
./gradlew :showcase:installDebug          # the catalogue app
```

You need JDK 17 or later and an Android SDK with API 37 installed. The
showcase run configuration is checked in, so the app is launchable from Android
Studio straight after cloning. Regenerating the fixtures also needs Node 20+,
but that is a deliberate act rather than part of a normal build.

## Before you open a pull request

```sh
./gradlew :textmorph:testDebugUnitTest ktlintCheck lint
```

Three things the review will look for:

**No warnings.** The library and the showcase both compile with warnings as
errors. A warning that is tolerated becomes a warning that is ignored.

**The goldens still pass.** If you touch anything under
`textmorph/src/main/kotlin/io/github/dim971/textmorph/core`, the fixtures are
the contract. See [docs/fidelity.md](docs/fidelity.md). Changing them means
changing what this library claims to be, so say why in the pull request.

**Parity with iOS.** This library has a
[twin](https://github.com/dim971/textmorph-ios). A change to shared behaviour
(segmentation, the diff, number alignment, the timing of a morph) should land
in both, or say plainly why it should not. The two repositories carry
byte-identical `goldens.json` and byte-identical `Tools/`, and CI checks the
digest.

## Conventions

[docs/coding-style.md](docs/coding-style.md) is the full version: the
[Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
as they apply here, plus the handful of places this project deliberately
differs. The short version:

- Comments explain *why*, not *what*. If a constant looks arbitrary, say where
  it came from.
- Every public declaration carries a KDoc comment, and the library is compiled
  with `-Xexplicit-api=strict`.
- `core` keeps upstream's positional signatures and upstream's names on
  purpose, so it can be read side by side with the JavaScript when a golden
  fails.
- `core` measures positions in UTF-16 offsets, which a Kotlin `String` gives
  for free and which the iOS twin has a whole type for.
- `core` uses `StrictMath` through `JsMath`, never `kotlin.math`, because V8's
  `Math` is a port of fdlibm and `StrictMath` is fdlibm.
- Commit messages describe the change and the reasoning, in prose.

## Reporting a bug

A morph is a transition between two values, so one value is never enough to
reproduce it. Give the value before, the value after, and the options in force.
If the report is about a number, say the locale: the decimal separator is the
pivot every digit alignment is measured from, and some locales group with a
non-breaking space.

## Code of conduct

Taking part means following the [Code of Conduct](CODE_OF_CONDUCT.md).
