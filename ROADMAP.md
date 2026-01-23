# KRelay Development Roadmap

This document outlines the strategic development plan for KRelay, focusing on reliability, platform expansion, and enterprise readiness.

## Vision

**Mission**: Become the standard solution for clean, leak-free platform interop in Kotlin Multiplatform projects.

**Core Values**:
- **Simplicity First**: Never sacrifice clarity for features
- **Reliability**: Zero-memory-leak guarantee
- **Developer Experience**: Clear APIs, excellent documentation
- **Unix Philosophy**: Do one thing and do it well

---

## 🚀 Phase 1: Launch & Education (Months 1-2)

**Goal**: Prove reliability and establish credibility in the KMP community.

### Maven Central Publishing

**Status**: ✅ Setup Complete

**Tasks**:
- [x] Configure Maven Central publishing in build.gradle.kts
- [x] Create GPG signing configuration
- [x] Write publishing automation script
- [x] Document setup process
- [x] Create Sonatype JIRA account
- [x] Request dev.brewkits group ID verification
- [x] Publish v1.0.0 to Maven Central
- [x] Publish v1.0.1 with iOS platform artifacts

**Deliverables**:
- Published artifact: `dev.brewkits:krelay:1.0.1`
- Available on Maven Central within 24 hours of release
- Separate iOS platform publications (iosarm64, iossimulatorarm64, iosx64)

### Community Education

**Goal**: Help developers understand when and why to use KRelay.

**Content Strategy**:

1. **Technical Blog Posts** (Medium/Dev.to)
   - [ ] "Stop Using SharedFlow for One-off Events in KMP"
     - Problem: SharedFlow causes lifecycle leaks
     - Solution: KRelay's weak reference pattern
     - Visual proof: Screen rotation demo

   - [ ] "The Glue Code Standard: Clean Platform Interop in KMP"
     - Problem: expect/actual clutter in shared code
     - Solution: Feature interfaces + KRelay dispatch
     - Code examples: Before/After comparison

   - [ ] "Zero-Leak ViewModels: How KRelay Solves the Platform Call Problem"
     - Technical deep dive
     - Memory leak analysis
     - Comparison with alternatives

2. **Visual Demos**
   - [ ] GIF: Screen rotation preserving Toast
     - Show: Rotate → Toast appears on new Activity
     - Proof: Queue persistence across configuration changes

   - [ ] Video: Basic integration tutorial (5 minutes)
     - Setup KRelay in new project
     - Implement first feature
     - Show debug logs

   - [ ] Video: Decompose navigation integration (10 minutes)
     - Complete navigation demo
     - ViewModels with zero dependencies
     - Testing with mocks

3. **Documentation**
   - [x] README.md with clear value proposition
   - [x] ARCHITECTURE.md technical deep dive
   - [x] MAVEN_CENTRAL_SETUP.md publishing guide
   - [ ] Integration guides for popular libraries:
     - [ ] Voyager
     - [ ] Decompose
     - [ ] Moko Permissions
     - [ ] Moko Biometry

4. **Social Proof**
   - [ ] Reddit post on r/Kotlin
   - [ ] LinkedIn article
   - [ ] Twitter/X thread
   - [ ] Kotlin Slack #multiplatform channel

**Success Metrics**:
- 1000+ GitHub stars
- 50+ production apps using KRelay
- 3+ blog posts with 500+ views each
- Active community discussions

---

## 🛠 Phase 2: Expansion (Months 3-6) - v1.1, v1.2

**Goal**: Expand platform support and improve developer experience.

### v1.1.0: Desktop Support (Month 3-4)

**Target Platforms**: JVM Desktop (Windows, macOS, Linux)

**Why Desktop?**
- KMP is expanding beyond mobile
- Compose Desktop is growing
- Low implementation cost (similar to Android)

**Tasks**:
- [ ] Implement Desktop WeakRef (Java WeakReference)
- [ ] Implement Desktop MainThreadExecutor (SwingUtilities/JavaFX)
- [ ] Test on Windows/macOS/Linux
- [ ] Add Desktop integration examples
- [ ] Update documentation

**Example Use Cases**:
```kotlin
// Desktop notification
KRelay.dispatch<NotificationFeature> {
    it.showSystemNotification("Task Complete", "Your export is ready")
}

// Desktop menu updates
KRelay.dispatch<MenuFeature> {
    it.updateRecentFiles(recentFiles)
}
```

