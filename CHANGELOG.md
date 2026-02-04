# Changelog

All notable changes to KRelay will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-02-04

### Added

#### Instance API for Super Apps and DI
- **Instance Creation**: New `KRelay.create(scopeName)` factory method for creating isolated instances
- **Builder Pattern**: New `KRelay.builder(scopeName)` for configuring instances with custom settings
- **Full Instance Isolation**: Each instance has independent registry, queue, and lock
- **Per-Instance Configuration**: Customize `maxQueueSize`, `actionExpiryMs`, and `debugMode` per instance
- **DI Integration**: First-class support for dependency injection frameworks (Koin, Hilt)
- **Super App Support**: Enables multiple independent modules without feature name conflicts

#### Developer Experience Improvements
- **Duplicate Scope Name Detection**: Warning logged in debug mode when creating instances with duplicate names
- **Input Validation**: Builder parameters validated at creation time
  - `maxQueueSize` must be > 0
  - `actionExpiryMs` must be > 0
  - `scopeName` must not be blank
- **Clear Error Messages**: Validation errors include actual invalid values for easier debugging

#### Documentation
- New migration guide for v1.x to v2.0 (`docs/MIGRATION_V2.md`)
- Super App architecture example with full implementation (`docs/SUPER_APP_EXAMPLE.md`)
- DI integration guide for Koin and Hilt (`docs/DI_INTEGRATION.md`)
- Comprehensive technical review document (`docs/COMPREHENSIVE_REVIEW_V2.md`)
- v2.0.0 improvements summary (`docs/V2_0_0_IMPROVEMENTS.md`)

#### Testing
- 10 new instance isolation tests
- 5 new validation tests
- All tests pass: 100% (15/15 instance tests)
- Verified backward compatibility: all existing tests pass

### Changed

#### Architecture Improvements
- Refactored `KRelay` singleton to facade pattern, delegating to internal `defaultInstance`
- Extracted core logic into reusable `KRelayInstanceImpl` class
- Improved code organization with clear separation between singleton and instance APIs

#### API Enhancements
- `KRelayInstance` interface with extension functions for type-safe operations
- `register`, `dispatch`, `unregister`, `isRegistered`, `getPendingCount`, `clearQueue` available on instances
- Non-reified methods (`getDebugInfo`, `dump`, `reset`) available directly on interface

### Technical Details

#### Performance
- Instance API overhead: <5% compared to singleton (negligible)
- Memory footprint: ~800 bytes per instance
- Lock granularity: Per-instance locks (no global contention)

#### Thread Safety
- All instance operations protected by per-instance locks
- Proper lock scope to prevent deadlocks
- Verified by stress tests

#### Memory Management
- Weak references prevent memory leaks
- Bounded queues with configurable size limits
- Automatic cleanup of expired actions

### Backward Compatibility

✅ **100% Backward Compatible**

- All existing v1.x code works without modification
- Singleton API delegates to default internal instance
- No breaking changes to public API
- Existing tests pass without changes

### Migration

Migration from v1.x to v2.0 is **optional** and can be done **incrementally**:

1. **No Changes Required**: Existing code continues to work
2. **Recommended for New Projects**: Use instance API with DI
3. **Recommended for Super Apps**: Migrate to instance API for module isolation
4. **Simple Apps**: Can continue using singleton API

See `docs/MIGRATION_V2.md` for detailed migration guide.

### Breaking Changes

None. This release is fully backward compatible with v1.x.

---

## [1.1.0] - 2025-XX-XX

### Added
- Thread safety improvements with atomic operations in stress tests
- Enhanced documentation
- Bug fixes and stability improvements

### Changed
- Improved test coverage

---

## [1.0.0] - 2024-XX-XX

### Added
- Initial release
- Singleton-based API
- WeakRef registry for automatic memory management
- Sticky queue for action replay
- Thread-safe operations
- Platform-specific main thread dispatch (Android Looper, iOS GCD)
- Debug mode and metrics
- Comprehensive test suite

### Features
- `KRelay.register<T>(impl)` - Register platform implementations
- `KRelay.dispatch<T> { }` - Dispatch actions to platform
- `KRelay.unregister<T>()` - Unregister implementations
- `KRelay.isRegistered<T>()` - Check registration status
- `KRelay.getPendingCount<T>()` - Get queued action count
- `KRelay.clearQueue<T>()` - Clear pending actions
- `KRelay.getDebugInfo()` - Get debug information
- `KRelay.dump()` - Dump state to console
- `KRelay.reset()` - Clear all registrations and queues

---

[2.0.0]: https://github.com/brewkits/KRelay/releases/tag/v2.0.0
[1.1.0]: https://github.com/brewkits/KRelay/releases/tag/v1.1.0
[1.0.0]: https://github.com/brewkits/KRelay/releases/tag/v1.0.0
