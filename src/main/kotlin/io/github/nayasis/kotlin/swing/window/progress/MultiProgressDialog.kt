package io.github.nayasis.kotlin.swing.window.progress

import io.github.nayasis.kotlin.swing.property.PositionProperty
import java.awt.Component

class MultiProgressDialog(progressCount: Int,title: String? = null): AbstractProgressDialog(progressCount,title) {

    fun updateProgress(index: Int, done: Number, max: Number) = window.updateProgress(index,done,max)
    fun updateProgress(index: Int, percent: Number) = window.updateProgress(index,percent)
    fun updateMessage(index: Int, message: String?) = window.updateMessage(index,message)
    fun updateSubMessage(index: Int, message: String?) = window.updateSubMessage(index,message)
    fun updateSubMessageAsProgress(index: Int, format: String = "%.1f%%") = window.updateSubMessageAsProgress(index,format)
    fun getProgress(index: Int): Double = window.getProgress(index)
    fun addHeaderRight(component: Component): MultiProgressDialog {
        window.addHeaderRight(component)
        return this
    }

    fun runSync(task: ((dialog: MultiProgressDialog) -> Unit)?) {
        super.internalRunSync(null, task)
    }

    fun runSync(point: PositionProperty?, task: ((dialog: MultiProgressDialog) -> Unit)?) {
        super.internalRunSync(point, task)
    }

    fun runAsync(task: ((dialog: MultiProgressDialog) -> Unit)?): MultiProgressDialog {
        super.internalRunAsync(null, task)
        return this
    }

    fun runAsync(point: PositionProperty?, task: ((dialog: MultiProgressDialog) -> Unit)?): MultiProgressDialog {
        super.internalRunAsync(point, task)
        return this
    }

    fun run(async: Boolean, task: ((dialog: MultiProgressDialog) -> Unit)?) {
        if(async) {
            super.internalRunAsync(null, task)
        } else {
            super.internalRunSync(null, task)
        }
    }

    fun run(async: Boolean, point: PositionProperty?, task: ((dialog: MultiProgressDialog) -> Unit)?) {
        if(async) {
            super.internalRunAsync(point, task)
        } else {
            super.internalRunSync(point, task)
        }
    }

    fun setOnSuccess(callback: (() -> Unit)): MultiProgressDialog {
        super.onSuccess = callback
        return this
    }

    fun setOnFail(callback: ((exception: Throwable) -> Unit)): MultiProgressDialog {
        super.onFail = callback
        return this
    }

    fun setOnDone(callback: (() -> Unit)): MultiProgressDialog {
        super.onDone = callback
        return this
    }

}