**Deliverables**:
- `dev.brewkits:krelay:1.1.0` with Desktop support
- Desktop demo app
- Integration guide

### v1.2.0: Web/JS Support (Month 5-6)

**Target Platforms**: Kotlin/JS, Kotlin/Wasm

**Why Web?**
- Compose Multiplatform for Web is maturing
- Full-stack Kotlin is gaining traction
- Complete KMP coverage

**Challenges**:
- No native WeakReference in JS
- Main thread = Event loop
- Browser lifecycle differences

**Proposed Implementation**:
```kotlin
// JS WeakRef using WeakMap
actual class WeakRef<T : Any>(referred: T) {
    private val weakMap = js("new WeakMap()")

    init {
        weakMap[uniqueKey] = referred
    }

    actual fun get(): T? = weakMap[uniqueKey]
}

// JS MainThreadExecutor
actual fun runOnMain(block: () -> Unit) {
    window.setTimeout({ block() }, 0)
}
```

**Tasks**:
- [ ] Research JS WeakRef compatibility
- [ ] Implement JS/Wasm WeakRef
- [ ] Implement JS MainThreadExecutor
- [ ] Test on major browsers (Chrome, Firefox, Safari)
- [ ] Add Web integration examples

**Deliverables**:
- `dev.brewkits:krelay:1.2.0` with Web/JS support
- Web demo app (Compose for Web)
- Browser compatibility table

### Debugging Tools (v1.1+)

**Goal**: Help developers debug KRelay issues easily.

**Features**:

1. **Export Logs**
   ```kotlin
   // Enable debug mode
   KRelay.debugEnabled = true

   // Export logs
   val logs = KRelay.exportLogs()
   println(logs.toJson())
   ```

2. **Queue Inspector**
   ```kotlin
   // Inspect pending queue
   val queueState = KRelay.inspectQueue<ToastFeature>()
   println("Pending actions: ${queueState.size}")
   println("Oldest action: ${queueState.oldest.timestamp}")
   ```

3. **Registration Monitor**
   ```kotlin
   // Monitor registrations
   KRelay.onFeatureRegistered<ToastFeature> { feature ->
       println("ToastFeature registered: ${feature::class}")
   }

   KRelay.onFeatureUnregistered<ToastFeature> {
       println("ToastFeature unregistered")
   }
   ```

**Deliverables**:
- Debugging API in v1.1.0
- Debug logging guide
- Troubleshooting documentation

---

## 🏢 Phase 3: Enterprise Ready (6+ months) - v2.0

**Goal**: Support large-scale apps, modularization, and Super App architectures.

### v2.0.0: Instance-Based API

**Problem**: Current singleton doesn't support:
- Multi-module apps with isolated concerns
- Testing module independence
- Feature namespacing in Super Apps

**Solution**: Optional instance-based API

**Prototype** (from KRelayV2Prototype.kt):
```kotlin
// Create named instances
val authRelay = KRelay.create("AuthModule")
val rideRelay = KRelay.create("RideModule")

// Each instance has isolated registry
authRelay.register<ToastFeature>(AuthToastImpl())
rideRelay.register<ToastFeature>(RideToastImpl())

// Dispatch to specific instance
authRelay.dispatch<ToastFeature> { it.show("Auth success") }
rideRelay.dispatch<ToastFeature> { it.show("Ride started") }
```

**Design Principles**:
- **Backward Compatible**: Singleton API stays default
- **Opt-In**: Only use instances when needed
- **Simple Migration**: Easy upgrade path

**Implementation Plan**:

1. **Core Changes**:
   ```kotlin
   // Existing singleton becomes default instance
   object KRelay {
       private val defaultInstance = KRelayInstance("default")

       // Delegate to default instance
       fun <reified T : RelayFeature> dispatch(action: (T) -> Unit) =
           defaultInstance.dispatch(action)

       // Factory for custom instances
       fun create(name: String): KRelayInstance =
           KRelayInstance(name)
   }

   // New instance class
   class KRelayInstance(val name: String) {
       private val registry = mutableMapOf<KClass<*>, WeakRef<Any>>()
       // ... isolated implementation
   }
   ```

