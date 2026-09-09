# Numbers

The part of the library that makes the case for it. A counter that cross-fades
is a counter; a counter whose digits roll by column is a quantity changing.

## What counts as a quantity

Strict on purpose, and on by default. A token is a quantity when, after
trimming affixes from both ends, what is left begins and ends with a digit and
holds nothing but digits and separators.

| Token | Quantity | Why |
| --- | --- | --- |
| `1,204` | yes | |
| `$1,234.56` | yes | a currency symbol is an affix |
| `12%` | yes | so is a percent sign |
| `(12)` | yes | and a bracket |
| `-5` | yes | and a sign |
| `3.5` | yes | |
| `COVID-19` | no | does not begin with a digit after trimming |
| `2024-01-01` | no | a hyphen is not a separator between digits |
| `v1.4.2` | no | |
| `0x1f` | no | |

Merely holding a digit is not enough, and that is the point: a date, a version
or a product code should morph character by character, and it does.

Turning `numbers` off makes every quantity behave like an ordinary word.

## How a digit finds its place

Not left to right. A digit's identity is its column, measured from the decimal
separator, which is why the separator is called the pivot.

- The affixes at either end pair off first and are then out of the way. A digit
  stops that walk: it belongs to the magnitude, and the magnitude is what
  columns are for.
- On the integer side, the digits pair by column when the count is unchanged
  and by subsequence when it changed, so 999,999 becoming 1,000,000 carries
  what it can.
- On the fraction side they always pair by column: a decimal point fixes those
  columns, so 1.5 becoming 1.25 gains a hundredths place rather than sliding
  the 5 along.
- A separator holds its distance from the pivot, which is what slides the comma
  one group along on that carry. After a reshape it leaves instead, because it
  would otherwise have to cross the digits that carried, the two passing in
  opposite directions.
- Past three digits of difference in the integer count, nothing carries at all.
  The columns overlap into a smear and a replacement reads better. Three is
  where upstream's own corpus divides.

A digit arrives from above and a symbol from below, so each reads as its own
event rather than as the whole number shifting.

## The caret

A field being typed into wants a different answer. The reader's attention is on
the keystroke, not on the magnitude, so with `cursorIndex` both sides of the
caret map across unchanged and only the edit is new.

The caret walk skips the grouping separators, because a comma reflows with the
magnitude rather than with the keystroke: carrying 123 to 1,234 is a
two-character delta of which the reader typed one, and the two are not
adjacent.

The caret is honoured only when the value holds exactly one quantity. With
several there is no telling which one it is in.

## Locales

The locale decides two things: how a numeric value is formatted, and what the
decimal separator is. Since the separator is the pivot, changing the locale
moves every column.

`DecimalFormatSymbols` agrees with `Intl.NumberFormat` on all thirty-one
locales in the fixture corpus, including the Arabic U+066B, so that boundary
needed no reconciliation.

Two limitations worth knowing, both upstream's and both reproduced:

- Arabic-Indic digits and the U+066C group separator are outside the character
  sets, so an Arabic-formatted number is not a quantity and morphs character by
  character.
- Several locales group with a non-breaking space, which is also what separates
  words, so such a number is treated as several quantities side by side rather
  than one. The place-value roll then happens within each group rather than
  across the whole number.

## Formatting

`decimals` sets both the minimum and the maximum fraction length, so `2` pads
as well as truncates. Leaving it out means at least none and at most three,
which is what `Intl.NumberFormat` does and not an arbitrary choice.

Halves round away from zero: -1.5 becomes -2, not -1. That is `halfExpand`,
which is what Intl uses, and the fixtures pin it for negative values rather
than trusting the name of a rounding mode.

Rounding goes through `BigDecimal.valueOf`, and that is not incidental. Intl
rounds the *shortest decimal form* of a double, so 1.005 at two fraction digits
gives 1.01; `NumberFormat` on the double itself rounds the binary value, which
is a hair below, and gives 1.00. `BigDecimal.valueOf` takes the shortest decimal
form, which is what makes the two agree.

Two places where `java.text` does not carry all of CLDR, both measured and both
asserted exactly in `NumberFormattingTest` so that a locale joining or leaving
the set is a failure rather than a silent change:

- **Minimum grouping digits.** es-ES, it-IT and pl-PL leave a four-digit number
  ungrouped where Intl groups it.
- **Non-uniform grouping.** hi-IN groups by lakh: 12,34,567 where this gives
  1,234,567.

`android.icu` has both, at the price of a formatter the unit tests cannot reach.
The trade is written down in [fidelity.md](fidelity.md).
