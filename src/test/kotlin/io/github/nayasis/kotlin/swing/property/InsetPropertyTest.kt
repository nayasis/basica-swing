package io.github.nayasis.kotlin.swing.property

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.awt.Point

class InsetPropertyTest {

    private val base = InsetProperty(0, 0, 100, 100)

    @Test
    fun `contains point`() {
        (Point(10, 10) in base) shouldBe true
        (Point(101, 10) in base) shouldBe false
        base.contains(null as Point?) shouldBe false
    }

    @Test
    fun `contains inset only when fully inside`() {
        (InsetProperty(10, 10, 20, 20) in base) shouldBe true
        (InsetProperty(50, 50, 100, 100) in base) shouldBe false
    }

    @Test
    fun intersects() {
        base.intersects(InsetProperty(50, 50, 100, 100)) shouldBe true
        base.intersects(InsetProperty(100, 0, 10, 10)) shouldBe false
        base.intersects(InsetProperty(200, 200, 10, 10)) shouldBe false
    }

    @Test
    fun getIntersectedArea() {
        base.getIntersectedArea(InsetProperty(10, 10, 20, 20)) shouldBe 400
        base.getIntersectedArea(InsetProperty(50, 50, 100, 100)) shouldBe 2500
        base.getIntersectedArea(InsetProperty(200, 200, 10, 10)) shouldBe 0
    }

    @Test
    fun `toRectangle keeps bounds`() {
        val rect = InsetProperty(1, 2, 3, 4).toRectangle()
        listOf(rect.x, rect.y, rect.width, rect.height) shouldBe listOf(1, 2, 3, 4)
    }

}
