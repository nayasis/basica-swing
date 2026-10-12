package io.github.nayasis.kotlin.swing.app

import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.basica.exception.rootCause
import io.github.nayasis.kotlin.swing.app.di.SimpleDiContainer
import io.github.nayasis.kotlin.swing.misc.runLater
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.github.nayasis.kotlin.swing.misc.shutdownAsync
import io.github.nayasis.kotlin.swing.preloader.DefaultPreloader
import io.github.nayasis.kotlin.swing.window.Dialog
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import java.awt.EventQueue
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JFrame
import kotlin.reflect.KClass
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

/**
 * application base: sets up the look and feel, DI container, environment and logging,
 * then puts [primaryView] into the primary frame.
 *
 * ```
 * class MyApp: SwingApp(Main::class) {
 *     override fun setupLookAndFeel() = FlatIntelliJLaf.setup().let {}
 * }
 *
 * fun main(args: Array<String>) = SwingApp.launch(MyApp::class, *args)
 * ```
 */
@Suppress("unused")
abstract class SwingApp(
    private val primaryView: KClass<out SwingView>? = null,
) {

    companion object {

        val ctx = SimpleDiContainer()

        lateinit var environment: Environment
            private set

        lateinit var primaryFrame: JFrame
            private set

        private val views = ConcurrentHashMap<KClass<out SwingView>, SwingView>()

        /**
         * get a view, creating and caching it on first use.
         *
         * views are not DI beans: they are instantiated through their no-arg
         * constructor and held as singletons, so they need no annotation.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T: SwingView> find(type: KClass<T>): T = views.getOrPut(type) {
            type.java.getDeclaredConstructor().newInstance()
        } as T

        /**
         * run the application: splash (if set), init, then start on the event dispatch thread.
         * call it from `main`.
         */
        fun launch(app: KClass<out SwingApp>, vararg args: String) {
            val instance = app.java.getDeclaredConstructor().newInstance()
            instance.setupDefaultExceptionHandler()
            instance.setupLookAndFeel()
            DefaultPreloader.show()
            val argv = arrayOf(*args)
            try {
                instance.init(argv)
                runUiAndWait { instance.start(argv) }
                DefaultPreloader.close()
            } catch (e: Throwable) {
                instance.fail(e)
            }
        }

    }

    /** set the look and feel (e.g. `FlatIntelliJLaf.setup()`). runs once, before any component is created. */
    open fun setupLookAndFeel() {}

    open fun setupDefaultExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            if (EventQueue.isDispatchThread()) {
                runCatching {
                    runLater { Dialog.error(e.message, e.rootCause) }
                }.onFailure { logger.error(it) }
            } else {
                logger.error(e)
            }
        }
    }

    private fun init(args: Array<String>) {
        environment = Environment(args, "application.yml")
        ctx.set(environment)
        LoggerConfig(environment).initialize()
        beforeInitialization(ctx, environment)
        ctx.scanPackages(this::class.java.packageName)
        afterInitialization(ctx, environment)
    }

    private fun start(args: Array<String>) {
        val frame = JFrame().apply { defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE }
        primaryFrame = frame
        onStart(DefaultParser().parse(setOptions() ?: Options(), args))
        val view = primaryView?.let { find(it) }
        frame.addWindowListener(object: WindowAdapter() {
            override fun windowClosed(e: WindowEvent) {
                view?.let { runCatching { it.onUndock() }.onFailure { t -> logger.error(t) } }
                stop()
            }
        })
        if (view != null) {
            frame.contentPane = view.root
            view.title?.let { frame.title = it }
        }
        frame.pack()
        frame.setLocationRelativeTo(null)
        onStart(frame)
        view?.onBeforeShow()
        frame.isVisible = true
    }

    private fun stop() {
        runCatching { onStop() }.onFailure { logger.error(it) }
        runCatching { shutdownAsync() }.onFailure { logger.error(it) }
        runCatching { ctx.close() }.onFailure { logger.error(it) }
        exitProcess(0)
    }

    private fun fail(e: Throwable) {
        logger.error(e)
        DefaultPreloader.close()
        runCatching { runUiAndWait { Dialog.error(e.message, e.rootCause, true) } }.onFailure { logger.error(it) }
        exitProcess(1)
    }

    open fun beforeInitialization(context: SimpleDiContainer, environment: Environment) {}
    open fun afterInitialization(context: SimpleDiContainer, environment: Environment) {}

    open fun onStart(command: CommandLine) {}
    open fun onStart(frame: JFrame) {}
    open fun setOptions(): Options? { return null }
    open fun onStop() {}

}
