package com.sahiljadhav.sjai

/** Thin JNI boundary to llama.cpp. All model work runs off the UI thread. */
object NativeBridge {
    init { System.loadLibrary("sjai-jni") }
    external fun nativeLoadModel(modelPath: String, contextSize: Int, threads: Int): String
    external fun nativeGenerate(prompt: String, maxTokens: Int): String
    external fun nativeRelease()
}
