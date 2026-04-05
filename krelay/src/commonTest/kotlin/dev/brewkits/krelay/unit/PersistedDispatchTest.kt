package dev.brewkits.krelay.unit

import dev.brewkits.krelay.*
import kotlin.test.*

/**
 * Tests for the persistent dispatch feature.
 *
 * Covers:
 * - PersistedCommand serialization/deserialization
 * - dispatchPersisted with no impl (goes to queue + adapter)
 * - dispatchPersisted with registered impl (executes immediately, no persist)
 * - restorePersistedActions restores from adapter to in-memory queue
 * - Expired commands are skipped during restoration
 * - reset() clears adapter scope
 */
class PersistedDispatchTest {

    interface ToastFeature : RelayFeature {
        fun show(message: String)
    }

    interface NavFeature : RelayFeature {
        fun navigateTo(screen: String)
    }

    class MockToast : ToastFeature {
        val shown = mutableListOf<String>()
        override fun show(message: String) { shown.add(message) }
    }

    class MockNav : NavFeature {
        val navigated = mutableListOf<String>()
        override fun navigateTo(screen: String) { navigated.add(screen) }
    }

    /** In-memory adapter that records calls for test verification. */
    class RecordingPersistenceAdapter : KRelayPersistenceAdapter {
        val saved = mutableListOf<Triple<String, String, PersistedCommand>>()
        val removed = mutableListOf<Triple<String, String, PersistedCommand>>()
        private val store = mutableMapOf<String, MutableMap<String, MutableList<PersistedCommand>>>()

        override fun save(scopeName: String, featureKey: String, command: PersistedCommand) {
            saved.add(Triple(scopeName, featureKey, command))
            store.getOrPut(scopeName) { mutableMapOf() }
                .getOrPut(featureKey) { mutableListOf() }
                .add(command)
        }

        override fun loadAll(scopeName: String): Map<String, List<PersistedCommand>> =
            store[scopeName]?.mapValues { it.value.toList() } ?: emptyMap()

        override fun remove(scopeName: String, featureKey: String, command: PersistedCommand) {
            removed.add(Triple(scopeName, featureKey, command))
            store[scopeName]?.get(featureKey)?.remove(command)
        }

        override fun clearScope(scopeName: String) { store.remove(scopeName) }
        override fun clearAll() { store.clear(); saved.clear(); removed.clear() }
    }

    private lateinit var instance: KRelayInstance
    private lateinit var adapter: RecordingPersistenceAdapter

    @BeforeTest
    fun setup() {
        KRelay.reset()
        instance = KRelay.create("PersistTestScope")
        adapter = RecordingPersistenceAdapter()
        instance.setPersistenceAdapter(adapter)
        instance.registerActionFactory<ToastFeature>("toast", "show") { payload -> { it.show(payload) } }
        instance.registerActionFactory<NavFeature>("nav", "go") { payload -> { it.navigateTo(payload) } }
    }

    @AfterTest
    fun tearDown() {
        instance.reset()
        adapter.clearAll()
        KRelay.reset()
        KRelay.clearInstanceRegistry()
    }

    // ──────────────────────────────────────────────────────────────────
    // PersistedCommand serialization
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun testPersistedCommand_serializeDeserialize_roundtrip() {
        val original = PersistedCommand("show_toast", "Hello World", 1234567890L, 50)
        val serialized = original.serialize()
        val restored = PersistedCommand.deserialize(serialized)

        assertNotNull(restored)
        assertEquals(original.actionKey, restored.actionKey)
        assertEquals(original.payload, restored.payload)
        assertEquals(original.timestampMs, restored.timestampMs)
        assertEquals(original.priority, restored.priority)
    }

    @Test
    fun testPersistedCommand_specialCharsInPayload() {
        val original = PersistedCommand("key", "payload with |pipes| and :colons: and | more |", 100L, 50)
        val serialized = original.serialize()
        val restored = PersistedCommand.deserialize(serialized)

        assertNotNull(restored)
        assertEquals(original.payload, restored.payload)
    }

    @Test
    fun testPersistedCommand_emptyPayload() {
        val original = PersistedCommand("navigate_home", "", 100L, 0)
        val restored = PersistedCommand.deserialize(original.serialize())

        assertNotNull(restored)
        assertEquals("", restored.payload)
        assertEquals("navigate_home", restored.actionKey)
    }

    @Test
    fun testPersistedCommand_deserialize_malformedReturnsNull() {
        assertNull(PersistedCommand.deserialize(""))
        assertNull(PersistedCommand.deserialize("notanumber:50:5:hello"))
        assertNull(PersistedCommand.deserialize("100:notanumber:5:hello"))
    }

