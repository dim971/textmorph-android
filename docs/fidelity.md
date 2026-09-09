# Fidelity

This library claims to be a port. That is a claim about behaviour, and it is
checked rather than asserted.

## How it is checked

`Tools/gen-goldens.mjs` drives the published `torph@0.1.3` and records what it
does. The tests replay it through the Kotlin engine and compare. 8770 cases in
all:

| Section | Cases | Compared |
| --- | --- | --- |
| UAX #29 conformance, grapheme and word | 2710 | exactly |
| Number formatting, 25 values against 6 fraction lengths against 31 locales | 4650 | exactly, except the CLDR gaps below |
| The diff, every ordered pair of a 22-value corpus, numbers on and off | 976 | exactly |
| Segmentation | 100 | exactly, except four recorded as diverging |
| Numeric words, decimal separators, place and caret alignment | 173 | exactly |
| Bezier samples, slopes, carried curves | 53 | exactly |
| FLIP anchors and replaced runs | 91 | exactly |
| The spring | 17 | the duration exactly, the position to within an ulp |

The Unicode conformance section is not a fixture of upstream at all: it is the
standard's own test suite. If one of those fails, the rules are wrong
independently of anything torph does.

The fixture files are byte identical to the iOS twin's copies, and both
repositories run the same generators.

## Regenerating

```sh
cd Tools && npm install --no-save torph@0.1.3 && cd ..
node Tools/gen-goldens.mjs textmorph/src/test/resources/goldens.json
node Tools/gen-unicode-tables.mjs \
  --kotlin textmorph/src/main/kotlin/io/github/dim971/textmorph/core/UnicodeBreakTables.kt \
  --tests textmorph/src/test/resources/unicode-break-tests.json
```

**If a golden fails, the port has drifted.** Find out why. Do not widen
anything, and do not regenerate to make a failure go away. Regenerating is only
correct when deliberately tracking a new upstream version, and the diff has to
be explainable, in both repositories, with `.github/parity-digests.txt` updated
in the same commit.

## The one tolerance

The spring's *position* is compared to within the ulp of one, and exactly one of
the 357 sampled positions needs it.

`JsMath` is `StrictMath`, which is fdlibm, and V8's `Math` is a *port* of fdlibm
rather than fdlibm itself. Almost everywhere that comes to the same bits. The
one place it does not was traced rather than absorbed: at stiffness 100, damping
5, mass 1, sampled at t = 0.60425, `cos(5.850625467364579)` is
`0x3fed0d7b2dbe7bad` in V8 and `0x3fed0d7b2dbe7bac` under `StrictMath`. `exp`,
`sin` and `sqrt` agree exactly at the same point, and `sqrt` is exactly rounded
by IEEE 754 so it always will.

The bound is tied to one rather than to the value because every branch of the
position function computes `1 - something`, so the result inherits the absolute
accuracy of a quantity near one however small the result itself is.

The spring's *duration* has no tolerance at all, and that is the important half.
Every opacity window in the library is a fraction of it, and it is an integer
produced by a threshold crossing inside an accumulating loop, so a last-bit
difference could in principle move it by a millisecond and shift every fade.
Across the whole parameter matrix, including the near-critical damping ratios
and a precision sweep, it does not move. That is a measurement, not a guarantee,
which is why the matrix is as wide as it is.

Nothing else anywhere has a tolerance.

## Deliberate deviations

All forced or fixed, all recorded in the fixtures or asserted exactly in a test
rather than only in a comment.

**Minted numeric identities.** Upstream mints them from a module-global counter,
which is not reproducible even in JavaScript, since the counter climbs for the
life of the process. Here it is an object the composable's engine owns. Upstream
only requires uniqueness against the identities already in play, so the
behaviour is the same. The fixtures canonicalise any identity beginning with
U+0000 to `#0`, `#1` and so on, in order of first appearance.

**Character splitting.** Upstream uses `String.prototype.split("")`, which cuts
on UTF-16 code units and so halves an astral character into two surrogates. The
iOS twin cannot hold an unpaired surrogate. Both ports split on extended
grapheme clusters, which changes identities and the character diff only for
words holding an astral character. This is the one place where this port does
*not* simply do what the JVM makes easy: deviating identically on both platforms
is worth more than being closer to upstream on one of them.

Everything else stays on UTF-16 offsets, including `space-{index}`,
`newline-{offset}`, the character walk in the diff and the public `cursorIndex`.

