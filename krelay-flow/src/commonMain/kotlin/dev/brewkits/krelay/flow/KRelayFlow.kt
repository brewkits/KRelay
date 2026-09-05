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


import kotlinx.coroutines.flow.onEach

/**
 * Binds any Flow to KRelay, dispatching every new value to the relay.
 * This is an intermediate operator. You must use a terminal operator like `launchIn` 
 * or `collect` to start the flow.
 *
 * Example:
 * ```
 * stateFlow
 *     .relayTo<String, ToastFeature>(instance) { feature, value -> feature.show(value) }
 *     .launchIn(viewModelScope)
 * ```
 */
inline fun <T : Any, reified F : RelayFeature> Flow<T>.relayTo(
    instance: KRelayInstance,
    crossinline mapper: (F, T) -> Unit
): Flow<T> = onEach { value ->
    instance.dispatch<F> { feature ->
        mapper(feature, value)
    }
}
