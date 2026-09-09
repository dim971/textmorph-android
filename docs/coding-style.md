# Coding style

The [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
as they apply here, plus the places this project deliberately differs.

## Naming

`core` keeps upstream's names. `segmentText`, `diffSegments`, `lcsIndices`,
`placeMatch`, `numericSkeleton`: they are not idiomatic Kotlin and they are not
meant to be. When a golden fails, the two files have to be readable side by
side, and a renamed function costs more in that moment than it earns in every
other.

Everything above `core` is named for a Kotlin reader.

The file names in `core` are the iOS twin's file names, module extension aside.
Three of them hold one type plus the free functions that only exist to keep a
file under the length the lint allows, so ktlint's rule that a file be named
after its single class is turned off: the naming that matters here is the
cross-repository one, and it is written down in `.editorconfig` with the reason.

## Documentation

Every public declaration carries a KDoc comment, and the library is compiled
with `-Xexplicit-api=strict`, so a declaration cannot become public by accident.
A doc comment says what the thing is for, and where a value looks arbitrary it
says where the value came from. "0.45" means nothing; "a digit that has already
left is a hole in the number, so the outgoing share is larger" means something.

Strict explicit API is scoped to the library's own sources. Tests have no
published surface, so it would only earn them an `internal` on every declaration
and teach the reader nothing.

## Comments explain why, not what

The code says what it does. A comment is for the reason, and in a port the
reason is usually "upstream does this, and here is what breaks if it does not".

Two kinds of comment are especially worth writing here:

- **A reproduced quirk.** Anything that looks like a mistake and is deliberate
  needs to say so, or the next reader fixes it and a golden turns red with no
  explanation.
- **A measurement.** Numbers that came out of a probe rather than out of a
  document belong next to the code that depends on them. "2.35px on a tracked
  value" is checkable; "the two measurements disagree" is not.

## Maths

`core` calls `JsMath`, which is `StrictMath`, and never `kotlin.math`. V8 pins
fdlibm and `StrictMath` is fdlibm; `kotlin.math` is whatever intrinsic the
device's JVM happens to have, which is free to differ in the last bit and free
to differ between devices. The spring's settling duration is an integer produced
by a threshold crossing, so a last-bit difference can move it a whole
millisecond and shift every fade window in the library.

## Units

`core` counts string positions in UTF-16 code units, which a Kotlin `String`
gives for free and which the iOS twin has a whole type for. `UTF16Offset` exists
here anyway, because a type present on one side and absent on the other is a
place where two ports drift.

Distances are in pixels. Times are in milliseconds, because upstream's are.

## Generated files

`UnicodeBreakTables.kt` is generated and is excluded from linting, so that the
checked-in file is exactly what the generator produces and "regenerate and see
no diff" stays a check. Nothing about it is meant to be read. The same goes for
the two fixture files.

A generator names, in its own header, the exact command that produces its output
and the upstream version it is pinned to. Regenerating is a deliberate act.

## No em dash

No em dash (U+2014) and no en dash (U+2013). Not in code, comments, docs, commit
messages or issue and pull request text. Use a comma, a colon, a semicolon,
parentheses or a full stop. CI fails the build if either character reappears.

## What is enforced mechanically

| Rule | Enforced by |
| --- | --- |
| Formatting, wrapping, spacing, import order | `ktlintCheck` |
| Nothing public by accident | `-Xexplicit-api=strict` |
| Zero build warnings | `allWarningsAsErrors` in both modules |
| Platform API levels, resources, manifest | `lintDebug` |
| No em dash or en dash | CI, first step |
| The engine matches the original | the golden fixtures |
| The generated tables are the generator's output | CI regenerates and diffs |
| The two ports match each other | the fixture and generator digests, in CI |

Everything else in this document is a review conversation.
