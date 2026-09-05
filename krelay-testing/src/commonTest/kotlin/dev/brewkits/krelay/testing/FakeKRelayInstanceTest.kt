package dev.brewkits.krelay.testing

import dev.brewkits.krelay.RelayFeature
import kotlin.test.Test
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Minimal test features
private interface TestToastFeature : RelayFeature {
    fun show(message: String)
}

private interface TestNavFeature : RelayFeature {
    fun navigateTo(screen: String)
}

class FakeKRelayInstanceTest {

    private val fake = FakeKRelayInstance()

    @AfterTest
    fun tearDown() = fake.reset()

    @Test
    fun `dispatch count is zero initially`() {
        assertEquals(0, fake.dispatchCount())
    }

    @Test
    fun `records dispatch and increments count`() {
        fake.dispatch(TestToastFeature::class, { it.show("Hello") })
        assertEquals(1, fake.dispatchCountFor<TestToastFeature>())
    }

    @Test
    fun `lastDispatchFor returns null when no dispatch`() {
        assertNull(fake.lastDispatchFor<TestToastFeature>())
    }

    @Test
    fun `lastDispatchFor returns most recent dispatch`() {
        fake.dispatch(TestToastFeature::class, { it.show("First") })
        fake.dispatch(TestToastFeature::class, { it.show("Second") })
        val record = fake.lastDispatchFor<TestToastFeature>()
        var captured = ""
        record?.executeWith(object : TestToastFeature {
            override fun show(message: String) { captured = message }
        })
        assertEquals("Second", captured)
    }

    @Test
    fun `dispatch is isolated per feature type`() {
        fake.dispatch(TestToastFeature::class, { it.show("toast") })
        assertEquals(1, fake.dispatchCountFor<TestToastFeature>())
        assertEquals(0, fake.dispatchCountFor<TestNavFeature>())
    }

    @Test
    fun `assertDispatched passes when dispatched`() {
        fake.dispatch(TestToastFeature::class, { it.show("ok") })
        fake.assertDispatched<TestToastFeature>() // should not throw
    }

    @Test
    fun `assertNotDispatched passes when not dispatched`() {
        fake.assertNotDispatched<TestToastFeature>() // should not throw
    }

    @Test
    fun `register makes isRegistered return true`() {
        assertFalse(fake.isRegistered(TestToastFeature::class))
        fake.register(TestToastFeature::class, object : TestToastFeature {
            override fun show(message: String) {}
        })
        assertTrue(fake.isRegistered(TestToastFeature::class))
    }

    @Test
    fun `unregister makes isRegistered return false`() {
        val impl = object : TestToastFeature {
            override fun show(message: String) {}
        }
        fake.register(TestToastFeature::class, impl)
        fake.unregister(TestToastFeature::class)
        assertFalse(fake.isRegistered(TestToastFeature::class))
    }

    @Test
    fun `dispatch immediately executes against registered impl`() {
        var captured = ""
        fake.register(TestToastFeature::class, object : TestToastFeature {
            override fun show(message: String) { captured = message }
        })
        fake.dispatch(TestToastFeature::class, { it.show("live!") })
        assertEquals("live!", captured)
    }

    @Test
    fun `reset clears all state`() {
        fake.dispatch(TestToastFeature::class, { it.show("x") })
        fake.register(TestToastFeature::class, object : TestToastFeature {
            override fun show(message: String) {}
        })
        fake.reset()
        assertEquals(0, fake.dispatchCount())
        assertFalse(fake.isRegistered(TestToastFeature::class))
    }

    @Test
    fun `KRelayTestRule resets after each test`() {
        val rule = KRelayTestRule()
        rule.relay.dispatch(TestToastFeature::class, { it.show("test") })
        assertEquals(1, rule.relay.dispatchCountFor<TestToastFeature>())
        rule.after()
        assertEquals(0, rule.relay.dispatchCountFor<TestToastFeature>())
    }
}
