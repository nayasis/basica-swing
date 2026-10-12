package io.github.nayasis.kotlin.swing.window.progress

import io.github.nayasis.kotlin.swing.common.assumeDisplay
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ProgressDialogTest {

    @Test
    fun `async task reports progress and completes`() {
        assumeDisplay()
        val done = CountDownLatch(1)
        val dialog = ProgressDialog("test").setOnSuccess { done.countDown() }

        dialog.runAsync { it.updateProgress(1, 2) }

        done.await(5, TimeUnit.SECONDS) shouldBe true
        dialog.getProgress() shouldBe 0.5
    }

    @Test
    fun `sync task finishes before runSync returns`() {
        assumeDisplay()
        val dialog = ProgressDialog("test")

        dialog.runSync { it.updateProgress(10, 10) }

        dialog.getProgress() shouldBe 1.0
    }

    @Test
    fun `failure is handed to onFail`() {
        assumeDisplay()
        val done   = CountDownLatch(1)
        val failed = AtomicReference<Throwable>()
        val dialog = ProgressDialog("test").setOnFail {
            failed.set(it)
            done.countDown()
        }

        dialog.runAsync { throw IllegalStateException("boom") }

        done.await(5, TimeUnit.SECONDS) shouldBe true
        failed.get().message shouldBe "boom"
    }

    @Test
    fun `invalid progress values are treated as zero`() {
        assumeDisplay()
        val done = CountDownLatch(1)
        val dialog = MultiProgressDialog(2, "test").setOnDone { done.countDown() }

        dialog.runAsync {
            it.updateProgress(0, Double.NaN, 10)
            it.updateProgress(1, 3, 4)
        }

        done.await(5, TimeUnit.SECONDS) shouldBe true
        dialog.getProgress(0) shouldBe 0.0
        dialog.getProgress(1) shouldBe 0.75
    }

}
