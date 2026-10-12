package io.github.nayasis.kotlin.swing.control.table

import io.github.nayasis.kotlin.swing.collection.FilterSortList
import java.io.Serializable
import javax.swing.table.AbstractTableModel
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

/** one sort criterion: the column [columnId] ascending or descending. */
data class ColumnSortKey(
    val columnId: String = "",
    val ascending: Boolean = true,
): Serializable

/**
 * table model over a [FilterSortList]: rows are the visible items, columns are declared with [column].
 *
 * ```
 * val model = ListTableModel(list)
 *     .column("Name", Person::name)
 *     .column("Age", Person::age, editable = true)
 * table.bind(model)
 * ```
 *
 * sort through [sortKeys], not by setting `FilterSortList.comparator` directly.
 */
class ListTableModel<S>(val source: FilterSortList<S>): AbstractTableModel() {

    class Column<S>(
        val id: String,
        val title: String,
        val type: Class<*>,
        val editable: Boolean,
        val comparator: Comparator<Any?>?,
        val getter: (S) -> Any?,
        val setter: ((S, Any?) -> Unit)?,
    ) {
        /** whether header clicks can sort by this column. */
        val sortable: Boolean
            get() = comparator != null || Comparable::class.java.isAssignableFrom(type)
    }

    private val _columns = ArrayList<Column<S>>()
    val columns: List<Column<S>>
        get() = _columns

    /** the view as it was before the latest change event; lets a [refreshWrapper] map the old selection to objects. */
    private var snapshot: List<S> = source.view

    /** wraps the table refresh that follows a source change, e.g. to keep the selection. */
    var refreshWrapper: ((refresh: () -> Unit) -> Unit)? = null

    var sortKeys: List<ColumnSortKey> = emptyList()
        set(value) {
            field = value.filter { key -> _columns.any { it.id == key.columnId } }
            source.comparator = buildComparator(field)
        }

    init {
        source.addChangeListener {
            val fire = { fireTableDataChanged() }
            refreshWrapper?.invoke(fire) ?: fire()
            snapshot = source.view
        }
    }

    fun <T: Any> column(
        title: String,
        id: String = title,
        type: Class<T>,
        editable: Boolean = false,
        comparator: Comparator<T>? = null,
        getter: (S) -> T?,
        setter: ((S, T?) -> Unit)? = null,
    ): ListTableModel<S> {
        require(_columns.none { it.id == id }) { "duplicated column id: $id" }
        @Suppress("UNCHECKED_CAST")
        _columns += Column(
            id         = id,
            title      = title,
            type       = type.kotlin.javaObjectType,
            editable   = editable,
            comparator = comparator as Comparator<Any?>?,
            getter     = getter,
            setter     = setter as ((S, Any?) -> Unit)?,
        )
        fireTableStructureChanged()
        return this
    }

    inline fun <reified T: Any> column(
        title: String,
        id: String = title,
        editable: Boolean = false,
        comparator: Comparator<T>? = null,
        noinline getter: (S) -> T?,
        noinline setter: ((S, T?) -> Unit)? = null,
    ): ListTableModel<S> = column(title, id, T::class.java, editable, comparator, getter, setter)

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> column(
        title: String,
        prop: KProperty1<S, T?>,
        id: String = title,
        editable: Boolean = false,
        comparator: Comparator<T>? = null,
    ): ListTableModel<S> {
        val type   = (prop.returnType.classifier as? KClass<*>)?.javaObjectType ?: Any::class.java
        val mutable = prop as? KMutableProperty1<S, T?>
        return column(
            title      = title,
            id         = id,
            type       = type as Class<T>,
            editable   = editable,
            comparator = comparator,
            getter     = { prop.get(it) },
            setter     = mutable?.let { p -> { item: S, value: T? -> p.set(item, value) } },
        )
    }

    /** row object at the table's view index. */
    fun getRowObject(viewRow: Int): S = source.view[viewRow]

    /** row object as it was before the latest change event. */
    fun previousRowObject(viewRow: Int): S? = snapshot.getOrNull(viewRow)

    /** view index of [item], or -1. */
    fun indexOf(item: S?): Int = if (item == null) -1 else source.indexOf(item)

    override fun getRowCount(): Int = source.size

    override fun getColumnCount(): Int = _columns.size

    override fun getColumnName(column: Int): String = _columns[column].title

    override fun getColumnClass(columnIndex: Int): Class<*> = _columns[columnIndex].type

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean =
        _columns[columnIndex].let { it.editable && it.setter != null }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? =
        source.view.getOrNull(rowIndex)?.let { _columns[columnIndex].getter(it) }

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        val item = source.view.getOrNull(rowIndex) ?: return
        _columns[columnIndex].setter?.invoke(item, aValue)
        source.refresh()
    }

    private fun buildComparator(keys: List<ColumnSortKey>): Comparator<S>? {
        return keys.mapNotNull { key ->
            _columns.firstOrNull { it.id == key.columnId }?.let { columnComparator(it, key.ascending) }
        }.reduceOrNull { first, next -> first.thenComparing(next) }
    }

    // nulls stay last in both directions
    private fun columnComparator(column: Column<S>, ascending: Boolean): Comparator<S> = Comparator { a, b ->
        val x = column.getter(a)
        val y = column.getter(b)
        when {
            x == null && y == null -> 0
            x == null -> 1
            y == null -> -1
            else -> {
                val result = compareValues(column, x, y)
                if (ascending) result else -result
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun compareValues(column: Column<S>, x: Any, y: Any): Int = when {
        column.comparator != null -> column.comparator.compare(x, y)
        x is Comparable<*> && x.javaClass.isInstance(y) -> (x as Comparable<Any>).compareTo(y)
        else -> x.toString().compareTo(y.toString())
    }

}
