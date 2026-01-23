# KRelay v1.0.1 - Maven Central Artifacts Summary

**Version**: 1.0.1
**Group ID**: dev.brewkits
**Date**: 2026-01-23

This document lists all generated Maven artifacts ready for upload to Maven Central (Sonatype OSSRH).

---

## What's New in v1.0.1

Version 1.0.1 adds **dedicated iOS platform artifacts** for improved dependency resolution and build performance:

- `krelay-iosarm64` - iOS devices (ARM64)
- `krelay-iossimulatorarm64` - iOS simulators on M1/M2 Macs (Apple Silicon)
- `krelay-iosx64` - iOS simulators on Intel Macs (Intel)

Previously, in v1.0.0, all iOS targets were bundled into the base `krelay` artifact. Now each iOS platform has its own publication, making dependency management cleaner and builds faster.

---

## Artifacts Location

All artifacts are located in:
```
krelay/build/maven-central-staging/dev/brewkits/
```

Bundle file:
```
krelay-1.0.1-bundle.zip (1.2MB)
```

---

## Artifact Publications

KRelay v1.0.1 includes **5 Maven publications**:

1. **krelay** - Base/metadata artifact
2. **krelay-android** - Android library
3. **krelay-iosarm64** - iOS ARM64 (devices)
4. **krelay-iossimulatorarm64** - iOS Simulator ARM64 (M1/M2 Macs)
5. **krelay-iosx64** - iOS Simulator X64 (Intel Macs)

---

## Detailed Artifact Structure

### 1. Base/Metadata Artifact (krelay)

**Path**: `krelay/1.0.1/`
**Artifact ID**: `krelay`

Main artifacts:
- `krelay-1.0.1.jar` - Multiplatform JAR (57,824 bytes)
- `krelay-1.0.1-sources.jar` - Common sources (12,117 bytes)
- `krelay-1.0.1.pom` - Maven POM file
- `krelay-1.0.1.module` - Gradle module metadata
- `krelay-1.0.1-kotlin-tooling-metadata.json` - Kotlin tooling metadata

Checksums (for each artifact):
- `.md5` - MD5 hash
- `.sha1` - SHA1 hash
- `.sha256` - SHA256 hash
- `.sha512` - SHA512 hash

Signatures:
- `.asc` - GPG signature

Metadata:
- `maven-metadata.xml` - Version metadata (with checksums and signature)

**Dependencies**: Implementation("dev.brewkits:krelay:1.0.1")

---

### 2. Android Library (krelay-android)

**Path**: `krelay-android/1.0.1/`
**Artifact ID**: `krelay-android`

Main artifacts:
- `krelay-android-1.0.1.aar` - Android library (35,920 bytes)
- `krelay-android-1.0.1-sources.jar` - Android sources (8,291 bytes)
- `krelay-android-1.0.1.pom` - Maven POM file
- `krelay-android-1.0.1.module` - Gradle module metadata

Checksums (for each artifact):
- `.md5` - MD5 hash
- `.sha1` - SHA1 hash
- `.sha256` - SHA256 hash
- `.sha512` - SHA512 hash

Signatures:
- `.asc` - GPG signature

Metadata:
- `maven-metadata.xml` - Version metadata (with checksums and signature)

**Usage**: Automatically resolved when using `androidTarget()` in Kotlin Multiplatform

---

### 3. iOS ARM64 - Devices (krelay-iosarm64)

**Path**: `krelay-iosarm64/1.0.1/`
**Artifact ID**: `krelay-iosarm64`

Main artifacts:
- `krelay-iosarm64-1.0.1.klib` - iOS native library (103K)
- `krelay-iosarm64-1.0.1-sources.jar` - iOS sources (40K)
- `krelay-iosarm64-1.0.1-metadata.jar` - Metadata JAR (4.1K)
- `krelay-iosarm64-1.0.1.pom` - Maven POM file
- `krelay-iosarm64-1.0.1.module` - Gradle module metadata

Checksums (for each artifact):
- `.md5` - MD5 hash
- `.sha1` - SHA1 hash
- `.sha256` - SHA256 hash
- `.sha512` - SHA512 hash

Signatures:
- `.asc` - GPG signature

Metadata:
- `maven-metadata.xml` - Version metadata (with checksums and signature)

**Usage**: Automatically resolved when using `iosArm64()` in Kotlin Multiplatform
**Platform**: iOS devices (iPhone, iPad) running on ARM64 processors

---

### 4. iOS Simulator ARM64 - M1/M2 Macs (krelay-iossimulatorarm64)

**Path**: `krelay-iossimulatorarm64/1.0.1/`
**Artifact ID**: `krelay-iossimulatorarm64`

Main artifacts:
- `krelay-iossimulatorarm64-1.0.1.klib` - iOS native library (103K)
- `krelay-iossimulatorarm64-1.0.1-sources.jar` - iOS sources (40K)
- `krelay-iossimulatorarm64-1.0.1-metadata.jar` - Metadata JAR (4.1K)
- `krelay-iossimulatorarm64-1.0.1.pom` - Maven POM file
- `krelay-iossimulatorarm64-1.0.1.module` - Gradle module metadata

Checksums (for each artifact):
- `.md5` - MD5 hash
- `.sha1` - SHA1 hash
- `.sha256` - SHA256 hash
- `.sha512` - SHA512 hash

Signatures:
- `.asc` - GPG signature

Metadata:
- `maven-metadata.xml` - Version metadata (with checksums and signature)

**Usage**: Automatically resolved when using `iosSimulatorArm64()` in Kotlin Multiplatform
**Platform**: iOS Simulator on Apple Silicon Macs (M1, M2, M3, etc.)

---

