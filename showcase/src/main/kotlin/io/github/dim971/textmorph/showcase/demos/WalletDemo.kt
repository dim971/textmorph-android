package io.github.dim971.textmorph.showcase.demos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.textmorph.compose.TextMorph
import io.github.dim971.textmorph.showcase.catalog.Demo
import io.github.dim971.textmorph.showcase.shared.Chip
import io.github.dim971.textmorph.showcase.shared.LocalShowcaseSettings
import io.github.dim971.textmorph.showcase.shared.Stage
import io.github.dim971.textmorph.showcase.shared.rememberTicker
import io.github.dim971.textmorph.showcase.shared.showcaseColour
import io.github.dim971.textmorph.showcase.shared.stageFont

private val WALLET =
    listOf(
        "Connect wallet",
        "Connecting\u2026",
        "0xd55a\u2026d2685",
        "lochie.eth",
    )

/** A connect button that becomes an address and then a name. */
@Composable
fun WalletDemo() {
    val settings = LocalShowcaseSettings.current
    val index = rememberTicker(WALLET.size, 1800)

    Stage {
        Chip {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // An avatar appears once there is an account to put one on,
                // which is what makes the pill grow from the left as well as
                // the right. Upstream loads a photograph; a disc is enough to
                // show the pill absorbing it.
                AnimatedVisibility(
                    visible = index >= 2,
                    enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.5f),
                    exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.5f),
                ) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
                TextMorph(
                    text = WALLET[index],
                    modifier = Modifier.padding(start = if (index >= 2) 8.dp else 0.dp),
                    options = settings.options,
                    font = stageFont(size = 18.sp),
                    colour = showcaseColour(),
                )
            }
        }
    }
}

val walletDemo =
    Demo(
        id = "wallet",
        name = "Wallet",
        summary =
            "Connect wallet, then an ellipsis while it connects, then a truncated address, " +
                "then a name. Four states with almost nothing in common, so most of it is a group " +
                "replacement, and the pill's width carries the whole way.",
        capability = "a chain of unrelated values, and the pill that holds them",
        code =
            """
            Chip { TextMorph(text = state) }
            """.trimIndent(),
        content = { WalletDemo() },
    )
