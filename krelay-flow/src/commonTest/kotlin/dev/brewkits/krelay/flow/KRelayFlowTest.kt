package dev.brewkits.krelay.flow

import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.RelayFeature
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

interface FlowTestFeature : RelayFeature {
    fun receive(value: String)
}

class MockFlowFeature : FlowTestFeature {
    val received = mutableListOf<String>()
    override fun receive(value: String) {
        received.add(value)
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class KRelayFlowTest {

    @BeforeTest
    fun setup() {
        KRelay.reset()
    }

    @Test
    fun testStateFlowRelay() = runTest {
        val stateFlow = MutableStateFlow("initial")
        val mock = MockFlowFeature()
        
        // Setup KRelay
        KRelay.register<FlowTestFeature>(mock)
        
        // Bind flow
        val job = stateFlow.relayTo<String, FlowTestFeature>(KRelay.instance) { feature, value ->
            feature.receive(value)
        }.launchIn(this)
        
        // Yield to allow initial emission to be processed
        advanceUntilIdle()
        assertEquals(listOf("initial"), mock.received)
        
        // Emit new value
        stateFlow.value = "update1"
        advanceUntilIdle()
        assertEquals(listOf("initial", "update1"), mock.received)
        
        job.cancel()
    }

    @Test
    fun testSharedFlowRelay() = runTest {
        val sharedFlow = MutableSharedFlow<String>(replay = 1)
        val mock = MockFlowFeature()
        
        KRelay.register<FlowTestFeature>(mock)
        
        val job = sharedFlow.relayTo<String, FlowTestFeature>(KRelay.instance) { feature, value ->
            feature.receive(value)
        }.launchIn(this)
        
        sharedFlow.tryEmit("event1")
        advanceUntilIdle()
        assertEquals(listOf("event1"), mock.received)
        
        sharedFlow.tryEmit("event2")
        advanceUntilIdle()
        assertEquals(listOf("event1", "event2"), mock.received)
        
        job.cancel()
    }
}
