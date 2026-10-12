package io.github.nayasis.kotlin.swing.common

import com.formdev.flatlaf.FlatIntelliJLaf
import org.junit.jupiter.api.Assumptions.assumeFalse
import java.awt.GraphicsEnvironment

/** install the default theme (FlatLaf IntelliJ) for tests and demo apps. */
fun installTheme() {
    FlatIntelliJLaf.setup()
}

/** skip the calling test when no display is available. */
fun assumeDisplay() {
    assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display")
}
