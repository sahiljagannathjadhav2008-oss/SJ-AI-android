# SJ AI — Android Phase 1

**Developer:** Sahil Jadhav  
**Goal:** A compact, offline-first Android assistant prototype.

## What this phase delivers
- Android app shell with a black/cyan cyber interface.
- ARM64 native inference integration using llama.cpp.
- Pinned Qwen3-0.6B Q4_K_M GGUF base model, bundled into the release APK by GitHub Actions.
- CI model integrity check (SHA-256 + GGUF header).
- Release APK artifact from GitHub Actions.

> Important: this is Phase 1, not a newly trained foundation model. The base weights are from Qwen3. SJ-specific fine-tuning and evaluation are later phases. Model capability will be far below GPT-6.

## Model and license
Base model: `Qwen3-0.6B` in GGUF Q4_K_M format, distributed by QuantFactory. The source model is published under Apache-2.0; preserve the upstream license and attribution. Model file: [QuantFactory/Qwen3-0.6B-GGUF](https://huggingface.co/QuantFactory/Qwen3-0.6B-GGUF). The exact file and SHA-256 are pinned in `.github/workflows/android.yml`.

llama.cpp is included at build time from the upstream `b5214` tag. Review its license and notices before distributing builds.

## Build in GitHub
1. Create a GitHub repository named `sj-ai-android`.
2. Upload all files in this project to the repository root.
3. Open **Actions → SJ AI Android Build → Run workflow** (or push to `main`).
4. Wait for `model-validation` and `android-build` jobs.
5. Download the `SJ-AI-Android-Release` artifact.

The workflow downloads the ~484 MB model during CI and bundles it in the APK. The model binary is intentionally excluded from Git history.

## Local build requirements
- JDK 17
- Android SDK 35
- Android NDK 27.0.12077973
- CMake 3.22.1
- Gradle 8.9

Run `gradle assembleDebug` or `gradle assembleRelease` after placing `sjai-model.gguf` in `app/src/main/assets/`.

## Device notes
- ARM64 (`arm64-v8a`) only in this prototype.
- Android 8.0+.
- First launch copies the bundled GGUF into app-private storage, so it temporarily needs roughly another model-sized amount of free storage.
- The app has no INTERNET permission; inference is local.
- Entry-level CPUs may generate slowly. Start with short prompts and keep other apps closed.

## Project layout
```
app/src/main/java/...       Compose UI + JNI bridge
app/src/main/cpp/           llama.cpp CMake + JNI inference
.github/workflows/          model verification + Android release build
scripts/                    GGUF integrity checks
docs/                       Phase 1 device test plan
```
