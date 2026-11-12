package com.jarvis.personalassistant.ai

import java.io.File

enum class LocalModelRuntimeStatus {
    NOT_IMPLEMENTED_NO_NATIVE_SDK,
    MODEL_FILE_MISSING,
    READY_EXTERNAL_BRIDGE
}

/**
 * Honest Status Adapter for Local On-Device LLMs (Gemma 3 & Qwen 3.5 via LiteRT-LM).
 *
 * IMPORTANT ARCHITECTURAL STATUS:
 * - Native LiteRT-LM JNI/C++ engine and multi-hundred-MB .litertlm model weight files
 *   (Gemma 3 1B / Qwen 3.5 0.8B) are NOT bundled or compiled into this lightweight MVP repository.
 * - This class validates whether a local .litertlm file path has been configured by the user
 *   and whether the com.google.ai.edge.litertlm runtime class is present on the classpath.
 * - When not present, it explicitly returns LocalModelRuntimeStatus.NOT_IMPLEMENTED_NO_NATIVE_SDK
 *   so ModelRouter falls back transparently to the Offline Deterministic Rule Engine without
 *   pretending that a neural LLM generated the response.
 */
object LiteRtLmBridge {

    fun checkRuntimeStatus(localModelFilePath: String?): LocalModelRuntimeStatus {
        val isSdkLinked = try {
            Class.forName("com.google.ai.edge.litertlm.LiteRtLm")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

        if (!isSdkLinked) {
            return LocalModelRuntimeStatus.NOT_IMPLEMENTED_NO_NATIVE_SDK
        }

        if (localModelFilePath.isNullOrBlank() || !File(localModelFilePath).exists()) {
            return LocalModelRuntimeStatus.MODEL_FILE_MISSING
        }

        return LocalModelRuntimeStatus.READY_EXTERNAL_BRIDGE
    }
}
