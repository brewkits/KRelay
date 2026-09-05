package dev.brewkits.krelay.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import dev.brewkits.krelay.KRelay
import dev.brewkits.krelay.KRelayInstance
import dev.brewkits.krelay.RelayFeature
import org.junit.Rule
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

interface DummyComposeFeature : RelayFeature {
    fun getLabel(): String
}

class KRelayComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var krelay: KRelayInstance

    @BeforeTest
    fun setup() {
        krelay = KRelay.create("ComposeTestScope")
    }

    @AfterTest
    fun tearDown() {
        krelay.reset()
    }

    @Test
    fun testKRelayEffect_registersAndUnregisters() {
        var isComposed by mutableStateOf(true)
        val impl = object : DummyComposeFeature {
            override fun getLabel() = "test"
        }

        composeTestRule.setContent {
            if (isComposed) {
                KRelayEffect<DummyComposeFeature>(instance = krelay) { impl }
            }
        }

        // Verify it is registered
        assertTrue(krelay.isRegistered(DummyComposeFeature::class))

        // Remove from composition
        isComposed = false
        composeTestRule.waitForIdle()

        // Verify it is unregistered
        assertFalse(krelay.isRegistered(DummyComposeFeature::class))
    }

    @Test
    fun testRememberKRelayImpl_updatesOnKeyChange() {
        var key by mutableStateOf(1)
        
        composeTestRule.setContent {
            rememberKRelayImpl<DummyComposeFeature>(instance = krelay, keys = arrayOf(key)) {
                object : DummyComposeFeature {
                    override fun getLabel() = "impl_$key"
                }
            }
        }

        // Initially key is 1
        var label = ""
        krelay.dispatch(DummyComposeFeature::class) {
            label = it.getLabel()
        }
        composeTestRule.waitForIdle()
        assertEquals("impl_1", label)

        // Change key to 2, causing recomposition and new impl registration
        key = 2
        composeTestRule.waitForIdle()

        krelay.dispatch(DummyComposeFeature::class) {
            label = it.getLabel()
        }
        composeTestRule.waitForIdle()
        assertEquals("impl_2", label)
    }
}