2. **DI Integration**:
   ```kotlin
   // Koin module
   module {
       single { KRelay.create("AuthModule") }
       single { KRelay.create("RideModule") }
   }

   // ViewModel
   class AuthViewModel(
       private val relay: KRelayInstance // Injected
   ) {
       fun login() {
           relay.dispatch<ToastFeature> { it.show("Login success") }
       }
   }
   ```

**Tasks**:
- [ ] Design instance API
- [ ] Implement KRelayInstance class
- [ ] Add factory method to KRelay object
- [ ] Update all tests
- [ ] Write migration guide
- [ ] Create DI integration examples (Koin, Hilt)

**Deliverables**:
- `dev.brewkits:krelay:2.0.0` with instance API
- Migration guide
- DI integration samples
- Super App architecture guide

### Dependency Injection Support

**Goal**: First-class support for popular DI frameworks.

**Supported Frameworks**:

1. **Koin**
   ```kotlin
   val krelayModule = module {
       single { KRelay.create("MyModule") }
       factory {
           MyViewModel(relay = get())
       }
   }
   ```

2. **Hilt (Android)**
   ```kotlin
   @Module
   @InstallIn(SingletonComponent::class)
   object KRelayModule {
       @Provides
       @Singleton
       fun provideKRelay(): KRelayInstance =
           KRelay.create("MyModule")
   }

   @HiltViewModel
   class MyViewModel @Inject constructor(
       private val relay: KRelayInstance
   ) : ViewModel()
   ```

3. **Kotlin-Inject**
   ```kotlin
   @Component
   class AppComponent {
       @Provides
       fun provideKRelay(): KRelayInstance =
           KRelay.create("MyModule")
   }
   ```

**Deliverables**:
- Integration guides for each framework
- Sample projects
- Best practices documentation

### Super App Architecture Patterns

**Goal**: Enable large-scale app development with KRelay.

**Challenges in Super Apps**:
- Multiple independent modules
- Feature naming conflicts
- Isolated testing requirements
- Team ownership boundaries

**KRelay Solutions**:

1. **Module Isolation**
   ```kotlin
   // Each module has its own KRelay instance
   object AuthModule {
       val relay = KRelay.create("Auth")

       interface ToastFeature : RelayFeature {
           fun show(message: String)
       }
   }

   object RideModule {
       val relay = KRelay.create("Ride")

       interface ToastFeature : RelayFeature {
           fun show(message: String)
       }
   }

   // No naming conflicts!
   ```

2. **Cross-Module Communication**
   ```kotlin
   // Event bus pattern for module-to-module
   interface ModuleEventBus : RelayFeature {
       fun onAuthComplete(userId: String)
       fun onRideStarted(rideId: String)
   }

   // Shared relay for cross-module events
   val appRelay = KRelay.create("App")
   ```

3. **Testing Isolation**
   ```kotlin
   @Test
   fun `auth module test`() {
       // Each test gets fresh instance
       val testRelay = KRelay.create("AuthTest_${UUID.randomUUID()}")
       val viewModel = AuthViewModel(testRelay)

       // Test in complete isolation
   }
   ```

**Deliverables**:
- Super App architecture guide
- Multi-module sample project
- Module isolation best practices
- Cross-module communication patterns

---

## Performance & Quality Goals

### Performance Targets

**Latency**:
- Dispatch overhead: < 2µs (current: < 2µs ✅)
- Memory per feature: < 100 bytes (current: ~80 bytes ✅)
- Queue replay: < 1ms for 100 actions

**Memory**:
- Total library overhead: < 10KB
- Queue memory: Bounded by maxQueueSize
- Zero memory leaks (guaranteed)

**Benchmarks**:
- [ ] Create performance test suite
- [ ] Measure dispatch latency
- [ ] Profile memory usage
- [ ] GC pressure analysis
- [ ] Publish benchmark results

### Code Quality

**Metrics**:
- Test coverage: > 90%
- Core codebase: < 500 lines
- Documentation coverage: 100%
- No compiler warnings

**Tools**:
- [ ] Set up Detekt (Kotlin linter)
- [ ] Configure Ktlint (code formatter)
- [ ] Add code coverage reporting
- [ ] Set up CI/CD (GitHub Actions)

