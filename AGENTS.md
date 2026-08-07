# Permanent & Protected Project Rules

## Application Identity
- **Application ID:** `com.aistudio.rdaphysical.academy.sdwmjf` (LOCKED - NEVER CHANGE)
- **Application Name:** RDA Physical Academy (or as set in resources)

## Protected Infrastructure & Build Files
The following files and configurations are strictly **LOCKED** and **PROTECTED**:
1. `app/build.gradle.kts`:
   - `applicationId`: `com.aistudio.rdaphysical.academy.sdwmjf`
   - Release signing configuration (`signingConfigs.release`) using `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
2. `.github/workflows/android-build.yml`:
   - Keystore decoding from `KEYSTORE_BASE64`
   - Release build and signing verification (`apksigner`)
   - JDK and Gradle configuration
3. `.gitignore`:
   - Keystore files (`*.jks`, `*.keystore`, `keystore_base64.txt`) must remain ignored.

## Permanent Signing Identity Rules
- **NEVER** generate a new keystore automatically or run `keytool` to replace keys.
- **NEVER** change the `KEY_ALIAS` or signing certificate.
- **NEVER** reset or change the `applicationId`.
- **NEVER** commit private keystores, passwords, or secrets to source control.

## Feature Development & Update Safety
- **Minimal Change Policy:** Modify only the files strictly required for a requested feature.
- **Data Safety:** Never delete, alter, or reset Firebase Firestore collections/documents, Authentication schema, or existing data structures.
- **Versioning:** Preserve `versionCode` progression for seamless APK updates.
- **Secret Safety:** Always use environment variables / GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Never print or log secret values.
