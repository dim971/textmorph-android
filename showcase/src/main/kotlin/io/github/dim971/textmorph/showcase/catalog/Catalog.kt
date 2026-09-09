package io.github.dim971.textmorph.showcase.catalog

import io.github.dim971.textmorph.showcase.demos.currencySwapDemo
import io.github.dim971.textmorph.showcase.demos.deltaDemo
import io.github.dim971.textmorph.showcase.demos.filtersDemo
import io.github.dim971.textmorph.showcase.demos.helloDemo
import io.github.dim971.textmorph.showcase.demos.hexColourDemo
import io.github.dim971.textmorph.showcase.demos.numberFieldDemo
import io.github.dim971.textmorph.showcase.demos.reflowDemo
import io.github.dim971.textmorph.showcase.demos.rewriteDemo
import io.github.dim971.textmorph.showcase.demos.spinDialDemo
import io.github.dim971.textmorph.showcase.demos.squeezeToAbbreviateDemo
import io.github.dim971.textmorph.showcase.demos.streamingDemo
import io.github.dim971.textmorph.showcase.demos.tickerDemo
import io.github.dim971.textmorph.showcase.demos.unitsDemo
import io.github.dim971.textmorph.showcase.demos.versionsDemo
import io.github.dim971.textmorph.showcase.demos.walletDemo

/**
 * The demos, in the order they are worth reading.
 *
 * Hand-ordered, the way upstream orders its own examples: each group opens with
 * the plainest use of what it covers. Text first, then numbers, then the things
 * that need a morph to be interrupted or constrained.
 */
val catalog: List<Demo> =
    listOf(
        helloDemo,
        rewriteDemo,
        streamingDemo,
        filtersDemo,
        hexColourDemo,
        versionsDemo,
        walletDemo,
        deltaDemo,
        unitsDemo,
        currencySwapDemo,
        squeezeToAbbreviateDemo,
        tickerDemo,
        numberFieldDemo,
        spinDialDemo,
        reflowDemo,
    )
