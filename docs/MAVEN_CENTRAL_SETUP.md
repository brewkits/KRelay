# Maven Central Publishing Setup Guide

This guide will walk you through setting up Maven Central publishing for KRelay.

## Prerequisites

1. **Sonatype Account**
   - Create account at: https://issues.sonatype.org/
   - Request access to `dev.brewkits` group ID
   - Wait for approval (usually 1-2 business days)

2. **GPG Key for Signing**
   - You need a GPG key to sign your artifacts
   - Maven Central requires all artifacts to be signed

## Step 1: Create Sonatype JIRA Account

1. Go to https://issues.sonatype.org/
2. Click "Sign up" in the top right
3. Fill in your details and create account
4. Verify your email address

## Step 2: Request Group ID Access

1. Create a new issue at: https://issues.sonatype.org/secure/CreateIssue.jspa?issuetype=21&pid=10134
2. Fill in the form:
   - **Summary**: `Request publishing access for dev.brewkits`
   - **Group Id**: `dev.brewkits`
   - **Project URL**: `https://github.com/brewkits/KRelay`
   - **SCM URL**: `https://github.com/brewkits/KRelay.git`
   - **Description**:
     ```
     I am the owner of the brewkits organization on GitHub.
     I would like to publish KRelay and other BrewKits libraries to Maven Central.
     ```

3. Wait for approval (usually 1-2 business days)
4. You'll receive email notification when approved

## Step 3: Generate GPG Key

### Option A: Using GPG Command Line (Recommended)

1. Install GPG:
   ```bash
   # macOS
   brew install gnupg

   # Linux
   sudo apt-get install gnupg
   ```

2. Generate a key pair:
   ```bash
   gpg --full-generate-key
   ```

   - Select: `(1) RSA and RSA`
   - Key size: `4096`
   - Expiration: `0` (never expires) or `2y` (2 years)
   - Enter your name and email
   - Enter a passphrase (REMEMBER THIS!)

3. List your keys:
   ```bash
   gpg --list-secret-keys --keyid-format LONG
   ```

   Output:
   ```
   sec   rsa4096/ABCD1234EFGH5678 2024-01-01 [SC]
   ```

   The key ID is: `ABCD1234EFGH5678`

4. Export your public key to a keyserver:
   ```bash
   gpg --keyserver keyserver.ubuntu.com --send-keys ABCD1234EFGH5678
   ```

   Also upload to other keyservers:
   ```bash
   gpg --keyserver keys.openpgp.org --send-keys ABCD1234EFGH5678
   gpg --keyserver pgp.mit.edu --send-keys ABCD1234EFGH5678
   ```

5. Export your private key for Gradle (Base64 encoded):
   ```bash
   gpg --export-secret-keys ABCD1234EFGH5678 | base64 > ~/gpg-key.txt
   ```

   This creates a base64-encoded key file at `~/gpg-key.txt`

### Option B: Using GPG Suite (macOS GUI)

1. Download GPG Suite: https://gpgtools.org/
2. Install and open GPG Keychain
3. Click "New" to create a new key pair
4. Enter your name and email
5. Set a strong passphrase
6. Right-click your key → "Send public key to Key Server"
7. Export private key and convert to base64 as shown above

## Step 4: Configure Gradle Properties

Create or edit `~/.gradle/gradle.properties`:

```properties
# Sonatype OSSRH credentials
ossrhUsername=your_sonatype_username
ossrhPassword=your_sonatype_password

# GPG Signing (choose ONE method)

# Method 1: In-memory key (RECOMMENDED for CI/CD and local)
# Copy the entire content from ~/gpg-key.txt (should be one long base64 string)
signing.key=LS0tLS1CRUdJTiBQR1AgUFJJVkFURSBLRVkgQkxPQ0stLS0tLQp...very...long...base64...string...
signing.password=your_gpg_passphrase

# Method 2: GPG Keyring (traditional method)
# signing.keyId=ABCD1234EFGH5678
# signing.password=your_gpg_passphrase
# signing.secretKeyRingFile=/Users/username/.gnupg/secring.gpg
```

**Important Notes:**
- The `signing.key` should be ONE continuous line (no line breaks)
- Do NOT commit this file to git (it contains secrets!)
- For `signing.key`, paste the entire base64 string from `~/gpg-key.txt`

## Step 5: Test Publishing Locally

Before publishing to Maven Central, test locally:

```bash
# Build the library
./gradlew :krelay:build

# Publish to local staging
./gradlew :krelay:publishAllPublicationsToMavenCentralLocalRepository

# Check artifacts
ls -R krelay/build/maven-central-staging/
```

