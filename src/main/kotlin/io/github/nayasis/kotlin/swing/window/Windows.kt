@file:Suppress("unused")

package io.github.nayasis.kotlin.swing.window

import io.github.nayasis.kotlin.basica.core.extension.FieldProperty
import io.github.nayasis.kotlin.swing.misc.runLater
import io.github.nayasis.kotlin.swing.property.InsetProperty
import java.awt.Frame
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.Serializable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

class Windows { companion object {

    val defaultIcons = IconContainer()

    val focusedWindow: Window?
        get() = windows.firstOrNull { it.isFocused }
            ?: java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow

    val windows: List<Window>
        get() = Window.getWindows().toList()

}}

fun Window.loadDefaultIcon(): Window = apply {
    if( ! Windows.defaultIcons.isEmpty() ) {
        setIconImages(Windows.defaultIcons.icons)
    }
}

fun Window.showAt(bounds: InsetProperty? = null) {
    bounds?.bind(this)
    isVisible = true
}

/**
 * hide the window while [block] runs, then show it again after [delay].
 */
fun <T> Window.hideWhile(
    delay: Duration = Duration.ZERO,
    block: () -> T,
): T {
    isVisible = false
    return try {
        block()
    } finally {
        runLater(delay) {
            isVisible = true
            toFront()
            requestFocus()
        }
    }
}

val Window.boundary: Rectangle
    get() = bounds

fun Window.addCloseRequest(listener: (WindowEvent) -> Unit) {
    addWindowListener(object: WindowAdapter() {
        override fun windowClosing(e: WindowEvent) = listener(e)
    })
}

/** maximized state of a [Frame]. other windows are never maximized. */
var Window.isMaximized: Boolean
    get() = (this as? Frame)?.let { (it.extendedState and Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH } ?: false
    set(value) {
        (this as? Frame)?.let {
            it.extendedState = when {
                value -> it.extendedState or Frame.MAXIMIZED_BOTH
                else  -> it.extendedState and Frame.MAXIMIZED_BOTH.inv()
            }
        }
    }

/**
 * run [block] on the event dispatch thread once the window is shown.
 */
fun Window.runWhenShown(block: () -> Unit) {
    if( isShowing ) {
        runLater(block)
        return
    }
    val done   = AtomicBoolean(false)
    val window = this
    val fire = {
        if( done.compareAndSet(false, true) ) runLater(block)
    }
    // componentShown and windowOpened both mark the first display. whichever comes first wins.
    window.addComponentListener(object: ComponentAdapter() {
        override fun componentShown(e: ComponentEvent) {
            window.removeComponentListener(this)
            fire()
        }
    })
    window.addWindowListener(object: WindowAdapter() {
        override fun windowOpened(e: WindowEvent) {
            window.removeWindowListener(this)
            fire()
        }
    })
}

// zoom : maximize that respects taskbar. undecorated frames would cover it when maximized by the OS.

private var Window.zoomed: Boolean by FieldProperty{ false }

var Window.previousZoomInset: InsetProperty? by FieldProperty{ null }

val Window.isZoomed: Boolean
    get() = zoomed

fun Window.setZoom(enable: Boolean) {
    zoomed = enable
    if( enable ) {
        previousZoomInset = InsetProperty(this)
        BoundaryChecker().getMajorScreen(this).visibleBounds.let {
            setBounds(it.x, it.y, it.width, it.height)
        }
    } else {
        previousZoomInset?.bind(this)
        previousZoomInset = null
    }
}

/**
 * Previous boundary before the window was maximized.
 *
 * It is activated by [Window.watchMaximized]
 */
var Window.previousBoundary: MaximizedProperty by FieldProperty{ MaximizedProperty() }

internal var Window.maximizedWatcherInstalled: Boolean by FieldProperty{ false }

/**
 * Manage previous window boundary (x,y,width,height) when maximized state is changed.
 *
 * Previous boundary is [Window.previousBoundary]
 */
fun Window.watchMaximized() {
    if( maximizedWatcherInstalled ) return
    maximizedWatcherInstalled = true

    val window = this
    previousBoundary.capture(window)

    var captureToken = 0L
    val scheduleCapture = {
        val expected = ++captureToken
        runLater {
            if( expected == captureToken && !window.isMaximized && window.isNormalBoundsCandidate() ) {
                window.previousBoundary.capture(window)
            }
        }
    }

    addWindowStateListener {
        val maximized = window.isMaximized
        window.previousBoundary.maximized = maximized
        if( !maximized ) scheduleCapture()
    }
    addComponentListener(object: ComponentAdapter() {
        override fun componentMoved(e: ComponentEvent)   { scheduleCapture() }
        override fun componentResized(e: ComponentEvent) { scheduleCapture() }
    })
}

internal fun Window.fillsCurrentScreen(inset: InsetProperty = InsetProperty(this), tolerance: Int = 16): Boolean {
    val screen = BoundaryChecker().getMajorScreen(this).visibleBounds
    return inset.width >= screen.width - tolerance &&
        inset.height >= screen.height - tolerance &&
        inset.x <= screen.x + tolerance &&
        inset.y <= screen.y + tolerance
}

private fun Window.isNormalBoundsCandidate(): Boolean = !fillsCurrentScreen()

class MaximizedProperty: Serializable {
    var maximized = false
    var boundary = InsetProperty()
    fun capture(window: Window) {
        boundary.apply {
            x = window.x
            y = window.y
            window.width. takeIf { it > 0 }?.let { width  = it }
            window.height.takeIf { it > 0 }?.let { height = it }
        }
    }
}
