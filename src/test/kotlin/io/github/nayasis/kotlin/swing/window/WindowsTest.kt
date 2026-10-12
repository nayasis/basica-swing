package io.github.nayasis.kotlin.swing.window

import io.github.nayasis.kotlin.swing.common.assumeDisplay
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.github.nayasis.kotlin.swing.property.InsetProperty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.awt.Rectangle
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.JFrame

class WindowsTest {

    private fun frame() = JFrame().apply {
        isUndecorated = true
        setBounds(100, 100, 400, 300)
    }

    @Test
    fun `zoom fills visible screen and restores previous bounds`() {
        assumeDisplay()
        runUiAndWait {
            val frame = frame().apply { isVisible = true }
            try {
                val expected = BoundaryChecker().getMajorScreen(frame).visibleBounds

                frame.setZoom(true)
                frame.isZoomed shouldBe true
                frame.bounds shouldBe expected

                frame.setZoom(false)
                frame.isZoomed shouldBe false
                frame.bounds shouldBe Rectangle(100, 100, 400, 300)
            } finally {
                frame.dispose()
            }
        }
    }

    @Test
    fun `maximized state is kept in extended state`() {
        assumeDisplay()
        runUiAndWait {
            val frame = frame().apply { isVisible = true }
            try {
                frame.isMaximized shouldBe false
                frame.isMaximized = true
                frame.isMaximized shouldBe true
                frame.isMaximized = false
                frame.isMaximized shouldBe false
            } finally {
                frame.dispose()
            }
        }
    }

    @Test
    fun `boundary is captured as inset`() {
        assumeDisplay()
        runUiAndWait {
            val frame = frame()
            try {
                MaximizedProperty().apply { capture(frame) }.boundary shouldBe InsetProperty(100, 100, 400, 300)
            } finally {
                frame.dispose()
            }
        }
    }

    @Test
    fun `runWhenShown runs once the window is shown`() {
        assumeDisplay()
        val shown = CountDownLatch(1)
        val frame = runUiAndWait { frame().apply { runWhenShown { shown.countDown() } } }
        try {
            shown.count shouldBe 1L
            runUiAndWait { frame.isVisible = true }
            shown.await(5, TimeUnit.SECONDS) shouldBe true
        } finally {
            runUiAndWait { frame.dispose() }
        }
    }

    @Test
    fun `window outside every screen is moved back by bind`() {
        assumeDisplay()
        runUiAndWait {
            val frame = frame()
            try {
                InsetProperty(-100_000, -100_000, 400, 300).bind(frame)
                BoundaryChecker().isShownOnScreen(frame) shouldBe true
            } finally {
                frame.dispose()
            }
        }
    }

}
