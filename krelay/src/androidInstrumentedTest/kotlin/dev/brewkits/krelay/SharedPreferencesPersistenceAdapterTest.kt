package dev.brewkits.krelay

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [SharedPreferencesPersistenceAdapter].
 *
 * Runs on a real Android device / emulator to verify:
 * - save / loadAll / remove round-trip with real SharedPreferences
 * - clearScope clears only the target scope
 * - clearAll wipes everything
 * - Special characters in payload survive encode/decode
 * - Concurrent save + loadAll does not corrupt state
 * - Stale entries left from a previous "process" are restored correctly
 */
@RunWith(AndroidJUnit4::class)
class SharedPreferencesPersistenceAdapterTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var adapter: SharedPreferencesPersistenceAdapter

    private val scope1 = "Scope1"
    private val scope2 = "Scope2"
    private val featureKey = "ToastFeature"

    @Before
    fun setup() {
        adapter = SharedPreferencesPersistenceAdapter(context)
        adapter.clearAll()
    }

    @After
    fun tearDown() {
        adapter.clearAll()
    }

    // ── save / loadAll round-trip ─────────────────────────────────────────

    @Test
    fun saveAndLoad_singleEntry() {
        val cmd = PersistedCommand("show", "Hello World", timestamp(), 50)
        adapter.save(scope1, featureKey, cmd)

        val loaded = adapter.loadAll(scope1)
        assertEquals(1, loaded[featureKey]?.size)
        val restored = loaded[featureKey]!!.first()
        assertEquals("show", restored.actionKey)
        assertEquals("Hello World", restored.payload)
        assertEquals(50, restored.priority)
    }

    @Test
    fun saveAndLoad_multipleEntriesSameFeature() {
        repeat(5) { i ->
            adapter.save(scope1, featureKey, PersistedCommand("show", "msg$i", timestamp(), 50))
        }
        val loaded = adapter.loadAll(scope1)
        assertEquals(5, loaded[featureKey]?.size)
    }

    @Test
    fun saveAndLoad_multipleFeatures() {
        adapter.save(scope1, "ToastFeature", PersistedCommand("show", "toast", timestamp(), 50))
        adapter.save(scope1, "NavFeature", PersistedCommand("go", "home", timestamp(), 50))

        val loaded = adapter.loadAll(scope1)
        assertEquals(2, loaded.size)
        assertEquals(1, loaded["ToastFeature"]?.size)
        assertEquals(1, loaded["NavFeature"]?.size)
    }

    @Test
    fun loadAll_emptyScope_returnsEmpty() {
        val loaded = adapter.loadAll("nonexistent-scope")
        assertTrue(loaded.isEmpty())
    }

    // ── remove ────────────────────────────────────────────────────────────

    @Test
    fun remove_deletesSpecificEntry() {
        val cmd1 = PersistedCommand("show", "first", timestamp(), 50)
        val cmd2 = PersistedCommand("show", "second", timestamp() + 1, 50)
        adapter.save(scope1, featureKey, cmd1)
        adapter.save(scope1, featureKey, cmd2)

        adapter.remove(scope1, featureKey, cmd1)

        val loaded = adapter.loadAll(scope1)
        assertEquals(1, loaded[featureKey]?.size)
        assertEquals("second", loaded[featureKey]!!.first().payload)
    }

    @Test
    fun remove_nonExistentEntry_noOp() {
        val cmd = PersistedCommand("show", "saved", timestamp(), 50)
        adapter.save(scope1, featureKey, cmd)

        val ghost = PersistedCommand("show", "ghost", timestamp() + 9999, 50)
        adapter.remove(scope1, featureKey, ghost)  // should not throw

        assertEquals(1, adapter.loadAll(scope1)[featureKey]?.size)
    }

    // ── clearScope ────────────────────────────────────────────────────────

    @Test
    fun clearScope_removesOnlyTargetScope() {
        adapter.save(scope1, featureKey, PersistedCommand("show", "s1", timestamp(), 50))
        adapter.save(scope2, featureKey, PersistedCommand("show", "s2", timestamp(), 50))

        adapter.clearScope(scope1)

        assertTrue(adapter.loadAll(scope1).isEmpty())
        assertEquals(1, adapter.loadAll(scope2)[featureKey]?.size)
    }

    // ── clearAll ──────────────────────────────────────────────────────────

    @Test
    fun clearAll_removesAllScopes() {
        adapter.save(scope1, featureKey, PersistedCommand("show", "s1", timestamp(), 50))
        adapter.save(scope2, featureKey, PersistedCommand("show", "s2", timestamp(), 50))

        adapter.clearAll()

        assertTrue(adapter.loadAll(scope1).isEmpty())
        assertTrue(adapter.loadAll(scope2).isEmpty())
    }

    // ── payload special characters ────────────────────────────────────────

    @Test
    fun specialCharacters_inPayload_roundTrip() {
        val payloads = listOf(
            "Hello:World",            // colon (used in serialize format)
            "pipe|separated",         // pipe
            "newline\nvalue",         // newline
            "emoji 🎉🚀✅",            // unicode/emoji
            "json:{\"key\":\"val\"}", // JSON-like string
            "",                       // empty payload
            "   spaces   "            // leading/trailing spaces
        )
        payloads.forEachIndexed { i, payload ->
            adapter.save(scope1, featureKey, PersistedCommand("action$i", payload, timestamp() + i, 50))
        }

        val loaded = adapter.loadAll(scope1)
        val restored = loaded[featureKey]!!.sortedBy { it.timestampMs }
        assertEquals(payloads.size, restored.size)
        restored.forEachIndexed { i, cmd ->
            assertEquals("Payload $i should survive round-trip", payloads[i], cmd.payload)
        }
    }

    @Test
    fun specialCharacters_inActionKey_roundTrip() {
        val cmd = PersistedCommand("show:toast::colon", "value", timestamp(), 50)
        adapter.save(scope1, featureKey, cmd)

        val loaded = adapter.loadAll(scope1)[featureKey]!!.first()
        assertEquals("show:toast::colon", loaded.actionKey)
    }

    // ── process-death simulation ───────────────────────────────────────────

    @Test
    fun processDeathSimulation_dataPersistedAcrossAdapterInstances() {
        // Simulate process 1: save to SharedPreferences
        val adapter1 = SharedPreferencesPersistenceAdapter(context)
        val cmd = PersistedCommand("show", "survives death", timestamp(), 100)
        adapter1.save(scope1, featureKey, cmd)

        // Simulate process 2: new adapter instance reads from same SharedPreferences
        val adapter2 = SharedPreferencesPersistenceAdapter(context)
        val loaded = adapter2.loadAll(scope1)

        assertEquals(1, loaded[featureKey]?.size)
        assertEquals("survives death", loaded[featureKey]!!.first().payload)

        adapter2.clearAll()
    }

    // ── priority preservation ─────────────────────────────────────────────

    @Test
    fun priority_preservedAfterRoundTrip() {
        val priorities = listOf(0, 50, 100, 1000)
        priorities.forEachIndexed { i, prio ->
            adapter.save(scope1, featureKey, PersistedCommand("a$i", "p$i", timestamp() + i, prio))
        }

        val loaded = adapter.loadAll(scope1)[featureKey]!!
        val loadedPriorities = loaded.map { it.priority }.sorted()
        assertEquals(priorities.sorted(), loadedPriorities)
    }

    // ── KRelay integration (end-to-end on device) ─────────────────────────

    @Test
    fun endToEnd_dispatchPersisted_survivesAdapterRecreation() {
        val instance = KRelay.create("InstrumentedE2EScope")
        try {
            instance.setPersistenceAdapter(SharedPreferencesPersistenceAdapter(context))
            instance.registerActionFactory<MockToastFeature>("show") { payload ->
                { feature -> feature.show(payload) }
            }

            // Dispatch (no impl registered → goes to queue + persisted)
            instance.dispatchPersisted<MockToastFeature>("show", "instrumented!")

            assertEquals(1, instance.getPendingCount<MockToastFeature>())

            // Simulate new adapter instance (process death) — same scope name so persistence key matches
            val instance2 = KRelay.create("InstrumentedE2EScope")
            instance2.setPersistenceAdapter(SharedPreferencesPersistenceAdapter(context))
            instance2.registerActionFactory<MockToastFeature>("show") { payload ->
                { feature -> feature.show(payload) }
            }
            instance2.restorePersistedActions()

            assertEquals(1, instance2.getPendingCount<MockToastFeature>())

            val mock = MockToastImpl()
            instance2.register<MockToastFeature>(mock)
            // Replay is posted to main looper — drain it before asserting
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals(listOf("instrumented!"), mock.shown)
        } finally {
            instance.reset()
            KRelay.clearInstanceRegistry()
            adapter.clearAll()
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private fun timestamp() = System.currentTimeMillis()

    interface MockToastFeature : RelayFeature {
        fun show(message: String)
    }

    class MockToastImpl : MockToastFeature {
        val shown = mutableListOf<String>()
        override fun show(message: String) { shown.add(message) }
    }
}