You should see:
- `*.jar` files (JVM artifacts)
- `*.aar` files (Android artifacts)
- `*.klib` files (Native artifacts)
- `*.pom` files (Maven metadata)
- `*.asc` files (GPG signatures)

## Step 6: Publish to Maven Central

### Using the Automated Script (Recommended)

```bash
./publish-to-maven-central.sh
```

The script will:
1. Check prerequisites
2. Build and test the library
3. Publish to local staging for verification
4. Ask for confirmation
5. Publish to Maven Central

### Manual Publishing

```bash
# Publish to OSSRH
./gradlew :krelay:publishAllPublicationsToOSSRHRepository
```

## Step 7: Release on Sonatype (RELEASE versions only)

For SNAPSHOT versions, skip this step (they're automatically available).

For RELEASE versions:

1. Login to https://s01.oss.sonatype.org/
2. Click "Staging Repositories" in the left menu
3. Find your repository (e.g., `devbrewkits-1001`)
4. Select it and click "Close" button
5. Wait for validation (2-5 minutes)
6. If validation passes, click "Release" button
7. Confirm the release

## Step 8: Verify Publication

### For SNAPSHOT Versions

Check immediately at:
```
https://s01.oss.sonatype.org/content/repositories/snapshots/dev/brewkits/krelay/
```

Use in your project:
```kotlin
repositories {
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
}

dependencies {
    implementation("dev.brewkits:krelay:1.0.0-SNAPSHOT")
}
```

### For RELEASE Versions

After releasing, artifacts will sync to Maven Central:

- **2-4 hours**: Available on Maven Central search
- **24 hours**: Indexed and fully searchable

Check at:
```
https://repo1.maven.org/maven2/dev/brewkits/krelay/
```

Use in your project:
```kotlin
dependencies {
    implementation("dev.brewkits:krelay:1.0.0")
}
```

## Troubleshooting

### "Unauthorized" Error

**Problem**: Wrong Sonatype credentials

**Solution**:
- Verify `ossrhUsername` and `ossrhPassword` in `~/.gradle/gradle.properties`
- Make sure you're using credentials for https://s01.oss.sonatype.org/ (not the old oss.sonatype.org)

### "Failed to sign" Error

**Problem**: GPG signing configuration issue

**Solution**:
- Verify `signing.key` is one continuous base64 string (no line breaks)
- Verify `signing.password` matches your GPG passphrase
- Test GPG key: `gpg --list-secret-keys`

### "Group ID not verified" Error

**Problem**: You don't have permission for `dev.brewkits` group ID

**Solution**:
- Create a Sonatype JIRA ticket (see Step 2)
- Wait for approval before publishing

### "Invalid POM" Error

**Problem**: Missing required POM metadata

**Solution**:
- Check `krelay/build.gradle.kts` has all required fields:
  - `name`, `description`, `url`
  - `licenses`, `developers`, `scm`

### Artifacts Missing Signature Files

**Problem**: GPG signing not working

**Solution**:
- Make sure signing configuration is correct
- Check local staging: `ls krelay/build/maven-central-staging/`
- Look for `*.asc` files (signatures)

## CI/CD Publishing (GitHub Actions)

To publish from GitHub Actions:

1. Add secrets to your repository:
   - `OSSRH_USERNAME`: Your Sonatype username
   - `OSSRH_PASSWORD`: Your Sonatype password
   - `SIGNING_KEY`: Your base64-encoded GPG private key
   - `SIGNING_PASSWORD`: Your GPG passphrase

2. Create workflow file `.github/workflows/publish.yml`:

```yaml
name: Publish to Maven Central

on:
  push:
    tags:
      - 'v*'

jobs:
  publish:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Publish to Maven Central
        env:
          OSSRH_USERNAME: ${{ secrets.OSSRH_USERNAME }}
          OSSRH_PASSWORD: ${{ secrets.OSSRH_PASSWORD }}
          SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
          SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
        run: ./gradlew :krelay:publishAllPublicationsToOSSRHRepository
```

3. Create a git tag and push:
```bash
git tag v1.0.0
git push origin v1.0.0
```

## Resources

- **Sonatype OSSRH Guide**: https://central.sonatype.org/publish/publish-guide/
- **GPG Signing Guide**: https://central.sonatype.org/publish/requirements/gpg/
- **Gradle Publishing Plugin**: https://docs.gradle.org/current/userguide/publishing_maven.html
- **Kotlin Multiplatform Publishing**: https://kotlinlang.org/docs/multiplatform-publish-lib.html

## Need Help?

- Sonatype JIRA: https://issues.sonatype.org/
- GitHub Issues: https://github.com/brewkits/KRelay/issues
- Email: dev@brewkits.dev
