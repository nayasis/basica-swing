package io.github.nayasis.kotlin.swing.app

import io.kotest.matchers.shouldBe
import javax.swing.JPanel
import kotlin.test.Test

private class TestView: SwingView("test title") {
    override val root = JPanel()
}

internal class SwingViewTest {

    @Test
    fun `title is kept`() {
        TestView().title shouldBe "test title"
    }

    @Test
    fun `currentWindow is null while the view is not in a window`() {
        TestView().currentWindow shouldBe null
    }

    @Test
    fun `find caches one instance per view type`() {
        val a = SwingApp.find(TestView::class)
        val b = SwingApp.find(TestView::class)
        (a === b) shouldBe true
    }

}
