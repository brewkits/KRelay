# Manual Upload to Maven Central - Instructions v1.0.1

**Bundle File**: `krelay-1.0.1-bundle.zip` (1.2MB)
**Date**: 2026-01-23
**Version**: 1.0.1

---

## ✅ Preparation Complete

All artifacts have been prepared for manual upload:

- ✅ **29 GPG signatures (.asc)** generated for all main artifacts
- ✅ **116 checksum files** (MD5, SHA1, SHA256, SHA512)
- ✅ **ZIP bundle** created: `krelay-1.0.1-bundle.zip`

### What's New in v1.0.1

This version adds **3 new iOS platform artifacts**:
- `krelay-iosarm64` - iOS devices (ARM64)
- `krelay-iossimulatorarm64` - iOS simulators on M1/M2 Macs (Apple Silicon)
- `krelay-iosx64` - iOS simulators on Intel Macs

Total publications: 5
- krelay (base/metadata)
- krelay-android (Android)
- krelay-iosarm64 (NEW)
- krelay-iossimulatorarm64 (NEW)
- krelay-iosx64 (NEW)

Bundle contains:
- Android library (AAR) + sources + metadata
- Multiplatform library (JAR) + sources + metadata
- iOS ARM64 native library (KLIB) + sources + metadata
- iOS Simulator ARM64 native library (KLIB) + sources + metadata
- iOS X64 native library (KLIB) + sources + metadata
- POM files with complete metadata
- Gradle module files
- All checksums and GPG signatures

---

## Step-by-Step Upload Process

### Step 1: Login to Sonatype OSSRH

1. Open browser and go to: **https://s01.oss.sonatype.org/**
2. Click **"Log In"** (top right)
3. Enter your Sonatype credentials:
   - Username: (your OSSRH username)
   - Password: (your OSSRH password)

**Note**: This is the NEW Sonatype instance (s01.oss.sonatype.org), not the old oss.sonatype.org

---

### Step 2: Upload Bundle

1. In the left sidebar, click **"Staging Upload"**
2. Select **"Upload Mode: Artifact Bundle"**
3. Click **"Select Bundle to Upload"**
4. Choose the file: `krelay-1.0.1-bundle.zip`
5. Click **"Upload Bundle"**
6. Wait for upload to complete (may take 30-60 seconds for 1.2MB)

**Expected result**: You should see "Upload successful" message

---

### Step 3: Find Your Staging Repository

1. In the left sidebar, click **"Staging Repositories"**
2. In the search box (top right), type: **"brewkits"**
3. Look for a repository named something like:
   - `devbrewkits-1001`
   - `devbrewkits-1002`
   - etc. (the number increments each time)
4. Click on your repository to select it

**Expected content**:
- Should show `dev/brewkits/krelay/1.0.1/` structure
- Should show `dev/brewkits/krelay-android/1.0.1/` structure
- Should show `dev/brewkits/krelay-iosarm64/1.0.1/` structure
- Should show `dev/brewkits/krelay-iossimulatorarm64/1.0.1/` structure
- Should show `dev/brewkits/krelay-iosx64/1.0.1/` structure

---

### Step 4: Close the Staging Repository

**What "Close" means**: Sonatype will validate your artifacts against Maven Central requirements

1. With your repository selected, click the **"Close"** button (top toolbar)
2. A dialog will appear asking for a description
   - Enter: `KRelay v1.0.1 - Add iOS platform artifacts`
3. Click **"Confirm"**
4. Wait for validation to complete (2-5 minutes)

**Validation checks**:
- ✓ POM file is valid (name, description, URL, licenses, developers, SCM)
- ✓ All artifacts have GPG signatures (.asc files)
- ✓ All artifacts have checksums (MD5, SHA1)
- ✓ Group ID `dev.brewkits` is authorized for your account
- ✓ All required files are present

**During validation**:
- The "Activity" tab (bottom panel) will show progress
- You'll see rules being executed (Checksum Validation, Signature Validation, etc.)
- Status will change from "Open" → "Closing" → "Closed"

---

### Step 5: Check Validation Results

**If validation SUCCEEDS**:
- ✅ Status changes to **"Closed"**
- ✅ The "Release" button becomes enabled
- ✅ Continue to Step 6

