package io.github.nayasis.kotlin.swing.property

import io.github.nayasis.kotlin.swing.misc.Desktop
import io.github.nayasis.kotlin.swing.window.BoundaryChecker
import java.awt.GraphicsConfiguration
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window
import java.io.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class InsetProperty(
    var x: Int      = 100,
    var y: Int      = 100,
    var width: Int  = 500,
    var height: Int = 600,
): Serializable {

    constructor(window: Window): this(
        x      = window.x,
        y      = window.y,
        width  = window.width,
        height = window.height,
    )

    constructor(boundary: Rectangle): this (
        x      = boundary.x,
        y      = boundary.y,
        width  = boundary.width,
        height = boundary.height,
    )

    constructor(configuration: GraphicsConfiguration): this() {
        val insets = Desktop.toolkit.getScreenInsets(configuration)
        val bounds = configuration.bounds
        x      = bounds.x + insets.left
        y      = bounds.y + insets.top
        width  = bounds.width  - (insets.left + insets.right)
        height = bounds.height - (insets.top + insets.bottom)
    }

    fun read(window: Window?) {
        window?.also {
            x      = it.x
            y      = it.y
            width  = it.width
            height = it.height
        }
    }

    fun bind(window: Window?) {
        window?.also {
            it.setBounds(x, y, width, height)
            BoundaryChecker().reset(it)
        }
    }

    operator fun contains(p: Point?): Boolean {
        return if(p == null) false else
            x <= p.x && p.x <= (x+width) &&
            y <= p.y && p.y <= (y+height)
    }

    operator fun contains(r: InsetProperty?): Boolean {
        return if (r == null) false else
            r.x >= x && r.y >= y && (r.x+r.width) <= (x+width) && (r.y+r.height) <= (y+height)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun intersects(r: InsetProperty?): Boolean {
        return if (r == null) false else (r.x+r.width) > x && (r.y+r.height) > y && r.x < (x+width) && r.y < (y+height)
    }

    fun toRectangle(): Rectangle =
        Rectangle(x, y, width, height)

    fun getIntersectedArea(r: InsetProperty): Int {
        return when {
            contains(r) -> r.width * r.height
            intersects(r) -> {
                val minX = max(x, r.x)
                val maxX = min(x+width,r.x + r.width)
                val minY = max(y, r.y)
                val maxY = min(y + height,r.y + r.height)
                abs(minX - maxX) * abs(minY - maxY)
            }
            else -> 0
        }
    }

}
