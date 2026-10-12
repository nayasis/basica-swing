@file:Suppress("MemberVisibilityCanBePrivate")

package io.github.nayasis.kotlin.swing.window.progress

import io.github.nayasis.kotlin.swing.misc.runAsync
import io.github.nayasis.kotlin.swing.misc.runAwait
import io.github.nayasis.kotlin.swing.misc.runLater
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.github.nayasis.kotlin.swing.property.PositionProperty
import java.awt.Dialog.ModalityType
import java.awt.Window

@Suppress("unused")
abstract class AbstractProgressDialog(val size: Int, title: String?) {

    private var titleText = title
    private var owner: Window? = null
    private var modality = ModalityType.MODELESS

    @Volatile
    private var created: ProgressDialogWindow? = null

    /**
     * the dialog window. it is created on first use, so [initOwner] and [initModality] must come first.
     */
    val window: ProgressDialogWindow
        get() = created ?: runUiAndWait {
            created ?: ProgressDialogWindow(size, owner).also {
                it.modalityType = modality
                it.updateTitle(titleText)
                created = it
            }
        }

    protected var onSuccess: (() -> Unit)? = null
    protected var onFail: ((exception: Throwable) -> Unit)? = { e -> throw e }
    protected var onDone: (() -> Unit)? = null

    fun initModality(modality: ModalityType) {
        check(created == null) { "initModality should be called before the dialog is used" }
        this.modality = modality
    }

    fun initOwner(window: Window?) {
        check(created == null) { "initOwner should be called before the dialog is used" }
        owner = window
    }

    fun updateTitle(title: String?) {
        titleText = title
        created?.updateTitle(title)
    }

    /** show the dialog without blocking, even when it is modal. */
    fun show() = runLater { window.isVisible = true }

    /**
     * Show dialog at specified point.
     *
     * @param point position to apply before showing. If null, shows at default position.
     */
    fun showAt(point: PositionProperty? = null) {
        bind(point)
        show()
    }

    fun close() = runLater {
        created?.apply {
            isVisible = false
            dispose()
        }
    }

    private fun bind(point: PositionProperty?) {
        if( point == null ) return
        runUiAndWait { point.bind(window) }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T: AbstractProgressDialog> internalRunSync(point: PositionProperty? = null, task: ((dialog: T) -> Unit)?) {
        showAt(point)
        if(task == null) return
        val self = this as T
        runAwait { runTask(task,self) }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T: AbstractProgressDialog> internalRunAsync(point: PositionProperty? = null, task: ((dialog: T) -> Unit)?) {
        showAt(point)
        if(task == null) return
        val self = this as T
        runAsync {
            runTask(task,self)
        }
    }

    private fun <T: AbstractProgressDialog> runTask(task: ((dialog: T) -> Unit)?, dialog: T) {
        try {
            task?.invoke(dialog)
            close()
            onSuccess?.let {
                runLater { it.invoke() }
            }
        } catch (e: Throwable) {
            close()
            onFail?.let {
                runLater { it.invoke(e) }
            }
        } finally {
            onDone?.let {
                runLater { it.invoke() }
            }
        }
    }

}
