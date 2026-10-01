#!/usr/bin/env bash
#
# Builds ONE Maven Central bundle (zip) containing every KRelay module, signed and checksummed.
#
#   scripts/build-maven-bundle.sh                 # build + verify + zip everything
#   scripts/build-maven-bundle.sh --skip-build    # reuse build/maven-central-staging
#   scripts/build-maven-bundle.sh --only-missing  # zip only artifacts not yet on Maven Central
#   scripts/build-maven-bundle.sh --upload manual|automatic
#                                                 # also upload via the Central Portal API
#                                                 # (needs CENTRAL_USERNAME / CENTRAL_PASSWORD = Portal user token)
#
# Signing: ~/.gradle/gradle.properties (signing.key / signing.password) or SIGNING_KEY / SIGNING_PASSWORD.
# Output:  krelay-v<version>-maven-bundle.zip in the repository root (git-ignored).
#
# Why one bundle: publishing modules in separate Gradle runs left some artifacts out of the
# Central deployment for 2.3.0. A single zip is one deployment, validated and published together.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SKIP_BUILD=0
ONLY_MISSING=0
UPLOAD=""
while [ $# -gt 0 ]; do
  case "$1" in
    --skip-build) SKIP_BUILD=1 ;;
    --only-missing) ONLY_MISSING=1 ;;
    --upload) UPLOAD="${2:?--upload needs 'manual' or 'automatic'}"; shift ;;
    -h|--help) sed -n '2,17p' "$0"; exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
  shift
done

GROUP_PATH="dev/brewkits"
MODULES=(krelay krelay-flow krelay-compose krelay-testing krelay-bom)
STAGING="$ROOT/build/maven-central-staging"

VERSION="$(sed -n 's/^version = "\(.*\)"/\1/p' krelay/build.gradle.kts | head -1)"
[ -n "$VERSION" ] || { echo "Cannot read version from krelay/build.gradle.kts" >&2; exit 1; }
for m in "${MODULES[@]}"; do
  v="$(sed -n 's/^version = "\(.*\)"/\1/p' "$m/build.gradle.kts" | head -1)"
  [ "$v" = "$VERSION" ] || { echo "Version mismatch: $m is $v, krelay is $VERSION" >&2; exit 1; }
done
echo "==> KRelay $VERSION"

# 1. Build + sign every module into the local staging repository
if [ "$SKIP_BUILD" -eq 0 ]; then
  rm -rf "$STAGING"
  TASKS=()
  for m in "${MODULES[@]}"; do TASKS+=(":$m:publishAllPublicationsToMavenCentralLocalRepository"); done
  ./gradlew "${TASKS[@]}" --no-daemon -q
fi
[ -d "$STAGING/$GROUP_PATH" ] || { echo "No staged artifacts in $STAGING" >&2; exit 1; }

# 2. Verify: every artifact file is signed and has checksums; filter by version
bad=0
while IFS= read -r f; do
  [ -f "$f.asc" ] && [ -f "$f.md5" ] && [ -f "$f.sha1" ] || { echo "Missing signature/checksum: $f" >&2; bad=1; }
done < <(find "$STAGING/$GROUP_PATH" -type f ! -name '*.asc' ! -name '*.md5' ! -name '*.sha*' ! -name 'maven-metadata*')
[ "$bad" -eq 0 ] || exit 1

# 3. Select artifacts for the bundle
WORK="$ROOT/build/maven-bundle-work"
rm -rf "$WORK"; mkdir -p "$WORK/$GROUP_PATH"
count=0
for dir in "$STAGING/$GROUP_PATH"/*/; do
  artifact="$(basename "$dir")"
  [ -d "$dir$VERSION" ] || continue
  if [ "$ONLY_MISSING" -eq 1 ]; then
    code="$(curl -s -o /dev/null -w '%{http_code}' "https://repo1.maven.org/maven2/$GROUP_PATH/$artifact/$VERSION/$artifact-$VERSION.pom")"
    if [ "$code" = "200" ]; then echo "    skip (already on Central): $artifact"; continue; fi
  fi
  mkdir -p "$WORK/$GROUP_PATH/$artifact"
  cp -R "$dir$VERSION" "$WORK/$GROUP_PATH/$artifact/"
  count=$((count + 1))
done
[ "$count" -gt 0 ] || { echo "Nothing to bundle." >&2; exit 1; }

# 4. Zip with the Maven layout at the archive root
SUFFIX=""; [ "$ONLY_MISSING" -eq 1 ] && SUFFIX="-missing"
ZIP="$ROOT/krelay-v$VERSION$SUFFIX-maven-bundle.zip"
rm -f "$ZIP"
( cd "$WORK" && zip -qr "$ZIP" dev )
rm -rf "$WORK"
echo "==> $count artifact(s) -> $ZIP ($(du -h "$ZIP" | cut -f1))"

# 5. Optional upload to the Central Portal (one deployment)
if [ -n "$UPLOAD" ]; then
  case "$UPLOAD" in
    manual) TYPE=USER_MANAGED ;;
    automatic) TYPE=AUTOMATIC ;;
    *) echo "--upload must be 'manual' or 'automatic'" >&2; exit 2 ;;
  esac
  : "${CENTRAL_USERNAME:?set CENTRAL_USERNAME (Portal user token name)}"
  : "${CENTRAL_PASSWORD:?set CENTRAL_PASSWORD (Portal user token password)}"
  TOKEN="$(printf '%s:%s' "$CENTRAL_USERNAME" "$CENTRAL_PASSWORD" | base64)"
  NAME="$(basename "$ZIP" .zip)"
  ID="$(curl -sS --fail-with-body -X POST \
        -H "Authorization: Bearer $TOKEN" \
        -F "bundle=@$ZIP" \
        "https://central.sonatype.com/api/v1/publisher/upload?name=$NAME&publishingType=$TYPE")"
  echo "==> Uploaded. Deployment id: $ID"
  echo "    Status: curl -X POST -H 'Authorization: Bearer <token>' 'https://central.sonatype.com/api/v1/publisher/status?id=$ID'"
fi