**If validation FAILS**:
- ❌ Status changes to **"Failed"** or returns to "Open"
- ❌ Click the "Activity" tab to see error details
- ❌ Common errors and fixes below

---

### Step 6: Release to Maven Central

**Important**: This step is IRREVERSIBLE. Once released, you cannot delete or modify the artifacts.

1. With your **"Closed"** repository selected, click the **"Release"** button (top toolbar)
2. A dialog will appear asking for confirmation
   - Description: `Release KRelay v1.0.1 to Maven Central - iOS platform support`
3. Check the option: **"Automatically Drop"** (recommended)
   - This will delete the staging repository after successful release
4. Click **"Confirm"**

**What happens next**:
- Artifacts are moved to Maven Central sync queue
- Staging repository is dropped (deleted)
- You'll see "Release in progress" in Activity tab

---

### Step 7: Wait for Maven Central Sync

Maven Central sync is NOT instant:

**Timeline**:
- ⏱️ **2-4 hours**: Artifacts appear on Maven Central
- ⏱️ **4-24 hours**: Fully indexed and searchable
- ⏱️ **24-48 hours**: Available in all mirrors worldwide

**Progress tracking**:
- After 2 hours, check: https://repo1.maven.org/maven2/dev/brewkits/krelay/1.0.1/
- After 4 hours, search: https://search.maven.org/search?q=g:dev.brewkits
- After 24 hours, should appear in IDE auto-completion

**Check iOS artifacts are available**:
- https://repo1.maven.org/maven2/dev/brewkits/krelay-iosarm64/1.0.1/
- https://repo1.maven.org/maven2/dev/brewkits/krelay-iossimulatorarm64/1.0.1/
- https://repo1.maven.org/maven2/dev/brewkits/krelay-iosx64/1.0.1/

---

## Verification Steps

### Immediate Verification (After Release)

Check that release was successful in Sonatype:
1. Go to **"Staging Repositories"**
2. Your repository should be gone (if "Automatically Drop" was checked)
3. No error messages in Activity tab

### 2-4 Hours After Release

Check Maven Central repository:
```
https://repo1.maven.org/maven2/dev/brewkits/krelay/1.0.1/
https://repo1.maven.org/maven2/dev/brewkits/krelay-android/1.0.1/
https://repo1.maven.org/maven2/dev/brewkits/krelay-iosarm64/1.0.1/
https://repo1.maven.org/maven2/dev/brewkits/krelay-iossimulatorarm64/1.0.1/
https://repo1.maven.org/maven2/dev/brewkits/krelay-iosx64/1.0.1/
```

Expected files for each iOS target:
- `krelay-iosarm64-1.0.1.klib`
- `krelay-iosarm64-1.0.1.pom`
- `krelay-iosarm64-1.0.1.module`
- `krelay-iosarm64-1.0.1-sources.jar`
- etc. (with checksums and signatures)

### 4-24 Hours After Release

Check Maven Central search:
```
https://search.maven.org/search?q=g:dev.brewkits%20AND%20a:krelay
```

Should show: **KRelay 1.0.1** with all 5 artifacts

### Test in a Project

Create a new Gradle project and add dependencies:

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
}

