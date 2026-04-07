# State vs. Event: Why your MVI/Redux app is probably leaking side-effects

**Author:** Nguyễn Tuấn Việt (Senior Tech Lead)  
**Date:** April 7, 2026.

---

### The "Everything is State" Trap

In modern mobile architecture (MVI, Redux, Bloc), we’ve been told that everything should be "State." It sounds clean, but it's a trap for side-effects like Toasts, Navigation, and Alerts. 

State is **persistent**. Events are **transient**. When you mix them, you get these classic bugs:
1. **The Ghost Toast:** You rotate the screen, the UI rebuilds, and the "Login Successful" toast pops up again because it's still stuck in the State.
2. **The Blind Spot:** You trigger a navigation event while the Activity is restarting. The event fires, but the UI isn't listening yet. *Poof* — the event is gone, and the user is stuck.
3. **The Cleanup Spaghetti:** You end up writing `setEvent(null)` or `eventConsumed()` everywhere just to "clean up" the state after it's been used. That's just boilerplate waiting to break.

---

### KRelay: A Better Way to Handle "One-Shot" Actions

KRelay isn't just another library; it's a standardized way to handle events that should happen **exactly once**, regardless of what's happening with the UI lifecycle. We call it **Buffered Multicasting**.

#### 1. Buffering (No more "Blind Spots")
KRelay holds your events in a small in-memory queue if the UI isn't ready. When your Activity or View finally registers, KRelay immediately "replays" those missed events. No more lost navigation signals during that 300ms window when the app is cold-starting or rotating.

#### 2. Multicasting (One signal, multiple consumers)
Sometimes a single event needs to trigger multiple things: show a Toast, send an Analytics log, and update a Notification. Standard `Channels` (which only support one listener) fail here. KRelay lets everyone listen to the same signal without stealing it from each other.

#### 3. No-Replay (Natural Lifecycle)
Unlike `StateFlow` or `LiveData`, KRelay events vanish the moment they are consumed. There is no "previous state" to haunt you when the screen rotates.

---

### Real-World Architecture

If you're building something **Mission-Critical** — think Fintech, VoIP, or Emergency SOS systems — you can't afford to "miss" a signal because a fragment was transitioning.

- **Core Logic:** Stays in Kotlin Multiplatform. It doesn't care about Android Activities or iOS ViewControllers.
- **The Bus (KRelay):** Acts as the high-reliability pipe.
- **The UI:** Just registers itself when it's ready to draw.

### The Manifesto

> **"State is for seeing, Event is for running."**

If a user needs to *see* it (like a profile name), put it in State. If the app needs to *do* something (like navigate or vibrate), use a Relay. Stop forcing your UI to guess whether an event is "new" or "old." Use a buffered relay and get back to building features that actually work.
