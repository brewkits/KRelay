#!/bin/bash

# KRelay - Maven Central Publishing Script
# Automated script to publish to Maven Central (Sonatype OSSRH)

set -e  # Exit on error

echo "🚀 KRelay - Maven Central Publishing Script"
echo "============================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check prerequisites
echo "📋 Checking prerequisites..."

# Check if credentials exist
if [ ! -f "$HOME/.gradle/gradle.properties" ]; then
    echo -e "${RED}❌ Error: ~/.gradle/gradle.properties not found${NC}"
    echo ""
    echo "Please create ~/.gradle/gradle.properties with:"
    echo ""
    echo "ossrhUsername=your_sonatype_username"
    echo "ossrhPassword=your_sonatype_password"
    echo "signing.key=<BASE64_GPG_KEY>"
    echo "signing.password=your_gpg_password"
    echo ""
    echo "See docs/MAVEN_CENTRAL_SETUP.md for detailed setup guide"
    exit 1
fi

# Check if GPG signing key is configured
if ! grep -q "signing.key" "$HOME/.gradle/gradle.properties" && ! grep -q "signing.keyId" "$HOME/.gradle/gradle.properties"; then
    echo -e "${YELLOW}⚠️  Warning: No GPG signing key found in gradle.properties${NC}"
    echo "Publishing without signing will fail on Maven Central"
    echo ""
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo -e "${GREEN}✅ Prerequisites check passed${NC}"
echo ""

# Extract version from build.gradle.kts
VERSION=$(grep "^version = " krelay/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/')
echo "📦 Publishing version: ${GREEN}${VERSION}${NC}"
echo ""

# Check if this is a snapshot version
if [[ $VERSION == *"SNAPSHOT"* ]]; then
    echo -e "${BLUE}ℹ️  This is a SNAPSHOT version${NC}"
    echo "Will publish to: https://s01.oss.sonatype.org/content/repositories/snapshots/"
else
    echo -e "${BLUE}ℹ️  This is a RELEASE version${NC}"
    echo "Will publish to: https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
fi
echo ""

read -p "Is this the correct version? (Y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Nn]$ ]]; then
    echo "Please update version in krelay/build.gradle.kts"
    exit 1
fi

# Clean build
echo ""
echo "🧹 Cleaning previous build..."
./gradlew clean

# Build and test
echo ""
echo "🔨 Building library..."
./gradlew :krelay:build

echo ""
echo "✅ Build successful!"

# Publish to local staging first (for verification)
echo ""
echo "📦 Publishing to local staging (for verification)..."
./gradlew :krelay:publishAllPublicationsToMavenCentralLocalRepository

echo ""
echo "✅ Local staging complete"
echo "Artifacts location: krelay/build/maven-central-staging/"

# Verify artifacts exist
STAGING_DIR="krelay/build/maven-central-staging/dev/brewkits/krelay/${VERSION}"
if [ ! -d "$STAGING_DIR" ]; then
    echo -e "${RED}❌ Error: Staging directory not found at $STAGING_DIR${NC}"
    exit 1
fi

# Count artifacts
echo ""
echo "📊 Searching for artifacts..."
JAR_COUNT=$(find "$STAGING_DIR" -name "*.jar" 2>/dev/null | wc -l)
AAR_COUNT=$(find "$STAGING_DIR" -name "*.aar" 2>/dev/null | wc -l)
POM_COUNT=$(find "$STAGING_DIR" -name "*.pom" 2>/dev/null | wc -l)
KLIB_COUNT=$(find "$STAGING_DIR" -name "*.klib" 2>/dev/null | wc -l)

echo ""
echo "📊 Artifact Summary:"
echo "  - JARs: $JAR_COUNT"
echo "  - AARs: $AAR_COUNT"
echo "  - POMs: $POM_COUNT"
echo "  - KLIBs: $KLIB_COUNT"
echo ""
echo "Targets included:"
find "$STAGING_DIR" -type f -name "*.module" -exec basename {} \; | sed 's/krelay-/  - /' | sed 's/.module//' | sort -u

# Final confirmation
echo ""
echo -e "${YELLOW}⚠️  IMPORTANT${NC}"
echo "You are about to publish to Maven Central (Sonatype OSSRH)"

if [[ $VERSION == *"SNAPSHOT"* ]]; then
    echo ""
    echo "SNAPSHOT publishing will:"
    echo "  1. Upload all artifacts to Sonatype snapshot repository"
    echo "  2. Sign artifacts with your GPG key"
    echo "  3. Make them immediately available (no staging/review needed)"
    echo ""
    echo "Snapshot repository: https://s01.oss.sonatype.org/content/repositories/snapshots/"
else
    echo ""
    echo "RELEASE publishing will:"
    echo "  1. Upload all artifacts to Sonatype staging repository"
    echo "  2. Sign artifacts with your GPG key"
    echo "  3. Deploy to staging for review"
    echo ""
    echo "After staging, you need to:"
    echo "  - Login to https://s01.oss.sonatype.org/"
    echo "  - Find your staging repository"
    echo "  - Click 'Close' to verify"
    echo "  - Click 'Release' to publish to Maven Central"
fi
echo ""

read -p "Continue with publishing? (yes/NO) " -r
echo
if [[ ! $REPLY == "yes" ]]; then
    echo "Publishing cancelled"
    exit 0
fi

# Publish to OSSRH
echo ""
echo "🚀 Publishing to Maven Central (OSSRH)..."
echo "This may take several minutes..."
echo ""

if ./gradlew :krelay:publishAllPublicationsToOSSRHRepository; then
    echo ""
    echo -e "${GREEN}✅ Publishing successful!${NC}"
    echo ""

    if [[ $VERSION == *"SNAPSHOT"* ]]; then
        echo "📋 SNAPSHOT Published:"
        echo "Repository: https://s01.oss.sonatype.org/content/repositories/snapshots/"
        echo "Group: dev.brewkits"
        echo "Artifact: krelay"
        echo "Version: ${VERSION}"
        echo ""
        echo "Add to your project:"
        echo ""
        echo "repositories {"
        echo "    maven { url = uri(\"https://s01.oss.sonatype.org/content/repositories/snapshots/\") }"
        echo "}"
        echo ""
        echo "dependencies {"
        echo "    implementation(\"dev.brewkits:krelay:${VERSION}\")"
        echo "}"
    else
        echo "📋 Next Steps:"
        echo "1. Visit https://s01.oss.sonatype.org/"
        echo "2. Login with your Sonatype credentials"
        echo "3. Click 'Staging Repositories' in left menu"
        echo "4. Find 'devbrewkits-XXXX' repository"
        echo "5. Click 'Close' button (wait for validation)"
        echo "6. If validation passes, click 'Release' button"
        echo ""
        echo "⏱️  Sync to Maven Central: ~2-4 hours after release"
        echo "⏱️  Searchable on Maven Central: ~24 hours after sync"
        echo ""
        echo "Once released, add to your project:"
        echo ""
        echo "dependencies {"
        echo "    implementation(\"dev.brewkits:krelay:${VERSION}\")"
        echo "}"
    fi
    echo ""
    echo "🎉 Congratulations!"
else
    echo ""
    echo -e "${RED}❌ Publishing failed${NC}"
    echo ""
    echo "Common issues:"
    echo "- Missing or incorrect signing credentials"
    echo "- Incorrect Sonatype credentials"
    echo "- Group ID 'dev.brewkits' not verified in Sonatype"
    echo "- Network connectivity issues"
    echo ""
    echo "Check logs above for details"
    echo ""
    echo "For help, see: docs/MAVEN_CENTRAL_SETUP.md"
    exit 1
fi