### 5. iOS Simulator X64 - Intel Macs (krelay-iosx64)

**Path**: `krelay-iosx64/1.0.1/`
**Artifact ID**: `krelay-iosx64`

Main artifacts:
- `krelay-iosx64-1.0.1.klib` - iOS native library (103K)
- `krelay-iosx64-1.0.1-sources.jar` - iOS sources (40K)
- `krelay-iosx64-1.0.1-metadata.jar` - Metadata JAR (4.1K)
- `krelay-iosx64-1.0.1.pom` - Maven POM file
- `krelay-iosx64-1.0.1.module` - Gradle module metadata

Checksums (for each artifact):
- `.md5` - MD5 hash
- `.sha1` - SHA1 hash
- `.sha256` - SHA256 hash
- `.sha512` - SHA512 hash

Signatures:
- `.asc` - GPG signature

Metadata:
- `maven-metadata.xml` - Version metadata (with checksums and signature)

**Usage**: Automatically resolved when using `iosX64()` in Kotlin Multiplatform
**Platform**: iOS Simulator on Intel Macs

---

## File Count Summary

```
Total artifacts: ~170 files

By type:
- Main artifacts (JAR, AAR, KLIB, POM, MODULE, JSON): 29 files
- GPG signatures (.asc): 29 files
- MD5 checksums (.md5): 29 files
- SHA1 checksums (.sha1): 29 files
- SHA256 checksums (.sha256): 29 files
- SHA512 checksums (.sha512): 29 files
- Maven metadata (maven-metadata.xml + checksums + signatures): ~20 files

By publication:
- krelay (base): ~40 files
- krelay-android: ~30 files
- krelay-iosarm64: ~30 files
- krelay-iossimulatorarm64: ~30 files
- krelay-iosx64: ~30 files
- Root metadata: ~10 files
```

---

## Verification

All artifacts have been verified:

✅ POM files are valid with all required metadata
✅ All artifacts have GPG signatures (.asc files)
✅ All artifacts have checksums (MD5, SHA1, SHA256, SHA512)
✅ Bundle ZIP created and tested
✅ File sizes are consistent
✅ Source JARs contain expected files

---

## Upload to Maven Central

### Bundle File

**File**: `krelay-1.0.1-bundle.zip`
**Size**: 1.2MB
**Location**: Project root directory

The bundle contains all 5 publications with:
- All main artifacts
- All checksums
- All GPG signatures
- All metadata files

### Upload Process

See detailed instructions in: `MANUAL_UPLOAD_INSTRUCTIONS_v1.0.1.md`

Quick steps:
1. Login to https://s01.oss.sonatype.org/
2. Upload `krelay-1.0.1-bundle.zip` via "Staging Upload"
3. Find staging repository (search for "brewkits")
4. Close repository (triggers validation)
5. Release repository (publishes to Maven Central)
6. Wait 2-24 hours for sync

---

## After Publication

After successful publication to Maven Central, users can add KRelay to their projects:

### Gradle Kotlin DSL

```kotlin
repositories {
    mavenCentral()
}

kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    
    sourceSets {
        commonMain.dependencies {
            implementation("dev.brewkits:krelay:1.0.1")
        }
    }
}
```

### Platform-Specific Dependencies

Users generally don't need to declare platform-specific dependencies. Gradle will automatically resolve:
- `krelay-android` for Android targets
- `krelay-iosarm64` for iOS ARM64 targets
- `krelay-iossimulatorarm64` for iOS Simulator ARM64 targets
- `krelay-iosx64` for iOS X64 targets

### Verification

Check artifacts are available:
```
https://repo1.maven.org/maven2/dev/brewkits/krelay/1.0.1/
https://repo1.maven.org/maven2/dev/brewkits/krelay-android/1.0.1/
https://repo1.maven.org/maven2/dev/brewkits/krelay-iosarm64/1.0.1/
https://repo1.maven.org/maven2/dev/brewkits/krelay-iossimulatorarm64/1.0.1/
https://repo1.maven.org/maven2/dev/brewkits/krelay-iosx64/1.0.1/
```

Search on Maven Central:
```
https://search.maven.org/search?q=g:dev.brewkits%20AND%20a:krelay
```

---

## Changes from v1.0.0

### Added
- ✅ Separate publication for `krelay-iosarm64`
- ✅ Separate publication for `krelay-iossimulatorarm64`
- ✅ Separate publication for `krelay-iosx64`

### Benefits
- 🚀 Faster builds (platform-specific dependencies only)
- 📦 Cleaner dependency resolution
- 🎯 Better IDE support
- 💾 Smaller download sizes per platform

### Migration

Projects using v1.0.0 can upgrade to v1.0.1 with zero code changes:

```kotlin
// Just update version number
implementation("dev.brewkits:krelay:1.0.1")
```

Gradle will automatically resolve the correct platform artifacts.

---

## Summary

**Status**: ✅ All Maven artifacts successfully generated for v1.0.1

**Generated**:
- 5 publications (base + android + 3 iOS platforms)
- ~170 total files (artifacts + checksums + signatures + metadata)
- 1.2MB bundle ZIP ready for upload

**Ready for**:
- ✅ Upload to Maven Central (Sonatype OSSRH)
- ✅ Public release

**Next Steps**:
1. Upload bundle to Sonatype OSSRH
2. Close and release staging repository
3. Wait for Maven Central sync (2-24 hours)
4. Update documentation
5. Announce release

---

**For detailed publishing instructions, see**: `MANUAL_UPLOAD_INSTRUCTIONS_v1.0.1.md`

**For checksums, see**: `CHECKSUMS_v1.0.1.txt`

**GitHub Release**: https://github.com/brewkits/KRelay/releases/tag/v1.0.1
