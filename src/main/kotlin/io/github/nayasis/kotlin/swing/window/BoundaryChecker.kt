package io.github.nayasis.kotlin.swing.window

import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import kotlin.math.min

/** screen bounds without taskbar, dock and other reserved areas. */
val GraphicsConfiguration.visibleBounds: Rectangle
    get() {
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(this)
        val b = bounds
        return Rectangle(
            b.x + insets.left,
            b.y + insets.top,
            b.width  - (insets.left + insets.right),
            b.height - (insets.top + insets.bottom),
        )
    }

class BoundaryChecker {

    private val screens: List<GraphicsConfiguration>
        get() = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.map { it.defaultConfiguration }

    private val primary: GraphicsConfiguration
        get() = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration

    /**
     * move window onto the primary screen when it is not displayed on any screen.
     */
    fun reset(window: Window) {
        if( isShownOnScreen(window) ) return
        primary.visibleBounds.let {
            window.setBounds(it.x, it.y, min(window.width, it.width), min(window.height, it.height))
        }
    }

    fun isShownOnScreen(window: Window): Boolean =
        isShownOnScreen(window.bounds)

    fun isShownOnScreen(rect: Rectangle): Boolean =
        screens.any { it.bounds.intersects(rect) }

    /**
     * get screen which has maximum area of the window.
     *
     * if there is no screen related to the window, return primary screen.
     */
    fun getMajorScreen(window: Window): GraphicsConfiguration =
        getMajorScreen(window.bounds)

    /**
     * get screen which has maximum area of the rectangle.
     *
     * if there is no screen related to the rectangle, return primary screen.
     */
    fun getMajorScreen(rect: Rectangle): GraphicsConfiguration {
        var major = primary
        var max   = 0L
        for( screen in screens ) {
            val area = screen.bounds.getIntersectedArea(rect)
            if( area > max ) {
                major = screen
                max   = area
            }
        }
        return major
    }

}

fun Rectangle.getIntersectedArea(r: Rectangle): Long {
    val intersection = this.intersection(r)
    return if( intersection.isEmpty ) 0L else intersection.width.toLong() * intersection.height
}
