package io.github.nayasis.kotlin.swing.app

import java.awt.Window
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.SwingUtilities

/**
 * a view: its [root] component plus the lifecycle hooks an application needs.
 *
 * ```
 * class Main: SwingView("application.title".message()) {
 *     override val root = JPanel(BorderLayout()).apply { add(JScrollPane(table), CENTER) }
 *     override fun onBeforeShow() { ... }
 * }
 * ```
 */
abstract class SwingView(val title: String? = null) {

    abstract val root: JComponent

    /** the window this view is currently shown in, if any. */
    val currentWindow: Window?
        get() = SwingUtilities.getWindowAncestor(root)

    /** the application's primary frame. */
    val primaryFrame: JFrame
        get() = SwingApp.primaryFrame

    /** called after the view is put into a frame, before the frame is shown. */
    open fun onBeforeShow() {}

    /** called after the view's frame is closed. */
    open fun onUndock() {}

}
