package io.github.nayasis.kotlin.swing.preloader

import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

object PreloaderRecorder {
    val events = CopyOnWriteArrayList<String>()
}

class RecordingPreloader: DefaultPreloader() {
    override fun onShow() { PreloaderRecorder.events.add("show") }
    override fun onProgress(notificator: ProgressNotificator) {
        PreloaderRecorder.events.add("progress:${notificator.progress}:${notificator.message}")
    }
    override fun onClose() { PreloaderRecorder.events.add("close") }
}

class PreloaderTest {

    @Test
    fun `progress is the ratio of index to max`() {
        ProgressNotificator(5, 10).progress shouldBe 0.5
        ProgressNotificator(11, 10).progress shouldBe 1.0
        ProgressNotificator(-1, 10).progress shouldBe null
        ProgressNotificator(1, Double.NaN).progress shouldBe null
        ProgressNotificator(1, Double.POSITIVE_INFINITY).progress shouldBe null
    }

    @Test
    fun `notifications reach the preloader in order`() {
        PreloaderRecorder.events.clear()
        DefaultPreloader.set(RecordingPreloader::class)

        DefaultPreloader.show()
        DefaultPreloader.notifyMessage("hello")
        DefaultPreloader.notifyProgress(0.5, "half")
        DefaultPreloader.close()
        // notifications are queued on the event dispatch thread. an empty call flushes them.
        runUiAndWait { }

        PreloaderRecorder.events.toList() shouldBe listOf("show", "progress:null:hello", "progress:0.5:half", "close")
    }

    @Test
    fun `notifications without a shown preloader are ignored`() {
        PreloaderRecorder.events.clear()
        DefaultPreloader.notifyMessage("lost")
        runUiAndWait { }
        PreloaderRecorder.events.isEmpty() shouldBe true
    }

}
