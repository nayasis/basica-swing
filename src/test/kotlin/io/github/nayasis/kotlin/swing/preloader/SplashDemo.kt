package io.github.nayasis.kotlin.swing.preloader

import io.github.nayasis.kotlin.swing.common.installTheme
import kotlin.system.exitProcess

class DemoSplash: DefaultSplash(500, 300, "/test.png")

/** manual demo. asserts nothing. */
object SplashDemo {
    @JvmStatic fun main(args: Array<String>) {
        installTheme()
        DefaultPreloader.set(DemoSplash::class)
        DefaultPreloader.show()
        for( i in 1..10 ) {
            DefaultPreloader.notifyProgress(i, 10, "loading $i / 10")
            Thread.sleep(300)
        }
        DefaultPreloader.close()
        Thread.sleep(500)
        exitProcess(0)
    }
}
