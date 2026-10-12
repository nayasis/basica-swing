package io.github.nayasis.kotlin.swing.control.combobox

import java.io.Serializable

data class ItemCombo(
    val value: String?,
    var label: String?,
    var ref: Any? = null
): Serializable {

    constructor(value: String): this(value,value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ItemCombo
        return value == other.value
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }

}
