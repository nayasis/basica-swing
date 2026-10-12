package io.github.nayasis.kotlin.swing.collection

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class HistorySetTest {

    @Test
    fun `adding moves a duplicate to the end and drops the oldest when full`() {
        val history = HistorySet<String>(3)
        listOf("a", "b", "c", "a", "d").forEach { history.add(it) }

        history.toList() shouldBe listOf("c", "a", "d")
        history.cursor shouldBe 2
    }

    @Test
    fun `prev and next wrap around`() {
        val history = HistorySet<String>(5)
        listOf("a", "b", "c").forEach { history.add(it) }

        history.next() shouldBe "a"
        history.prev() shouldBe "c"
        history.prev() shouldBe "b"
    }

    @Test
    fun `removing before the cursor keeps it on the same element`() {
        val history = HistorySet<String>(5)
        listOf("a", "b", "c").forEach { history.add(it) }

        history.remove("a") shouldBe true
        history[history.cursor!!] shouldBe "c"
    }

}
