<div align="center">

# TextMorph for Jetpack Compose

**A native port of [Torph](https://torph.lochie.me) by [Lochie Axon](https://github.com/lochie).**

Text continuity for native interfaces. When a value changes, the characters,
words and digits that survive the change move to their new place instead of
disappearing in a cross-fade.

[![Licence](https://img.shields.io/badge/licence-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-blueviolet.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-24%2B-lightgrey.svg)](#requirements)

</div>

This repository is being built in the open, commit by commit. The public API is
a single composable, `TextMorph`, mirroring upstream's single component, plus
the two utilities upstream exports. Its twin is
[textmorph-ios](https://github.com/dim971/textmorph-ios): the two ports mirror
each other file for file and replay the same fixtures.

Nothing is released yet. See [CHANGELOG.md](CHANGELOG.md) for where it stands
and [CONTRIBUTING.md](CONTRIBUTING.md) for how to build it.

## Licence

MIT. See [LICENSE](LICENSE). This is a port of Torph, MIT, copyright 2025
Lochie Axon; see [NOTICE](NOTICE) for the attribution and for exactly which
files are derived from it.
