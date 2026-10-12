package io.github.nayasis.kotlin.swing.property

import java.awt.Component
import java.awt.Dimension
import java.awt.Window
import java.io.Serializable

data class SizeProperty(
    var width: Int = 400,
    var height: Int = 300,
): Serializable {

    constructor(window: Window?): this() { window?.let { read(it) } }
    constructor(component: Component?): this() { component?.let { read(it) } }

    fun read(window: Window?) {
        window?.let {
            width  = it.width
            height = it.height
        }
    }

    fun read(component: Component?) {
        component?.let {
            width  = it.width
            height = it.height
        }
    }

    fun bind(window: Window?) {
        window?.setSize(width, height)
    }

    fun bind(component: Component?) {
        component?.preferredSize = Dimension(width, height)
    }

}