---

## Community & Ecosystem

### Community Building

**Goals**:
- Active GitHub Discussions
- Regular blog posts
- Conference talks
- Open source contributions

**Activities**:
- [ ] Enable GitHub Discussions
- [ ] Create Discord/Slack community
- [ ] Monthly blog posts
- [ ] Submit KotlinConf talk proposal
- [ ] Submit Droidcon talk proposal

### Ecosystem Integration

**Popular Libraries**:
- [ ] Voyager integration guide
- [ ] Decompose integration guide
- [ ] Moko libraries integration
- [ ] Ktor integration examples
- [ ] Compose Multiplatform samples

**Documentation**:
- [ ] Integration guides for each library
- [ ] Video tutorials
- [ ] Sample projects repository
- [ ] Best practices cookbook

---

## Non-Goals (By Design)

These features will **NEVER** be added to maintain simplicity and focus:

❌ **Suspend Function Support**
- Violates fire-and-forget pattern
- Use expect/actual instead

❌ **State Management**
- Use StateFlow/MutableStateFlow
- State ≠ Events

❌ **Background Processing**
- Always runs on main thread (by design)
- Use Dispatchers.IO for heavy work

❌ **Persistent Queue**
- Lambdas can't be serialized
- Use WorkManager for critical tasks

❌ **Built-in DI**
- Use Koin/Hilt/Kotlin-Inject
- Not KRelay's scope

See [ARCHITECTURE.md](docs/ARCHITECTURE.md) for detailed rationale.

---

## Success Metrics

### Adoption

**Year 1 Goals**:
- 2,000+ GitHub stars
- 100+ production apps
- 10,000+ downloads from Maven Central
- Featured in Kotlin Weekly

**Year 2 Goals**:
- 5,000+ GitHub stars
- 500+ production apps
- 50,000+ downloads
- Conference talks accepted

### Quality

**Continuous Goals**:
- Zero critical bugs
- < 24hr issue response time
- > 90% test coverage
- 100% documentation coverage

### Community

**Engagement**:
- Active GitHub Discussions
- Regular blog posts (1-2/month)
- Conference presentations (1-2/year)
- Community contributions

---

## Timeline Summary

```
2026 Q1 (Jan-Mar)
├─ ✅ v1.0.0 Launch
├─ 📝 Maven Central Publishing
├─ 📝 Community Education (Blogs, Videos)
└─ 📝 GitHub Launch Campaign

2026 Q2 (Apr-Jun)
├─ v1.1.0 Desktop Support
├─ v1.2.0 Web/JS Support
└─ Debugging Tools

2026 Q3 (Jul-Sep)
├─ v2.0.0 Design & Prototyping
├─ Instance-based API
└─ DI Integration

2026 Q4 (Oct-Dec)
├─ v2.0.0 Release
├─ Super App Guide
└─ Enterprise Case Studies

2027+
├─ Ecosystem Growth
├─ Conference Talks
└─ Continuous Improvement
```

---

## Versioning Strategy

**Semantic Versioning** (MAJOR.MINOR.PATCH):

- **MAJOR** (breaking changes):
  - v1.0.0 → v2.0.0: Add instance-based API
  - Rare, well-communicated
  - Migration guide provided

- **MINOR** (new features, backward compatible):
  - v1.0.0 → v1.1.0: Desktop support
  - v1.1.0 → v1.2.0: Web/JS support
  - Quarterly releases

- **PATCH** (bug fixes):
  - v1.0.0 → v1.0.1: Fix critical bug
  - As needed

**Stability Guarantees**:
- v1.x: Production ready, stable API
- v2.x: Enhanced for enterprise, backward compatible migration
- No experimental features in main artifact

---

## Get Involved

**Contribute**:
- GitHub: https://github.com/brewkits/KRelay
- Issues: Bug reports, feature requests
- Discussions: Architecture, best practices
- PRs: Always welcome!

**Stay Updated**:
- Watch GitHub repository for releases
- Follow [@brewkits](https://twitter.com/brewkits) on Twitter
- Join discussions on Kotlin Slack #multiplatform

---

**Last Updated**: 2026-01-23
**Current Version**: v1.0.1
**Next Release**: v1.1.0 (Desktop Support) - Planned Q2 2026
