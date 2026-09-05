package dev.brewkits.krelay.testing

import dev.brewkits.krelay.RelayFeature

/**
 * A JVM/Android test rule that provides a [FakeKRelayInstance] and automatically
 * resets it after each test.
 *
 * ## Usage with kotlin.test
 *
 * ```kotlin
 * class MyViewModelTest {
 *     private val relayRule = KRelayTestRule()
 *     private val viewModel by lazy { MyViewModel(krelay = relayRule.relay) }
 *
 *     @AfterTest
 *     fun tearDown() = relayRule.after()
 *
 *     @Test
 *     fun `on error dispatches toast`() {
 *         viewModel.simulateError()
 *         relayRule.relay.assertDispatched<ToastFeature>()
 *     }
 * }
 * ```
 */
class KRelayTestRule {

    /** The [FakeKRelayInstance] to inject into the system under test. */
    val relay = FakeKRelayInstance()

    /**
     * Resets the [FakeKRelayInstance] state.
     * Call this from an `@AfterTest` function.
     */
    fun after() {
        relay.reset()
    }
}

// ---------------------------------------------------------------------------
// Convenience extension — execute last dispatch against a test double
// ---------------------------------------------------------------------------

/**
 * Retrieves the last dispatch for feature [T] and immediately executes it against [impl].
 * Useful for asserting the side effect of a dispatch in a single line.
 *
 * ```kotlin
 * val fake = FakeKRelayInstance()
 * viewModel.showError()
 *
 * var message: String? = null
 * fake.executeLastDispatch<ToastFeature>(object : ToastFeature {
 *     override fun show(msg: String) { message = msg }
 * })
 * assertEquals("Something went wrong", message)
 * ```
 */
inline fun <reified T : RelayFeature> FakeKRelayInstance.executeLastDispatch(impl: T) {
    val record = lastDispatchFor<T>()
    checkNotNull(record) { "No dispatch recorded for ${T::class.simpleName}" }
    record.executeWith(impl)
}
