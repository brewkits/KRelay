package dev.brewkits.krelay

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.atomicfu.atomic
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Instrumented tests for main-thread dispatch behaviour on Android.
 *
 * Verifies:
 * - KRelay.dispatch() always executes the action on the Android main thread (Looper)
 * - Actions dispatched from background threads arrive on the main thread
 * - Queued (replay) actions also execute on main thread after registration
 * - Thread identity is preserved even with many concurrent dispatchers
 */
@RunWith(AndroidJUnit4::class)
class MainThreadDispatchInstrumentedTest {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    @Before
    fun setup() {
        KRelay.reset()
        KRelay.resetConfiguration()
    }

    @After
    fun tearDown() {
        KRelay.reset()
        KRelay.resetConfiguration()
    }

    // ── immediate dispatch ────────────────────────────────────────────────

    @Test
    fun immediateDispatch_executesOnMainThread() {
        val latch = CountDownLatch(1)
        val executedOnMain = atomic(false)

        val impl = object : ToastFeature {
            override fun show(message: String) {
                executedOnMain.value = (Looper.myLooper() == Looper.getMainLooper())
                latch.countDown()
            }
        }
        KRelay.register<ToastFeature>(impl)
        KRelay.dispatch<ToastFeature> { it.show("hello") }

        assertTrue("Action should execute within 2s", latch.await(2, TimeUnit.SECONDS))
        assertTrue("Action must execute on main thread", executedOnMain.value)
    }

    @Test
    fun immediateDispatch_fromBackgroundThread_executesOnMainThread() {
        val latch = CountDownLatch(1)
        val executedOnMain = atomic(false)

        val impl = object : ToastFeature {
            override fun show(message: String) {
                executedOnMain.value = (Looper.myLooper() == Looper.getMainLooper())
                latch.countDown()
            }
        }
        KRelay.register<ToastFeature>(impl)

        // Dispatch from background thread
        Thread {
            KRelay.dispatch<ToastFeature> { it.show("from bg") }
        }.also { it.start() }.join()

        assertTrue("Action should execute within 2s", latch.await(2, TimeUnit.SECONDS))
        assertTrue("Action must execute on main thread even when dispatched from bg", executedOnMain.value)
    }

    // ── queued (replay) dispatch ──────────────────────────────────────────

    @Test
    fun queuedReplay_executesOnMainThread() {
        val latch = CountDownLatch(3)
        val mainThreadCount = atomic(0)

        // Dispatch 3 actions before registering
        KRelay.dispatch<ToastFeature> { it.show("q1") }
        KRelay.dispatch<ToastFeature> { it.show("q2") }
        KRelay.dispatch<ToastFeature> { it.show("q3") }

        val impl = object : ToastFeature {
            override fun show(message: String) {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    mainThreadCount.incrementAndGet()
                }
                latch.countDown()
            }
        }
        KRelay.register<ToastFeature>(impl)

        assertTrue("All replays should complete within 3s", latch.await(3, TimeUnit.SECONDS))
        assertEquals("All 3 replays must execute on main thread", 3, mainThreadCount.value)
    }

    // ── concurrent background dispatchers ─────────────────────────────────

    @Test
    fun concurrentBackgroundDispatchers_allExecuteOnMainThread() {
        val total = 50
        val latch = CountDownLatch(total)
        val mainThreadCount = atomic(0)

        val impl = object : ToastFeature {
            override fun show(message: String) {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    mainThreadCount.incrementAndGet()
                }
                latch.countDown()
            }
        }
        KRelay.register<ToastFeature>(impl)

        // 10 background threads, each dispatching 5 times
        val threads = (0 until 10).map { t ->
            Thread {
                repeat(5) { i ->
                    KRelay.dispatch<ToastFeature> { it.show("t$t-i$i") }
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertTrue("All $total dispatches should complete within 5s", latch.await(5, TimeUnit.SECONDS))
        assertEquals("All dispatches must run on main thread", total, mainThreadCount.value)
    }

    // ── scope token dispatch ──────────────────────────────────────────────

    @Test
    fun scopeTokenDispatch_immediate_executesOnMainThread() {
        val latch = CountDownLatch(1)
        val executedOnMain = atomic(false)

        val impl = object : ToastFeature {
            override fun show(message: String) {
                executedOnMain.value = (Looper.myLooper() == Looper.getMainLooper())
                latch.countDown()
            }
        }
        KRelay.register<ToastFeature>(impl)

        val token = scopedToken()
        KRelay.dispatch<ToastFeature>(token) { it.show("scoped") }

        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertTrue("Scoped dispatch must execute on main thread", executedOnMain.value)
    }

    // ── priority dispatch ─────────────────────────────────────────────────

    @Test
    fun priorityDispatch_replay_executesOnMainThread() {
        val latch = CountDownLatch(2)
        val mainThreadCount = atomic(0)

        KRelay.dispatchWithPriority<ToastFeature>(ActionPriority.HIGH) { it.show("high") }
        KRelay.dispatchWithPriority<ToastFeature>(ActionPriority.CRITICAL) { it.show("critical") }

        val impl = object : ToastFeature {
            override fun show(message: String) {
                if (Looper.myLooper() == Looper.getMainLooper()) mainThreadCount.incrementAndGet()
                latch.countDown()
            }
        }
        KRelay.register<ToastFeature>(impl)

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        assertEquals("Both priority replays must run on main thread", 2, mainThreadCount.value)
    }
}