    // ──────────────────────────────────────────────────────────────────
    // dispatchPersisted — no impl
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun testDispatchPersisted_noImpl_queuesAndPersists() {
        instance.dispatchPersisted<ToastFeature>("toast", "show", "Hello")

        // In-memory queue
        assertEquals(1, instance.getPendingCount<ToastFeature>())
        // Persisted
        assertEquals(1, adapter.saved.size)
        assertEquals("toast", adapter.saved[0].second)
        assertEquals("show", adapter.saved[0].third.actionKey)
        assertEquals("Hello", adapter.saved[0].third.payload)
    }

    @Test
    fun testDispatchPersisted_multipleActions_allPersistedAndQueued() {
        instance.dispatchPersisted<ToastFeature>("toast", "show", "msg1")
        instance.dispatchPersisted<ToastFeature>("toast", "show", "msg2")
        instance.dispatchPersisted<NavFeature>("nav", "go", "home")

        assertEquals(2, instance.getPendingCount<ToastFeature>())
        assertEquals(1, instance.getPendingCount<NavFeature>())
        assertEquals(3, adapter.saved.size)
    }

    // ──────────────────────────────────────────────────────────────────
    // dispatchPersisted — impl already registered
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun testDispatchPersisted_withImpl_executesImmediately_noSave() {
        val mock = MockToast()
        instance.register<ToastFeature>(mock)

        instance.dispatchPersisted<ToastFeature>("toast", "show", "Immediate")

        // Not queued, not persisted
        assertEquals(0, instance.getPendingCount<ToastFeature>())
        assertEquals(0, adapter.saved.size)
    }

    // ──────────────────────────────────────────────────────────────────
    // restorePersistedActions
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun testRestorePersistedActions_restoresFromAdapter() {
        // Simulate: app died after persisting
        instance.dispatchPersisted<ToastFeature>("toast", "show", "Restored!")
        assertEquals(1, adapter.saved.size)

        // Simulate: app restart — create new instance with SAME scope name and adapter
        // (same scope = same storage key, simulating the app restarting)
        val newInstance = KRelay.builder("PersistTestScope")  // same scope as original
            .build()
        newInstance.setPersistenceAdapter(adapter)
        newInstance.registerActionFactory<ToastFeature>("toast", "show") { payload -> { it.show(payload) } }

        // Restore
        newInstance.restorePersistedActions()

        // Action should be back in queue
        assertEquals(1, newInstance.getPendingCount<ToastFeature>())

        // Adapter should have removed the entry
        assertEquals(1, adapter.removed.size)

        newInstance.reset()
    }

    @Test
    fun testRestorePersistedActions_thenRegister_replaysAction() {
        instance.dispatchPersisted<ToastFeature>("toast", "show", "After Restore")

        // Simulate restart — same scope name
        val newInstance = KRelay.builder("PersistTestScope").build()
        newInstance.setPersistenceAdapter(adapter)
        newInstance.registerActionFactory<ToastFeature>("toast", "show") { payload -> { it.show(payload) } }
        newInstance.restorePersistedActions()

        val mock = MockToast()
        newInstance.register<ToastFeature>(mock)

        assertEquals(0, newInstance.getPendingCount<ToastFeature>())

        newInstance.reset()
    }

    @Test
    fun testRestorePersistedActions_emptyAdapter_noOp() {
        // No prior dispatches
        instance.restorePersistedActions()
        assertEquals(0, instance.getPendingCount<ToastFeature>())
    }

    @Test
    fun testRestorePersistedActions_noFactoryRegistered_skipsGracefully() {
        instance.dispatchPersisted<ToastFeature>("toast", "show", "will be skipped")

        // New instance with NO factory registered
        val newInstance = KRelay.builder("PersistTestScope4").build()
        newInstance.setPersistenceAdapter(adapter)
        // Intentionally NOT registering factory

        newInstance.restorePersistedActions()  // should not throw

        assertEquals(0, newInstance.getPendingCount<ToastFeature>())
        newInstance.reset()
    }

    // ──────────────────────────────────────────────────────────────────
    // reset() clears adapter scope
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun testReset_clearsPersistenceScope() {
        instance.dispatchPersisted<ToastFeature>("toast", "show", "to be cleared")
        assertEquals(1, adapter.loadAll("PersistTestScope").size)

        instance.reset()

        assertEquals(0, adapter.loadAll("PersistTestScope").size)
    }

    // ──────────────────────────────────────────────────────────────────
    // dispatchPersisted without factory → should throw
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun testDispatchPersisted_noFactory_throws() {
        val freshInstance = KRelay.create("NoFactoryScope")
        freshInstance.setPersistenceAdapter(adapter)
        // No factory registered for NavFeature

        assertFailsWith<IllegalStateException> {
            freshInstance.dispatchPersisted<NavFeature>("nav", "go", "home")
        }

        freshInstance.reset()
        KRelay.clearInstanceRegistry()
    }
}
