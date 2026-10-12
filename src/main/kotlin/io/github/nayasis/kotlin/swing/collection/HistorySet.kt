package io.github.nayasis.kotlin.swing.collection

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * bounded history without duplicates, with a cursor to walk it back and forth.
 *
 * adding an element moves it to the end and puts the cursor on it. when full, the oldest element is dropped.
 * [prev] and [next] wrap around.
 */
class HistorySet<T>(
    @JsonIgnore
    private val capacity: Int = 128
) {

    private val set = ArrayList<T>(capacity)

    var cursor: Int? = null

    val size: Int
        get() = set.size

    fun contains(element: T): Boolean {
        return set.contains(element)
    }

    fun isEmpty(): Boolean {
        return set.isEmpty()
    }

    fun add(element: T) {
        set.remove(element)
        if(size >= capacity) {
            set.removeAt(0)
        }
        set.add(element)
        cursor = set.lastIndex
    }

    fun clear() {
        set.clear()
        cursor = null
    }

    fun iterator(): Iterator<T> {
        return set.iterator()
    }

    fun remove(element: T): Boolean {
        return set.indexOf(element).let { if(it >= 0) it else null }?.let { index ->
            cursor = when {
                cursor == null -> null
                size <= 1 -> null
                index <= cursor!! -> cursor!! - 1
                else -> cursor
            }
            set.removeAt(index)
            true
        } ?: false
    }

    operator fun get(index: Int): T? {
        if( isEmpty() ) return null
        var idx = index % size
        if( idx < 0 )
            idx += size
        cursor = idx
        return set.elementAt(idx)
    }

    fun next(): T? {
        if( isEmpty() ) return null
        cursor = cursor?.let { it + 1 } ?: 0
        return get(cursor!!)
    }

    fun prev(): T? {
        if( isEmpty() ) return null
        cursor = cursor?.let { it - 1 } ?: set.lastIndex
        return get(cursor!!)
    }

    fun toList(): List<T> {
        return set
    }

    override fun toString(): String {
        return """
            cursor : $cursor,
            set    : $set
        """.trimIndent()
    }

}