**Critical damping.** At a damping ratio of exactly one, upstream's overdamped
branch makes its two roots equal, divides by their difference, and returns NaN
for every time; and because `NaN > precision` is false, the settling search then
concludes the spring settled before it started and reports a duration of minus
zero. So `damping = 20.0` at the default stiffness and mass, an ordinary thing
to ask for, animates nothing at all. The port adds the analytic critical branch.
The fixture records upstream's answer for the record and the test checks the
fixed branch is monotonic, finite and settles. Deviating identically on both
platforms is the requirement; following upstream would ship the same broken
option twice.

**Word segmentation for dictionary languages.** The UAX #29 rules here do not do
dictionary breaking, so a run of CJK letters in a value that holds a space
elsewhere stays one word where ICU cuts it into lexical words. Four fixture
cases record it, and a test asserts they still diverge, so the day one of them
starts agreeing is a test result rather than a surprise.

**One separator added.** U+2019 is allowed between digits here and is not
upstream. CLDR groups Swiss German with U+0027 in one ICU version and U+2019 in
another, and which one a device produces depends on its OS. Without U+2019 in
the set, the same de-CH number would roll by place value on one OS version and
morph character by character on the next, which is worse than widening a set by
a character that is a group separator in CLDR either way. The corpus carries a
U+2019 token and the test asserts the deviation exists, in that direction only:
a token upstream reads as a quantity and this does not is still a failure.

**Two CLDR gaps in `java.text`.** Measured, not guessed, and two different
causes. Minimum grouping digits: es-ES, it-IT and pl-PL leave a four-digit
number ungrouped where Intl groups it. Non-uniform grouping: hi-IN groups by
lakh, 12,34,567 rather than 1,234,567. `NumberFormattingTest` asserts that set
exactly, with both causes named, so a locale joining or leaving it is a failure
rather than a silent change, and the comparison is confined by a normalisation
that leaves anything else failing.

`android.icu` would close both, at the price of a formatter the JVM unit tests
cannot reach, which would move the whole 4650-case matrix onto an instrumented
test and off the machine that runs the goldens. That trade is not worth four
locales' grouping, and it is written down here rather than left as an omission.

**Rounding through `BigDecimal`.** Intl rounds the shortest decimal form of a
double; `NumberFormat` on the double rounds the binary value. 1.005 at two
fraction digits is 1.01 to Intl and 1.00 to `NumberFormat`.
`BigDecimal.valueOf` takes the shortest decimal form, which is what makes the
two agree, and it closed 62 of the 110 failures at a stroke.

## Two upstream quirks reproduced rather than tidied

Neither is a bug exactly, and both decide what moves.

The character subsequence inside a morphing word is computed over the old word's
characters but indexed into the old word's *segments*, and those are not the same
length when the word was already several segments, as `3.5 km/h` is. Upstream
checks the result instead of the index, so a pairing quietly does not happen.
Reproduced with the check made explicit.

A segment's kind is decided by calling a single-character digit test on a string,
and JavaScript compares strings lexicographically rather than refusing: `"12"`
reads as a digit because `"1"` sorts between `"0"` and `"9"`, `"0x1f"` does too,
and `"9x"` does not because it sorts after `"9"`. Reproduced with the rule
spelled out.

## What the fixtures cannot check

`MorphPlan` has no upstream counterpart: upstream writes keyframes and lets the
browser interpolate them. So the plan is checked against the timing table in
`MorphTiming`, which carries upstream's own numbers under upstream's own names,
and against properties: that sampling is total, that everything lands exactly at
rest, that opacity runs on linear time even where the curve does not. Those
assertions are the iOS twin's, over the same synthetic monospace metric, so the
two ports agreeing is something the machine checks.

The state machine is checked the same way, and the interruption path is the part
of upstream least exercised there: its own React wrapper never wires
`onAnimationCancel`. So the suite scripts twenty updates eight milliseconds
apart and asserts every morph accounts for exactly one of complete and cancel,
and that across two hundred frames no segment moves more than one character
between consecutive frames.

## Cross-platform

The iOS twin replays the same `goldens.json` and `unicode-break-tests.json`,
byte for byte, and runs the same generators. `.github/parity-digests.txt` is
byte identical in both repositories, and CI on both sides compares it against
what is checked in, so a divergence is a red build rather than a discovery
months later.
