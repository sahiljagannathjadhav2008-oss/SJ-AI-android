# SJ AI Phase 1 — Model validation plan

## Scope
Phase 1 validates that the pinned Qwen3-0.6B Q4_K_M GGUF file is retrievable, intact, and suitable for a first Android CPU inference prototype. This is not a claim that the model has already been fine-tuned into SJ AI.

## Automated checks
1. Download the exact GGUF file from the pinned Hugging Face repository.
2. Verify SHA-256 against the pinned digest.
3. Verify the GGUF magic header and a conservative minimum file size.
4. Run a Linux CPU inference smoke test with llama.cpp and verify the arithmetic result.
5. Bundle the verified file into the Android release APK.
6. Compile the Android ARM64 JNI runtime against the pinned llama.cpp revision.

## Manual device smoke test
- Install the release APK on an ARM64 Android device.
- Confirm the model loading status changes to `READY`.
- Ask: `What is 17 multiplied by 19? Explain briefly.` Expected core answer: `323`.
- Ask: `भारत की राजधानी क्या है?` Expected core answer: `नई दिल्ली`.
- Ask: `Explain what a variable is in programming.` Expected a concise explanation.
- Enable airplane mode and repeat the questions. The app must continue working.
- Record cold-start time, first-token latency, generation speed, peak RAM, and device temperature.

## Device constraints
The initial runtime uses CPU inference, 2 threads, and a 1024-token context. A 0.6B Q4 model may still be slow on entry-level CPUs. If the app is killed or overheats, reduce context/output tokens and benchmark again. Do not assume 4 GB RAM guarantees smooth inference.

## Acceptance criteria
- APK installs on Android 8.0+ ARM64.
- Model checksum and GGUF header pass in CI.
- Model loads without network access after installation.
- Three smoke prompts return non-empty output.
- No app crash during a 5-turn conversation.

## Known limitation
The current prototype uses greedy decoding and a simple ChatML prompt. It does not yet implement streaming tokens, persistent conversation history, custom SJ AI fine-tuning, safety evaluation, or a full model benchmark suite.
