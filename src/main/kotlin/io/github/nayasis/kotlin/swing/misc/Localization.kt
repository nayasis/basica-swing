package io.github.nayasis.kotlin.swing.misc

import java.awt.Window
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * texts read from message codes, applied again when the language changes.
 *
 * ```
 * val menuFile = JMenu().localize { text = "menu.file".message() }
 *
 * // later, on a language switch
 * Messages.defaultLocale = Locale.JAPANESE
 * Localization.refresh()      // menuFile.text is now the japanese one
 * ```
 *
 * targets are held weakly, so a component that is dropped stops being updated. the block gets the target as
 * its receiver: reach the target through `this`, not through an outer reference, or it is never released.
 * all calls belong on the event dispatch thread.
 */
object Localization {

    private class Binding(val target: WeakReference<Any>, val block: (Any) -> Unit)

    private val bindings  = ArrayList<Binding>()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    /** apply [block] to [target] now and again on every [refresh]. */
    fun <T: Any> bind(target: T, block: T.() -> Unit): T {
        bindings.removeAll { it.target.get() == null }
        @Suppress("UNCHECKED_CAST")
        bindings.add(Binding(WeakReference<Any>(target), block as (Any) -> Unit))
        target.block()
        return target
    }

    /**
     * call [listener] on every [refresh], for texts that do not belong to one component property:
     * a status line, a tooltip put together from several parts. close the returned handle to stop.
     */
    fun onRefresh(listener: () -> Unit): AutoCloseable {
        listeners += listener
        return AutoCloseable { listeners -= listener }
    }

    /** apply every bound text again, after the message locale has changed. */
    fun refresh() {
        bindings.removeAll { it.target.get() == null }
        bindings.toList().forEach { binding -> binding.target.get()?.let(binding.block) }
        listeners.forEach { it() }
        // texts change length: lay the windows out again
        Window.getWindows().forEach { it.validate(); it.repaint() }
    }

}

/** apply [block] now and again on every [Localization.refresh]. */
fun <T: Any> T.localize(block: T.() -> Unit): T = Localization.bind(this, block)
