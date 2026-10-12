package io.github.nayasis.kotlin.swing.property

import java.awt.Window
import java.io.Serializable

data class PositionProperty(
    var x: Int = 0,
    var y: Int = 0,
): Serializable {

    constructor(window: Window?): this() { window?.let { read(it) } }

    fun read(window: Window?) {
        window?.let {
            x = it.x
            y = it.y
        }
    }

    fun bind(window: Window?) {
        window?.setLocation(x, y)
    }

}
