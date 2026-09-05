package dev.brewkits.krelay.flow

import dev.brewkits.krelay.KRelayInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow

import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

import dev.brewkits.krelay.RelayFeature
import dev.brewkits.krelay.dispatch



/**
 * Binds a StateFlow to KRelay, dispatching every new value to the relay.
 */
suspend inline fun <T : Any, reified F : RelayFeature> StateFlow<T>.relayTo(
    instance: KRelayInstance,
    crossinline mapper: (F, T) -> Unit
) {
    collect { value ->
        instance.dispatch<F> { feature ->
            mapper(feature, value)
        }
    }
}

/**
 * Binds a SharedFlow to KRelay, dispatching every new value to the relay.
 */
suspend inline fun <T : Any, reified F : RelayFeature> SharedFlow<T>.relayTo(
    instance: KRelayInstance,
    crossinline mapper: (F, T) -> Unit
) {
    collect { value ->
        instance.dispatch<F> { feature ->
            mapper(feature, value)
        }
    }
}
