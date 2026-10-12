package io.github.nayasis.kotlin.swing.window.progress

import io.github.nayasis.kotlin.swing.common.installTheme
import io.github.nayasis.kotlin.swing.window.Dialog
import javax.swing.JButton
import kotlin.system.exitProcess

/** manual demo. asserts nothing. */
object ProgressDialogDemo {
    @JvmStatic fun main(args: Array<String>) {
        installTheme()

        var cancel = false
        val cancelButton = JButton("cancel").apply { addActionListener { cancel = true } }

        Dialog.progress("single progress", async = false, headerButton = cancelButton) { dialog ->
            for( i in 1..100 ) {
                if( cancel ) break
                dialog.updateProgress(i, 100)
                dialog.updateMessage("working $i")
                dialog.updateSubMessageAsProgress()
                Thread.sleep(30)
            }
        }

        Dialog.progressMulti(2, "multi progress", async = false) { dialog ->
            for( i in 1..100 ) {
                dialog.updateProgress(0, i, 100)
                dialog.updateProgress(1, i / 2, 100)
                dialog.updateMessage(0, "first $i")
                dialog.updateMessage(1, "second ${i / 2}")
                dialog.updateSubMessageAsProgress(0)
                dialog.updateSubMessageAsProgress(1)
                Thread.sleep(30)
            }
        }

        exitProcess(0)
    }
}
