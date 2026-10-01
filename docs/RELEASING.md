# Releasing KRelay

1. Bump `version` in all five modules (`krelay`, `krelay-flow`, `krelay-compose`, `krelay-testing`, `krelay-bom`), update `CHANGELOG.md`, merge to `main`.
2. Tag: `git tag -a vX.Y.Z -m "KRelay X.Y.Z" && git push origin vX.Y.Z`. The **Release** workflow tests, builds and publishes.

## Maven bundle script

`scripts/build-maven-bundle.sh` signs and gathers **all** modules into one zip, so Maven Central gets
a single deployment containing every artifact.

```bash
scripts/build-maven-bundle.sh                    # build + verify + zip  ->  krelay-vX.Y.Z-maven-bundle.zip
scripts/build-maven-bundle.sh --skip-build       # reuse build/maven-central-staging
scripts/build-maven-bundle.sh --only-missing     # zip only what is not on Maven Central yet
CENTRAL_USERNAME=... CENTRAL_PASSWORD=... \
  scripts/build-maven-bundle.sh --upload manual  # upload via the Central Portal API (manual = you click Publish)
```

- Signing key: `signing.key` / `signing.password` in `~/.gradle/gradle.properties`, or `SIGNING_KEY` / `SIGNING_PASSWORD`.
- `CENTRAL_*` are a Central Portal **user token**. In CI they come from the `OSSRH_USERNAME` / `OSSRH_PASSWORD` secrets of the `maven-central` environment.
- The script refuses to run if module versions differ and fails if any artifact lacks a signature or checksum.
- After publishing, confirm all artifacts resolve: `krelay`, `krelay-flow`, `krelay-compose`, `krelay-testing`, `krelay-bom` and their `-jvm`, `-android`, `-ios*`, `-wasm-js` variants.
