#include <jni.h>
#include <string>
#include <vector>
#include <mutex>
#include <algorithm>
#include "llama.h"

static llama_model* g_model = nullptr;
static std::mutex g_mutex;
static int g_context_size = 1024;
static int g_threads = 2;

static std::string fromJString(JNIEnv* env, jstring value) {
    const char* chars = env->GetStringUTFChars(value, nullptr);
    std::string result(chars ? chars : "");
    if (chars) env->ReleaseStringUTFChars(value, chars);
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_sahiljadhav_sjai_NativeBridge_nativeLoadModel(JNIEnv* env, jobject, jstring path, jint contextSize, jint threads) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
    llama_backend_init();
    g_context_size = std::max(512, std::min((int)contextSize, 2048));
    g_threads = std::max(1, std::min((int)threads, 4));
    llama_model_params params = llama_model_default_params();
    params.n_gpu_layers = 0;
    std::string modelPath = fromJString(env, path);
    g_model = llama_model_load_from_file(modelPath.c_str(), params);
    if (!g_model) return env->NewStringUTF("ERROR: model load failed");
    return env->NewStringUTF("READY • Qwen3 0.6B loaded (CPU)");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_sahiljadhav_sjai_NativeBridge_nativeGenerate(JNIEnv* env, jobject, jstring promptValue, jint maxTokens) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_model) return env->NewStringUTF("Model is not loaded.");
    std::string prompt = fromJString(env, promptValue);
    const llama_vocab* vocab = llama_model_get_vocab(g_model);
    std::vector<llama_token> tokens(std::max<size_t>(prompt.size() + 16, 256));
    int n = llama_tokenize(vocab, prompt.c_str(), (int)prompt.size(), tokens.data(), (int)tokens.size(), true, true);
    if (n < 0) {
        tokens.resize((size_t)(-n));
        n = llama_tokenize(vocab, prompt.c_str(), (int)prompt.size(), tokens.data(), (int)tokens.size(), true, true);
    }
    if (n <= 0 || n > 1000) return env->NewStringUTF("ERROR: prompt too long or tokenization failed");
    tokens.resize((size_t)n);
    int limit = std::max(1, std::min((int)maxTokens, 180));
    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = (uint32_t)std::max(g_context_size, n + limit + 8);
    cparams.n_batch = (uint32_t)n;
    cparams.n_threads = g_threads;
    cparams.n_threads_batch = g_threads;
    llama_context* ctx = llama_init_from_model(g_model, cparams);
    if (!ctx) return env->NewStringUTF("ERROR: context creation failed");
    llama_batch batch = llama_batch_get_one(tokens.data(), (int)tokens.size());
    if (llama_decode(ctx, batch) != 0) { llama_free(ctx); return env->NewStringUTF("ERROR: prompt decode failed"); }

    llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_greedy());
    std::string output;
    char piece[512];
    for (int i = 0; i < limit; ++i) {
        llama_token token = llama_sampler_sample(sampler, ctx, -1);
        if (llama_vocab_is_eog(vocab, token)) break;
        int pieceLen = llama_token_to_piece(vocab, token, piece, sizeof(piece), 0, true);
        if (pieceLen > 0) output.append(piece, (size_t)pieceLen);
        llama_sampler_accept(sampler, token);
        llama_batch next = llama_batch_get_one(&token, 1);
        if (llama_decode(ctx, next) != 0) break;
    }
    llama_sampler_free(sampler);
    llama_free(ctx);
    return env->NewStringUTF(output.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_sahiljadhav_sjai_NativeBridge_nativeRelease(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
    llama_backend_free();
}
