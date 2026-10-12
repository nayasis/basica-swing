package io.github.nayasis.kotlin.swing.window

import io.github.nayasis.kotlin.swing.common.installTheme
import kotlin.system.exitProcess

/** manual demo. asserts nothing. */
object DialogDemo {
    @JvmStatic fun main(args: Array<String>) {
        installTheme()

        Dialog.alert("information", "detail text of the information\nsecond line")
        println("confirm : ${Dialog.confirm("are you sure?")}")
        println("prompt  : ${Dialog.prompt("your name?", "nayasis")}")
        Dialog.error("something wrong", IllegalStateException("boom"))
        println("files   : ${Dialog.filePicker(title = "pick files", extension = "png,jpg", description = "images", mode = FilePickerMode.Multi)}")
        println("dir     : ${Dialog.dirPicker(title = "pick directory")}")

        exitProcess(0)
    }
}
