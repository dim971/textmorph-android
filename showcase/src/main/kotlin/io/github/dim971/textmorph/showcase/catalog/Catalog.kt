package io.github.dim971.textmorph.showcase.catalog

import io.github.dim971.textmorph.showcase.demos.actionDemo
import io.github.dim971.textmorph.showcase.demos.amountDemo
import io.github.dim971.textmorph.showcase.demos.bubbleSliderDemo
import io.github.dim971.textmorph.showcase.demos.chartDemo
import io.github.dim971.textmorph.showcase.demos.copyDemo
import io.github.dim971.textmorph.showcase.demos.currencySwapDemo
import io.github.dim971.textmorph.showcase.demos.deltaDemo
import io.github.dim971.textmorph.showcase.demos.dimensionsDemo
import io.github.dim971.textmorph.showcase.demos.downloadDemo
import io.github.dim971.textmorph.showcase.demos.earnedDemo
import io.github.dim971.textmorph.showcase.demos.filtersDemo
import io.github.dim971.textmorph.showcase.demos.helloDemo
import io.github.dim971.textmorph.showcase.demos.hexColourDemo
import io.github.dim971.textmorph.showcase.demos.holdToConfirmDemo
import io.github.dim971.textmorph.showcase.demos.installDemo
import io.github.dim971.textmorph.showcase.demos.numoraFieldDemo
import io.github.dim971.textmorph.showcase.demos.pullToCountDemo
import io.github.dim971.textmorph.showcase.demos.rangeShoveDemo
import io.github.dim971.textmorph.showcase.demos.ratingSliderDemo
import io.github.dim971.textmorph.showcase.demos.reflowDemo
import io.github.dim971.textmorph.showcase.demos.reorderListDemo
import io.github.dim971.textmorph.showcase.demos.resizeDemo
import io.github.dim971.textmorph.showcase.demos.resultsSummaryDemo
import io.github.dim971.textmorph.showcase.demos.rewriteDemo
import io.github.dim971.textmorph.showcase.demos.sloshGaugeDemo
import io.github.dim971.textmorph.showcase.demos.spinDialDemo
import io.github.dim971.textmorph.showcase.demos.splitBarDemo
import io.github.dim971.textmorph.showcase.demos.squeezeToAbbreviateDemo
import io.github.dim971.textmorph.showcase.demos.squishyNumberDemo
import io.github.dim971.textmorph.showcase.demos.streamingDemo
import io.github.dim971.textmorph.showcase.demos.tickerDemo
import io.github.dim971.textmorph.showcase.demos.trailingTagDemo
import io.github.dim971.textmorph.showcase.demos.unitsDemo
import io.github.dim971.textmorph.showcase.demos.versionsDemo
import io.github.dim971.textmorph.showcase.demos.walletDemo

/**
 * The demos, in upstream's order.
 *
 * Card for card with torph's own examples page, and in its sequence, so the two
 * can be read side by side. Upstream hand-orders them, opening each group with
 * the plainest use of what it covers: text, then numbers, then the cards that
 * need a morph to be interrupted, constrained or driven by a gesture.
 *
 * The values, the intervals and the eases are upstream's exactly, and so is the
 * physics on the two cards where the physics is the demo: the bubble sliders
 * hang their pills on upstream's own spring and pivot them apart with
 * upstream's own collision test, constants included. See `Bubbles.kt`.
 *
 * Everywhere else the interface around the morph is written for this platform
 * rather than reproduced. Upstream's elastic squish, its taffy stretch and its
 * slot-machine reels are not here, because none of them is about TextMorph and
 * reimplementing all of them would put thousands of lines of physics in a
 * catalogue whose job is to show one library. Where a card differs, its own
 * file says so.
 *
 * The last two are ours. Upstream shows neither the first render, which never
 * animates, nor a value emptying out, and both are worth a screen.
 */
val catalog: List<Demo> =
    listOf(
        installDemo,
        bubbleSliderDemo,
        rangeShoveDemo,
        spinDialDemo,
        numoraFieldDemo,
        streamingDemo,
        copyDemo,
        hexColourDemo,
        walletDemo,
        deltaDemo,
        earnedDemo,
        filtersDemo,
        versionsDemo,
        holdToConfirmDemo,
        unitsDemo,
        currencySwapDemo,
        actionDemo,
        dimensionsDemo,
        resultsSummaryDemo,
        rewriteDemo,
        tickerDemo,
        chartDemo,
        downloadDemo,
        reorderListDemo,
        pullToCountDemo,
        ratingSliderDemo,
        splitBarDemo,
        resizeDemo,
        squishyNumberDemo,
        squeezeToAbbreviateDemo,
        sloshGaugeDemo,
        amountDemo,
        trailingTagDemo,
        helloDemo,
        reflowDemo,
    )
