# KRelay Documentation

> Complete documentation for KRelay - The Glue Code Standard for Kotlin Multiplatform

---

## 📚 Getting Started

Start here to understand KRelay and get it working in your project:

1. **[Main README](../README.md)** - Overview, Quick Start, and Key Features
2. **[Integration Guides](INTEGRATION_GUIDES.md)** - How to integrate with Voyager, Moko, Peekaboo, etc.
3. **[Quick Reference](QUICK_REFERENCE.md)** - Complete API documentation
4. **[iOS Test Report](IOS_TEST_REPORT.md)** 📱 - v1.1.0 iOS validation results

---

## 🎯 Core Documentation

### Essential Guides

- **[Integration Guides](INTEGRATION_GUIDES.md)** *(790 lines)*
  - Universal 4-step pattern
  - Moko Permissions, Moko Biometry
  - Voyager, Decompose, Compose Navigation
  - Peekaboo, Play Core/StoreKit, Firebase Analytics
  - Testing integrations

- **[Anti-Patterns](ANTI_PATTERNS.md)** *(669 lines)*
  - What NOT to use KRelay for
  - Real Super App scenarios
  - Critical vs Non-critical operations
  - When to use WorkManager instead

- **[Testing Guide](TESTING.md)** *(799 lines)*
  - Unit testing ViewModels with KRelay
  - Mock implementations
  - Testing patterns and examples

- **[Managing Warnings](MANAGING_WARNINGS.md)** *(427 lines)*
  - Understanding @ProcessDeathUnsafe
  - Understanding @SuperAppWarning
  - How to suppress warnings at module level
  - Best practices for opt-in annotations

---

## 🏗️ Technical Deep Dives

### Advanced Topics

- **[Architecture](ARCHITECTURE.md)** *(1802 lines)*
  - Internal implementation details
  - WeakReference mechanism
  - Queue management
  - Thread safety
  - Platform-specific implementations

- **[Positioning](POSITIONING.md)** *(678 lines)*
  - Why KRelay exists
  - The "Last Mile Problem" in KMP
  - KRelay as The Glue Code Standard
  - Comparison with alternatives

- **[Quick Reference](QUICK_REFERENCE.md)** *(541 lines)*
  - Complete API documentation
  - All functions with examples
  - Configuration options
  - Debug mode

---

## 📖 Design Decisions

### Architecture Decision Records (ADR)

- **[ADR-0001: Singleton and Serialization Tradeoffs](adr/0001-singleton-and-serialization-tradeoffs.md)**
  - Why KRelay uses global singleton
  - Why queue is not persistent
  - Trade-offs and alternatives
  - Future improvements (v2.0)

---

## 🚀 Quick Navigation

### By Use Case

**"I want to integrate a navigation library"**
→ [Integration Guides: Voyager/Decompose](INTEGRATION_GUIDES.md#3-voyager-navigation)

**"I need to request permissions from ViewModel"**
→ [Integration Guides: Moko Permissions](INTEGRATION_GUIDES.md#1-moko-permissions)

**"Can I use KRelay for payments/uploads?"**
→ [Anti-Patterns: Critical Operations](ANTI_PATTERNS.md)

**"How do I test ViewModels that use KRelay?"**
→ [Testing Guide](TESTING.md)

**"Too many @OptIn warnings in my code"**
→ [Managing Warnings: Module-level Suppression](MANAGING_WARNINGS.md)

**"How does KRelay work internally?"**
→ [Architecture: Deep Dive](ARCHITECTURE.md)

---

## 📊 Documentation Structure

```
docs/
├── README.md                           # This file - Documentation index
├── INTEGRATION_GUIDES.md              # How to integrate libraries ⭐
├── ANTI_PATTERNS.md                   # What NOT to do ⚠️
├── TESTING.md                         # Testing guide 🧪
├── MANAGING_WARNINGS.md               # OptIn annotations guide
├── ARCHITECTURE.md                    # Technical deep dive 🏗️
├── POSITIONING.md                     # Why KRelay exists 🎯
├── QUICK_REFERENCE.md                 # API docs 📖
└── adr/
    └── 0001-singleton-and-serialization-tradeoffs.md
```

---

## 🎓 Learning Path

### Beginner → Advanced

1. **Start**: Read [Main README](../README.md) - Understand what KRelay is
2. **Quick Win**: Follow [Quick Start](../README.md#quick-start) - Get it working in 5 minutes
3. **Real Integration**: Pick a library from [Integration Guides](INTEGRATION_GUIDES.md)
4. **Best Practices**: Read [Anti-Patterns](ANTI_PATTERNS.md) - Learn what to avoid
5. **Testing**: Implement tests using [Testing Guide](TESTING.md)
6. **Deep Dive**: Understand internals in [Architecture](ARCHITECTURE.md)
7. **Philosophy**: Read [Positioning](POSITIONING.md) - See the bigger picture

---

## 🔍 Search by Keyword

- **Memory Leaks** → [Main README](../README.md#problem-1-memory-leaks-from-strong-references) | [Architecture](ARCHITECTURE.md)
- **Process Death** → [Anti-Patterns](ANTI_PATTERNS.md) | [ADR-0001](adr/0001-singleton-and-serialization-tradeoffs.md)
- **Super App** → [Anti-Patterns](ANTI_PATTERNS.md) | [@SuperAppWarning](MANAGING_WARNINGS.md)
- **Voyager** → [Integration Guides](INTEGRATION_GUIDES.md#3-voyager-navigation)
- **Moko Permissions** → [Integration Guides](INTEGRATION_GUIDES.md#1-moko-permissions)
- **Testing** → [Testing Guide](TESTING.md)
- **Thread Safety** → [Architecture](ARCHITECTURE.md)
- **Queue** → [Architecture](ARCHITECTURE.md) | [Anti-Patterns](ANTI_PATTERNS.md)

---

## 💡 Need Help?

- **Issues**: [GitHub Issues](https://github.com/yourusername/krelay/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/krelay/discussions)
- **Can't find integration for your library?** Open an issue - we'll create a guide!

---

**Made with ❤️ for the Kotlin Multiplatform community**
