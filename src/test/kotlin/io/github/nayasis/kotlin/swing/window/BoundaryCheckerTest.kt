package io.github.nayasis.kotlin.swing.window

import io.github.nayasis.kotlin.swing.common.assumeDisplay
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.awt.Rectangle

class BoundaryCheckerTest {

    @Test
    fun `intersected area of rectangles`() {
        Rectangle(0, 0, 100, 100).getIntersectedArea(Rectangle(50, 50, 100, 100)) shouldBe 2500L
        Rectangle(0, 0, 100, 100).getIntersectedArea(Rectangle(10, 10, 20, 20)) shouldBe 400L
        Rectangle(0, 0, 100, 100).getIntersectedArea(Rectangle(200, 200, 10, 10)) shouldBe 0L
    }

    @Test
    fun `rectangle far from every screen is not shown`() {
        assumeDisplay()
        BoundaryChecker().isShownOnScreen(Rectangle(-100_000, -100_000, 100, 100)) shouldBe false
    }

    @Test
    fun `primary screen is the major screen of a rectangle on no screen`() {
        assumeDisplay()
        val checker = BoundaryChecker()
        val screen  = checker.getMajorScreen(Rectangle(-100_000, -100_000, 100, 100))
        (screen.bounds.width > 0) shouldBe true
        (screen.visibleBounds.width <= screen.bounds.width) shouldBe true
    }

}
