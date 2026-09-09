package io.github.dim971.textmorph.showcase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * The one activity.
 *
 * It reads two intent extras, and they exist so the screenshots in the README
 * and under docs/ can be taken by a script rather than by hand: `screen` opens
 * a tab and `demo` opens one demo by its catalogue id. Nothing in the app's own
 * navigation uses them, and neither is a public interface.
 *
 *   adb shell am start -n <package>/.ShowcaseActivity --es demo wallet
 */
class ShowcaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val screen = intent?.getStringExtra("screen")
        val demo = intent?.getStringExtra("demo")
        setContent { ShowcaseApp(startScreen = screen, startDemo = demo) }
    }
}