kotlin {
    // Android
    androidTarget()
    
    // iOS
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

Run: `./gradlew dependencies`

Expected: KRelay 1.0.1 with all platform artifacts should download successfully

---

## Troubleshooting

### Upload Failed

**Symptoms**: Error message during bundle upload

**Solutions**:
- Check internet connection
- Verify file is not corrupted: `unzip -t krelay-1.0.1-bundle.zip`
- Try uploading again
- Try different browser

### Close/Validation Stuck

**Symptoms**: Status stays "Closing" for more than 10 minutes

**Solutions**:
- Refresh the page
- Click "Refresh" button in toolbar
- Check Sonatype status: https://status.maven.org/
- Wait and try again later

### Can't Find Staging Repository

**Symptoms**: No repository appears after upload

**Solutions**:
- Click "Refresh" button
- Make sure you're searching for "brewkits" (not "krelay")
- Check you're logged in with correct account
- Try uploading bundle again

### Release Button Disabled

**Symptoms**: Can't click "Release" button

**Solutions**:
- Repository must be "Closed" first
- Validation must pass completely
- Refresh the page
- Select the repository again

### Artifacts Not Appearing on Maven Central

**Symptoms**: 24 hours passed, still not on Maven Central

**Solutions**:
- Check https://repo1.maven.org/maven2/dev/brewkits/krelay/ (should show 1.0.1 folder)
- If folder exists but search doesn't find it, wait more (indexing delay)
- If folder doesn't exist, check Sonatype Activity log for errors
- Contact Sonatype support: https://issues.sonatype.org/

---

## After Successful Publication

### 1. Update README.md

Update installation instructions to use v1.0.1:
```kotlin
dependencies {
    implementation("dev.brewkits:krelay:1.0.1")
}
```

Add iOS platform support note:
```markdown
## Platform Support

- ✅ Android (JVM/Kotlin)
- ✅ iOS ARM64 (devices)
- ✅ iOS Simulator ARM64 (M1/M2 Macs)
- ✅ iOS Simulator X64 (Intel Macs)
```

### 2. Update ROADMAP.md

Mark Phase 1 tasks complete:
- [x] Publish v1.0.0 to Maven Central
- [x] Publish v1.0.1 with iOS platform artifacts

### 3. Create GitHub Release

Create release tag for v1.0.1:
```bash
git tag -a v1.0.1 -m "Release v1.0.1 - iOS platform artifacts"
git push origin v1.0.1
```

Create GitHub release with notes:
```markdown
## KRelay v1.0.1 - iOS Platform Support

This release adds separate Maven artifacts for iOS platforms to improve dependency resolution and build performance.

### New Artifacts

- `krelay-iosarm64` - iOS devices (ARM64)
- `krelay-iossimulatorarm64` - iOS simulators on M1/M2 Macs
- `krelay-iosx64` - iOS simulators on Intel Macs

### Installation

```kotlin
dependencies {
    implementation("dev.brewkits:krelay:1.0.1")
}
```

### Full Changelog

- Add separate iOS platform publications for better dependency management
- Maintain backward compatibility with v1.0.0

---

Available on Maven Central: https://search.maven.org/artifact/dev.brewkits/krelay/1.0.1/jar
```

### 4. Announce Release

Post announcements on:
- [ ] Reddit r/Kotlin
- [ ] Kotlin Slack #multiplatform
- [ ] Twitter/X (with #KotlinMultiplatform hashtag)
- [ ] LinkedIn
- [ ] Dev.to or Medium blog post

Example announcement:
```
📦 KRelay v1.0.1 now available on Maven Central!

This update adds dedicated iOS platform artifacts for better dependency resolution:
✅ krelay-iosarm64 (iOS devices)
✅ krelay-iossimulatorarm64 (M1/M2 Mac simulators)
✅ krelay-iosx64 (Intel Mac simulators)

implementation("dev.brewkits:krelay:1.0.1")

The Glue Code Standard for Kotlin Multiplatform - Safe, leak-free bridge between shared code and platform APIs.

GitHub: https://github.com/brewkits/KRelay
Docs: https://github.com/brewkits/KRelay#readme

#KotlinMultiplatform #KMP #AndroidDev #iOSDev
```

---

## Support

If you encounter issues:

1. **Sonatype Support**: https://issues.sonatype.org/
2. **GitHub Issues**: https://github.com/brewkits/KRelay/issues
3. **Email**: dev@brewkits.dev

---

## Quick Reference

**Bundle file**: `krelay-1.0.1-bundle.zip` (1.2MB)
**Upload URL**: https://s01.oss.sonatype.org/
**Maven Central**: https://repo1.maven.org/maven2/dev/brewkits/
**Search**: https://search.maven.org/search?q=g:dev.brewkits

**Credentials needed**:
- Sonatype username/password
- GPG key (already used for signing)

**Artifacts**:
- krelay (base)
- krelay-android (Android)
- krelay-iosarm64 (iOS ARM64) - NEW
- krelay-iossimulatorarm64 (iOS Simulator ARM64) - NEW
- krelay-iosx64 (iOS X64) - NEW

---

**Good luck with your v1.0.1 release! 🚀**
