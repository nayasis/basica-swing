package io.github.nayasis.kotlin.swing.misc

import java.awt.EventQueue
import javax.swing.Timer
import kotlin.time.Duration

/** whether the caller is on the Swing event dispatch thread. */
val isUiThread: Boolean
    get() = EventQueue.isDispatchThread()

/**
 * run [func] on the Swing event dispatch thread.
 *
 * [func] is always queued, even when already on the dispatch thread, so the caller never re-enters.
 */
fun runLater(func: () -> Unit) = EventQueue.invokeLater(func)

/**
 * run [func] on the Swing event dispatch thread after [delay].
 *
 * can be called from any thread. call [Timer.stop] on the returned timer to abort the pending execution.
 *
 * @return the started one-shot timer
 */
fun runLater(delay: Duration, func: () -> Unit): Timer {
    val millis = delay.inWholeMilliseconds.toInt().coerceAtLeast(0)
    return Timer(millis) { func() }.apply {
        isRepeats = false
        start()
    }
}
