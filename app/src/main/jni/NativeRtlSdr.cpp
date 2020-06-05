#include <string.h>
#include <android/log.h>
#include <stdlib.h>
#include "rtl-ais/main.h"
#include "NativeRtlSdrUtils.h"
#include "NativeRtlSdr.h"

#ifdef __cplusplus
extern "C" {
#endif

// Android log function wrappers
static const char* kTAG = "jni-NativeRtlSdr";
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, kTAG, __VA_ARGS__))
#define LOGW(...) \
  ((void)__android_log_print(ANDROID_LOG_WARN, kTAG, __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, kTAG, __VA_ARGS__))

typedef struct ships_context {
    JavaVM  *javaVM;
    jclass   jniClz;
    jobject  jniObj;
} ShipsContext;
ShipsContext g_ctx;

/*
 * processing one time initialization:
 *     Cache the javaVM into our context
 *     Find class ID for NativeRtlSdr
 *     Create an instance of NativeRtlSdr
 *     Make global reference since we are using them from a native thread
 * Note:
 *     All resources allocated here are never released by application
 *     we rely on system to free all global refs when it goes away;
 *     the pairing function JNI_OnUnload() never gets called at all.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    memset(&g_ctx, 0, sizeof(g_ctx));

    g_ctx.javaVM = vm;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR; // JNI version not supported.
    }

    jclass clz = env->FindClass("net/videgro/ships/bridge/NativeRtlSdr");
    g_ctx.jniClz = (jclass)env->NewGlobalRef(clz);

    jmethodID jniHelperCtor = env->GetMethodID(g_ctx.jniClz,"<init>", "()V");
    jobject handler = env->NewObject(g_ctx.jniClz,jniHelperCtor);
    g_ctx.jniObj = env->NewGlobalRef(handler);

    return JNI_VERSION_1_6;
}

/********************************************************************** From Native to Java */

void send_exception(const int exception_code) {
    ShipsContext *pctx = &g_ctx;
    JavaVM *javaVM = pctx->javaVM;

    JNIEnv *env;
    jint res=javaVM->AttachCurrentThread( &env, NULL );
    if (JNI_OK != res) {
        LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
        return;
    }

    jmethodID method = env->GetMethodID(pctx->jniClz, "onException", "(I)V");
    if (!method){
        LOGE("Failed to retrieve onException methodID @ line %d",__LINE__);
        return;
    }

    env->CallVoidMethod(pctx->jniObj, method,exception_code);

    //javaVM->DetachCurrentThread();
    return;
}

void send_ready() {
    ShipsContext *pctx = &g_ctx;
    JavaVM *javaVM = pctx->javaVM;

    JNIEnv *env;
    jint res=javaVM->AttachCurrentThread( &env, NULL );
    if (JNI_OK != res) {
        LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
        return;
    }
    jmethodID method = env->GetMethodID(pctx->jniClz, "onReady", "()V");
    if (!method){
        LOGE("Failed to retrieve onReady methodID @ line %d",__LINE__);
        return;
    }

    env->CallVoidMethod(pctx->jniObj, method);

    //javaVM->DetachCurrentThread();
    return;
}

/********************************************************************** From Java to Native */

JNIEXPORT jboolean JNICALL
Java_net_videgro_ships_bridge_NativeRtlSdr_isRunningRtlSdrAis(JNIEnv *env, jobject thiz) {
    return (jboolean) ((rtl_ais_isrunning()) ? (JNI_TRUE) : (JNI_FALSE));
}

JNIEXPORT void JNICALL
Java_net_videgro_ships_bridge_NativeRtlSdr_stopRtlSdrAis(JNIEnv *env, jobject thiz) {
    rtl_ais_close();
}

JNIEXPORT void JNICALL
Java_net_videgro_ships_bridge_NativeRtlSdr_changeRtlSdrPpm(JNIEnv *env, jobject thiz, jint newPpm) {
    rtlsdr_change_ppm(newPpm);
}

JNIEXPORT void JNICALL
Java_net_videgro_ships_bridge_NativeRtlSdr_startRtlSdrAis(JNIEnv *env, jobject instance,jstring args,jint fd, jstring uspfs_path) {
    const char *nargs = env->GetStringUTFChars(args, 0);
    const char *n_uspfs_path = (uspfs_path == NULL) ? (NULL) : (env->GetStringUTFChars(uspfs_path,0));
    const int nargslength = env->GetStringLength(args);
    int argc = 0;
    char **argv;

    allocate_args_from_string(nargs, nargslength, &argc, &argv);

    main_android(fd, n_uspfs_path, argc, argv);

    env->ReleaseStringUTFChars(args, nargs);
    if (uspfs_path != NULL) {
        env->ReleaseStringUTFChars(uspfs_path, n_uspfs_path);
    }

    int i;
    for (i = 0; i < argc; i++) {
        free(argv[i]);
    }
    free(argv);
}

#ifdef __cplusplus
}
#endif