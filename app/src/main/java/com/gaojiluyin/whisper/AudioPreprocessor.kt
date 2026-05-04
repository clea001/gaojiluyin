package com.gaojiluyin.whisper

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPreprocessor @Inject constructor() {

    fun loadPcmAsFloat(wavPath: String): FloatArray {
        val file = File(wavPath)
        val bytes = file.readBytes()

        val dataOffset = findDataOffset(bytes)
        val pcmBytes = bytes.copyOfRange(dataOffset, bytes.size)

        val shortSamples = ShortArray(pcmBytes.size / 2)
        ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortSamples)

        return FloatArray(shortSamples.size) { i ->
            shortSamples[i].toFloat() / 32768.0f
        }
    }

    private fun findDataOffset(wavBytes: ByteArray): Int {
        var i = 12
        while (i < wavBytes.size - 8) {
            val chunkId = String(wavBytes, i, 4)
            val chunkSize = ByteBuffer.wrap(wavBytes, i + 4, 4)
                .order(ByteOrder.LITTLE_ENDIAN).int
            if (chunkId == "data") return i + 8
            i += 8 + chunkSize
        }
        return 44
    }
}
