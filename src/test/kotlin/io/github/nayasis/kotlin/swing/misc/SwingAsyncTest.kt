package io.github.nayasis.kotlin.swing.misc

import io.github.nayasis.kotlin.swing.common.assumeDisplay
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.time.Duration.Companion.milliseconds

internal class SwingAsyncTest {

    @Test
    fun `runAsync runs the block on a background thread`() {
        val caller = Thread.currentThread()
        val worker = AtomicReference<Thread?>()
        val latch = CountDownLatch(1)

        runAsync {
            worker.set(Thread.currentThread())
            latch.countDown()
        }

        latch.await(5, TimeUnit.SECONDS) shouldBe true
        (worker.get() === caller) shouldBe false
    }

    @Test
    fun `runAwait runs the block on another thread and waits for it`() {
        val caller = Thread.currentThread()
        val worker = AtomicReference<Thread?>()
        val done = AtomicBoolean(false)

        runAwait {
            worker.set(Thread.currentThread())
            Thread.sleep(50)
            done.set(true)
        }

        done.get() shouldBe true
        (worker.get() === caller) shouldBe false
    }

    @Test
    fun `runAwait hands a failure to the default handler instead of the caller`() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        val caught = AtomicReference<Throwable?>()
        val reported = CountDownLatch(1)
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            caught.set(e)
            reported.countDown()
        }
        try {
            var reachedNextStatement = false

            runAwait { throw IllegalStateException("boom") }
            reachedNextStatement = true

            reachedNextStatement shouldBe true
            reported.await(5, TimeUnit.SECONDS) shouldBe true
            caught.get()?.message shouldBe "boom"
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }
    }

    @Test
    fun `runAwait on the dispatch thread keeps the event queue running`() {
        assumeDisplay()
        val processed = AtomicBoolean(false)
        val result = AtomicBoolean(false)

        SwingUtilities.invokeAndWait {
            SwingUtilities.invokeLater { processed.set(true) }
            runAwait { Thread.sleep(200) }
            result.set(processed.get())
        }

        result.get() shouldBe true
    }

    @Test
    fun `runLater runs the block on the dispatch thread`() {
        assumeDisplay()
        val onUiThread = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        runLater {
            onUiThread.set(isUiThread)
            latch.countDown()
        }

        latch.await(5, TimeUnit.SECONDS) shouldBe true
        onUiThread.get() shouldBe true
    }

    @Test
    fun `runLater with delay can be cancelled`() {
        assumeDisplay()
        val fired = AtomicBoolean(false)

        runLater(100.milliseconds) { fired.set(true) }.stop()
        Thread.sleep(300)

        fired.get() shouldBe false
    }

    @Test
    fun `runUiAndWait returns the value computed on the dispatch thread`() {
        assumeDisplay()
        runUiAndWait { isUiThread } shouldBe true
    }

    @Test
    fun `runAsync result can be awaited`() {
        runBlocking { runAsync { 1 + 2 }.await() } shouldBe 3
    }

    @Test
    fun `launchAsync can switch to the dispatch thread and back with withUi`() {
        assumeDisplay()
        val onUi = AtomicBoolean(false)
        val back = AtomicReference<Boolean>()

        runBlocking {
            launchAsync {
                withUi { onUi.set(isUiThread) }
                back.set(isUiThread)
            }.join()
        }

        onUi.get() shouldBe true
        back.get() shouldBe false
    }

    @Test
    fun `launchUi runs on the dispatch thread`() {
        assumeDisplay()
        val onUi = AtomicBoolean(false)

        runBlocking { launchUi { onUi.set(isUiThread) }.join() }

        onUi.get() shouldBe true
    }

    @Test
    fun `shutdownAsync cancels tasks that outlive the timeout`() {
        val task = runAsync { Thread.sleep(10_000) }

        shutdownAsync(100)

        task.isCancelled shouldBe true
    }

}
