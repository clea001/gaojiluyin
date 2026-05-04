#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include "whisper.h"

#define TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_gaojiluyin_whisper_WhisperEngine_nativeInit(
        JNIEnv *env, jobject /* this */, jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading model from: %s", path);

    struct whisper_context_params params = whisper_context_default_params();
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, params);

    env->ReleaseStringUTFChars(modelPath, path);

    if (ctx == nullptr) {
        LOGE("Failed to load model");
        return 0;
    }

    LOGI("Model loaded successfully");
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT jstring JNICALL
Java_com_gaojiluyin_whisper_WhisperEngine_nativeTranscribe(
        JNIEnv *env, jobject /* this */, jlong contextPtr, jfloatArray audioData) {
    auto *ctx = reinterpret_cast<whisper_context *>(contextPtr);
    if (ctx == nullptr) {
        LOGE("Context is null");
        return env->NewStringUTF("");
    }

    jsize len = env->GetArrayLength(audioData);
    jfloat *data = env->GetFloatArrayElements(audioData, nullptr);

    LOGI("Transcribing %d samples", len);

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_progress = false;
    params.print_special = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.translate = false;
    params.language = "zh";
    params.n_threads = 4;
    params.no_context = true;
    params.single_segment = false;

    int result = whisper_full(ctx, params, data, len);

    env->ReleaseFloatArrayElements(audioData, data, JNI_ABORT);

    if (result != 0) {
        LOGE("whisper_full failed with code %d", result);
        return env->NewStringUTF("");
    }

    int n_segments = whisper_full_n_segments(ctx);
    std::string full_text;

    for (int i = 0; i < n_segments; i++) {
        const char *text = whisper_full_get_segment_text(ctx, i);
        if (text != nullptr) {
            full_text += text;
        }
    }

    LOGI("Transcription complete: %zu chars", full_text.size());
    return env->NewStringUTF(full_text.c_str());
}

JNIEXPORT void JNICALL
Java_com_gaojiluyin_whisper_WhisperEngine_nativeFree(
        JNIEnv *env, jobject /* this */, jlong contextPtr) {
    auto *ctx = reinterpret_cast<whisper_context *>(contextPtr);
    if (ctx != nullptr) {
        whisper_free(ctx);
        LOGI("Context freed");
    }
}

} // extern "C"
