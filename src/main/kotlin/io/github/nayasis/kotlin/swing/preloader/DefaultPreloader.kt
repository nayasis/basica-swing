package io.github.nayasis.kotlin.swing.preloader

import io.github.nayasis.kotlin.swing.misc.runLater
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.github.nayasis.kotlin.swing.window.Dialog
import kotlin.reflect.KClass

/**
 * something shown while the application starts, such as a splash window.
 *
 * ```
 * class Splash: DefaultSplash(500,300,"/view/splash/splash.jpg")
 *
 * fun main(args: Array<String>) {
 *   DefaultPreloader.set(Splash::class)
 *   SwingApp.launch(MyApp::class, args)
 * }
 * ```
 *
 * a subclass needs a no-arg constructor. every callback runs on the event dispatch thread.
 */
abstract class DefaultPreloader {

    /** create and show the preloader window. */
    abstract fun onShow()

    abstract fun onProgress(notificator: ProgressNotificator)

    /** release the preloader window. */
    open fun onClose() {}

    open fun onHide() {}

    open fun isShowing(): Boolean = false

    open fun onError(message: String?, throwable: Throwable?) {
        onHide()
        Dialog.error(message, throwable, true)
        onClose()
    }

    companion object {

        private var preloaderType: KClass<out DefaultPreloader>? = null

        @Volatile
        private var current: DefaultPreloader? = null

        fun isShowing(): Boolean {
            return current?.isShowing() ?: false
        }

        /** register the preloader to show. it is shown by [show]. */
        fun set(preloader: KClass<out DefaultPreloader>) {
            preloaderType = preloader
        }

        /** show the registered preloader and wait until it is shown. nothing happens if none is registered. */
        fun show() {
            val type = preloaderType ?: return
            runUiAndWait {
                current = type.java.getDeclaredConstructor().apply { isAccessible = true }.newInstance().also { it.onShow() }
            }
        }

        private fun notify(notificator: Notificator) {
            runLater { current?.handle(notificator) }
        }

        fun notifyMessage(message: String) {
            notify(ProgressNotificator(null, message))
        }

        fun notifyProgress(percent: Number, message: String? = null) {
            notify(ProgressNotificator(percent.toDouble(), message))
        }

        fun notifyProgress(index: Number, max: Number, message: String? = null) {
            notify(ProgressNotificator(index,max,message))
        }

        fun notifyError(message: String? = null, throwable: Throwable? = null) {
            notify(ErrorNotificator(message,throwable))
        }

        fun close() = notify(CloseNotificator())

        private fun DefaultPreloader.handle(notificator: Notificator) {
            when(notificator) {
                is ProgressNotificator -> onProgress(notificator)
                is ErrorNotificator -> {
                    onError(notificator.message, notificator.throwable)
                    current = null
                }
                is CloseNotificator -> {
                    onClose()
                    current = null
                }
                is HideNotificator -> onHide()
            }
        }

    }

}
