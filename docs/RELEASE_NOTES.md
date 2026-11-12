# Release Notes — JARVIS AI v1.0.0-jarvis (Corrected Android Gradle Structure)

- Fixed ZIP archive structure: all files sit at their standard root-relative Android Gradle locations with explicit ZIP directory entries (`drwxr-xr-x`).
- Added complete Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`).
- Documented honest status of `LiteRtLmBridge.kt`: Local Gemma 3 and Qwen 3.5 are architectural adapter stubs only (no native LiteRT-LM JNI binary or `.litertlm` weights are bundled in the source repo); Gemini 2.5 Flash REST API and the Offline Deterministic Bilingual Rule Engine are fully implemented.
