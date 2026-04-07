# Under the Hood: How KRelay actually works

If you're reading this, you probably want to know what's happening behind the scenes. KRelay isn't magic—it's just a carefully synchronized bridge between your shared Kotlin code and the messy world of platform lifecycles.

---

## The Three Pillars

We built KRelay around three core problems we kept seeing in KMP projects:

### 1. Safe Dispatch (The Threading Problem)
**The Pain:** You call a Toast from a background thread in your ViewModel. Android crashes because you touched the UI from the wrong thread. iOS just ignores you.
**Our Fix:** Every `dispatch` in KRelay is automatically routed to the **Main Thread** (Looper on Android, Main Queue on iOS). You don't have to worry about `withContext(Dispatchers.Main)` anymore.

### 2. Weak Registry (The Memory Leak Problem)
**The Pain:** You pass an `Activity` to a ViewModel. The user rotates the screen. The old Activity is destroyed, but the ViewModel still holds a reference to it. **Memory Leak.**
**Our Fix:** KRelay only stores **WeakReferences** to your platform implementations. When your Activity or UIViewController dies, the reference becomes `null` automatically. No `onDestroy` cleanup required.

### 3. Sticky Queue (The "Blind Spot" Problem)
**The Pain:** You navigate to a new screen. While it's loading, you trigger a "Success" toast. But the new Activity hasn't registered its listener yet. The event is lost.
**Our Fix:** If no implementation is registered, KRelay pushes your action into a **Buffered Queue**. As soon as a listener registers, it "replays" the missed actions. No more lost signals during cold starts or rotations.

---

## How the data flows

### Scenario: The "Happy Path" (Implementation exists)
1. **Shared Code:** Calls `KRelay.dispatch<ToastFeature> { ... }` from any thread.
2. **KRelay:** Grabs a lock, finds the WeakRef for `ToastFeature`.
3. **Execution:** Since the implementation is there, it immediately wraps your block in a `runOnMain` call.
4. **Platform:** The code runs safely on the UI thread.

### Scenario: The "Blind Spot" (Implementation missing)
1. **Shared Code:** Dispatches an event while the UI is rotating.
2. **KRelay:** Finds no registered implementation.
3. **Queueing:** It wraps your action and puts it in the `pendingQueue`.
4. **Registration:** 300ms later, the new Activity calls `KRelay.register()`.
5. **Replay:** KRelay sees the pending queue, clears it, and runs every action on the Main Thread against the new Activity.

---

## Platform Specifics

We keep the core as thin as possible, delegating the "heavy lifting" to platform-native tools:

- **Android:** We use a `Handler(Looper.getMainLooper())` for dispatching and `java.lang.ref.WeakReference`.
- **iOS:** We use `dispatch_async(dispatch_get_main_queue())` and Kotlin/Native's own `WeakReference`.

---

## Design Decisions (and the trade-offs we made)

### Why a Singleton?
We chose `object KRelay` because for 90% of apps, a global bridge is exactly what you want. It's zero-config and matches platform patterns like `Dispatchers.Main`. 
*Update (v2.0):* We added the **Instance API** for Super Apps where you need isolated bridges per module.

### Why Reified Generics?
We wanted `KRelay.dispatch<MyFeature>`. It’s type-safe, provides great IDE autocomplete, and prevents string-key typos. The trade-off is that it’s not directly callable from Swift, so we provide a tiny Swift-friendly wrapper.

---

## Performance Considerations

KRelay is extremely lightweight. 
- **Memory:** A typical app with 10 features and a few queued actions uses **less than 1KB** of RAM.
- **CPU:** A dispatch takes roughly **2µs**. You could trigger thousands of these without breaking a sweat (though your UI might not appreciate it!).

---

## The "Process Death" Reality
KRelay stores its queue in memory. If the OS kills your app process, the queue is gone. 
**This is by design.** 
Events like Toasts and Navigation are ephemeral. For critical data that *must* survive process death (like a payment), use **WorkManager** or **Room**. KRelay is the messenger, not the database.

---

## When to use what?

| Feature | KRelay | expect/actual | StateFlow | WorkManager |
|---------|--------|---------------|-----------|-------------|
| **Platform Calls** | ✅ Perfect | ✅ Good | ❌ No | ❌ No |
| **Return Values** | ❌ No | ✅ Yes | ✅ Yes | ⚠️ Async only |
| **State Management** | ❌ No | ❌ No | ✅ Perfect | ❌ No |
| **Guaranteed Execution**| ❌ No | ✅ Yes | ✅ Yes | ✅ Yes |
| **Process Death Survival**| ❌ No | ✅ Yes | ⚠️ Depends | ✅ Yes |

---

**Last Updated:** April 7, 2026.
