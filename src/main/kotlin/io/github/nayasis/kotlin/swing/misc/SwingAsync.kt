package io.github.nayasis.kotlin.swing.misc

import io.github.nayasis.kotlin.basica.etc.error
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.EventQueue
import java.awt.Toolkit
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

private val job = SupervisorJob()

/**
 * scope of every task started by this library (and by [launchAsync] / [launchUi]).
 *
 * background work runs on [Dispatchers.IO], so blocking calls are fine. its threads are daemon
 * threads: they do not keep the JVM alive, call [shutdownAsync] to give running tasks time to finish.
 */
val asyncScope = CoroutineScope(job + Dispatchers.IO)

/**
 * run [func] on a background thread.
 *
 * a failure inside [func] is not propagated to the caller. it is handed to the default
 * uncaught exception handler on the event dispatch thread, so the caller keeps running and
 * the handler decides how to surface it. [Deferred.await] still rethrows it.
 *
 * @return the running task, to observe or cancel it
 */
fun <T> runAsync(func: () -> T): Deferred<T> {
    val task = asyncScope.async { func() }
    task.invokeOnCompletion { e ->
        if (e != null && e !is CancellationException) runLater { reportUncaught(e) }
    }
    return task
}

/**
 * run [func] on a background thread and wait for termination.
 *
 * when called from the event dispatch thread, a secondary event loop keeps the UI responsive
 * while waiting, so this halts further execution without blocking repaint. failures are routed
 * as described in [runAsync].
 */
fun runAwait(func: () -> Unit) {

    val task = runAsync(func)

    if(!EventQueue.isDispatchThread()) {
        runBlocking { task.join() }
        return
    }

    val loop = Toolkit.getDefaultToolkit().systemEventQueue.createSecondaryLoop()
    // the exit is queued behind the current event, so it can only run once the loop below is
    // entered, even when [func] finishes immediately. exit() before enter() would hang the loop.
    task.invokeOnCompletion { runLater { loop.exit() } }
    loop.enter()
}

/** start a coroutine on a background thread. [block] may suspend and call [withUi]. */
fun launchAsync(context: CoroutineContext = Dispatchers.IO, block: suspend CoroutineScope.() -> Unit): Job =
    asyncScope.launch(context, block = block)

/** start a coroutine on the event dispatch thread. */
fun launchUi(block: suspend CoroutineScope.() -> Unit): Job =
    asyncScope.launch(Dispatchers.Swing, block = block)

/** switch to the event dispatch thread for [func], then resume. */
suspend fun <T> withUi(func: () -> T): T = withContext(Dispatchers.Swing) { func() }

/** switch to a background thread for [func], then resume. */
suspend fun <T> withBackground(func: () -> T): T = withContext(Dispatchers.IO) { func() }

/**
 * run [func] on the event dispatch thread and wait for the result.
 *
 * runs directly when already on the dispatch thread.
 */
fun <T> runUiAndWait(func: () -> T): T {
    if(EventQueue.isDispatchThread()) return func()
    val task = FutureTask { func() }
    EventQueue.invokeLater(task)
    try {
        return task.get()
    } catch (e: ExecutionException) {
        throw e.cause ?: e
    }
}

/**
 * wait up to [timeoutMillis] for running tasks to finish, then cancel the rest.
 * the scope stays usable afterwards.
 */
fun shutdownAsync(timeoutMillis: Long = 1_000) = runBlocking {
    val finished = withTimeoutOrNull(timeoutMillis) { job.children.toList().joinAll() }
    if(finished == null) {
        logger.warn { "async tasks did not finish in ${timeoutMillis}ms, cancelling" }
        job.cancelChildren()
    }
}

private fun reportUncaught(e: Throwable) {
    val handler = Thread.getDefaultUncaughtExceptionHandler()
    if(handler == null) {
        logger.error(e)
        return
    }
    handler.uncaughtException(Thread.currentThread(), e)
}
