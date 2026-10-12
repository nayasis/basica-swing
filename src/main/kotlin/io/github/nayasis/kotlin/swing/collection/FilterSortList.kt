package io.github.nayasis.kotlin.swing.collection

import java.util.concurrent.CopyOnWriteArrayList

/**
 * list with a predicate filter and a comparator sort.
 *
 * [items] is the full backing list; iteration, [size], [view] and [get] see the filtered and sorted result.
 * every mutation (also through [items]) recomputes the view and notifies the listeners registered with
 * [addChangeListener]. call [refresh] after mutating an element in place.
 *
 * not thread safe: use it on the Swing event dispatch thread.
 *
 * ```
 * val links = FilterSortList<Link>()
 * links.predicate = { it.title.contains(keyword) }
 * ```
 */
class FilterSortList<T>(initial: Collection<T> = emptyList()): Iterable<T> {

    private val backing   = ArrayList<T>(initial)
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    /** the full list. mutations through it refresh the view. */
    val items: MutableList<T> = object: AbstractMutableList<T>() {
        override val size: Int get() = backing.size
        override fun get(index: Int): T = backing[index]
        override fun set(index: Int, element: T): T = backing.set(index, element).also { refresh() }
        override fun add(index: Int, element: T) { backing.add(index, element); refresh() }
        override fun removeAt(index: Int): T = backing.removeAt(index).also { refresh() }
        override fun addAll(elements: Collection<T>): Boolean = backing.addAll(elements).also { if (it) refresh() }
        override fun addAll(index: Int, elements: Collection<T>): Boolean = backing.addAll(index, elements).also { if (it) refresh() }
        override fun removeAll(elements: Collection<T>): Boolean = backing.removeAll(elements.toHashSet()).also { if (it) refresh() }
        override fun retainAll(elements: Collection<T>): Boolean = backing.retainAll(elements.toHashSet()).also { if (it) refresh() }
        override fun clear() { if (backing.isNotEmpty()) { backing.clear(); refresh() } }
    }

    /** the filtered and sorted snapshot. replaced on every refresh. */
    var view: List<T> = emptyList()
        private set

    /** which items are visible. applies to items added later as well. */
    var predicate: (T) -> Boolean = { true }
        set(value) {
            field = value
            refresh()
        }

    /** sort order of the view. null keeps the insertion order. */
    var comparator: Comparator<T>? = null
        set(value) {
            field = value
            refresh()
        }

    /** number of visible items. use `items.size` for the total. */
    val size: Int
        get() = view.size

    init {
        refresh()
    }

    operator fun get(index: Int): T = view[index]

    /** index in the view, or -1 when the item is hidden or absent. */
    fun indexOf(item: T): Int = view.indexOf(item)

    fun add(item: T): Boolean = items.add(item)
    fun addAll(list: Collection<T>): Boolean = items.addAll(list)
    fun remove(item: T): Boolean = items.remove(item)
    fun clear() = items.clear()

    /** replace the element at [index] of the full list. */
    operator fun set(index: Int, item: T): T = items.set(index, item)

    /** change the full list in one step: the view is recomputed once, after [block]. */
    fun update(block: MutableList<T>.() -> Unit) {
        backing.block()
        refresh()
    }

    /** recompute the view and notify listeners. */
    fun refresh() {
        val p = predicate
        val c = comparator
        val filtered = backing.filter(p)
        view = if (c == null) filtered else filtered.sortedWith(c)
        listeners.forEach { it() }
    }

    /** register a listener called after every refresh. close the returned handle to unregister. */
    fun addChangeListener(listener: () -> Unit): AutoCloseable {
        listeners.add(listener)
        return AutoCloseable { listeners.remove(listener) }
    }

    override fun iterator(): Iterator<T> = view.iterator()

}
