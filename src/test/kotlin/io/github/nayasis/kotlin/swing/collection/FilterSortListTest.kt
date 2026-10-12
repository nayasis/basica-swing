package io.github.nayasis.kotlin.swing.collection

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class FilterSortListTest {

    @Test
    fun `predicate hides items but keeps the backing list intact`() {
        val list = FilterSortList(listOf(1, 2, 3, 4))

        list.size shouldBe 4

        list.predicate = { it % 2 == 0 }

        list.size shouldBe 2          // visible count
        list.items.size shouldBe 4    // total count
        list.toList() shouldBe listOf(2, 4)
    }

    @Test
    fun `mutations go through to the backing list`() {
        val list = FilterSortList<Int>()

        list.add(1)
        list.addAll(listOf(2, 3))
        list.remove(2)

        list.items.toList() shouldBe listOf(1, 3)

        list.clear()
        list.items.size shouldBe 0
    }

    @Test
    fun `items added after the predicate is set are filtered too`() {
        val list = FilterSortList<Int>()
        list.predicate = { it % 2 == 0 }

        list.addAll(listOf(1, 2, 3, 4))

        list.toList() shouldBe listOf(2, 4)
        list.items.size shouldBe 4
    }

    @Test
    fun `comparator sorts the view and leaves the backing order`() {
        val list = FilterSortList(listOf(3, 1, 2))

        list.comparator = Comparator.naturalOrder()

        list.view shouldBe listOf(1, 2, 3)
        list.items.toList() shouldBe listOf(3, 1, 2)

        list.comparator = null

        list.view shouldBe listOf(3, 1, 2)
    }

    @Test
    fun `mutating through items refreshes the view`() {
        val list = FilterSortList<Int>()
        list.predicate = { it > 1 }

        list.items.add(1)
        list.items.add(5)
        list.items.addAll(listOf(7, 0))

        list.view shouldBe listOf(5, 7)

        list.items.removeAt(1)

        list.view shouldBe listOf(7)
    }

    @Test
    fun `listeners are notified once per change and can be removed`() {
        val list = FilterSortList<Int>()
        var count = 0
        val handle = list.addChangeListener { count++ }

        list.add(1)
        list.addAll(listOf(2, 3))
        list.predicate = { it > 1 }
        list.update { add(4); add(5) }

        count shouldBe 4

        handle.close()
        list.add(6)

        count shouldBe 4
    }

    @Test
    fun `refresh picks up an in place change`() {
        class Box(var n: Int)
        val a = Box(1)
        val b = Box(2)
        val list = FilterSortList(listOf(a, b))
        list.comparator = compareBy<Box> { it.n }

        list.view shouldBe listOf(a, b)

        a.n = 3
        list.refresh()

        list.view shouldBe listOf(b, a)
    }

}
