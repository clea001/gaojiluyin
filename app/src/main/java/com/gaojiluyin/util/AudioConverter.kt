package com.gaojiluyin.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioConverter @Inject constructor() {

    fun convertToWav(inputPath: String, outputPath: String) {
        val extractor = MediaExtractor()
        extractor.setDataSource(inputPath)

        val trackIndex = findAudioTrack(extractor)
        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val targetSampleRate = 16000
        val pcmData = decodeToPcm(extractor, codec)
        codec.stop()
        codec.release()
        extractor.release()

        val resampled = if (sampleRate != targetSampleRate) {
            resample(pcmData, sampleRate, targetSampleRate, channels)
        } else {
            pcmData
        }

        val monoData = if (channels == 2) {
            stereoToMono(resampled)
        } else {
            resampled
        }

        writeWav(outputPath, monoData, targetSampleRate, 1)
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)!!
            if (mime.startsWith("audio/")) return i
        }
        throw IllegalStateException("No audio track found")
    }

    private fun decodeToPcm(extractor: MediaExtractor, codec: MediaCodec): ByteArray {
        val output = mutableListOf<Byte>()
        val bufferInfo = MediaCodec.BufferInfo()
        val timeoutUs = 10_000L
        var inputDone = false
        var outputDone = false

        while (!outputDone) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (outputIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                val data = ByteArray(bufferInfo.size)
                outputBuffer.get(data)
                output.addAll(data.toList())
                codec.releaseOutputBuffer(outputIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    outputDone = true
                }
            }
        }
        return output.toByteArray()
    }

    private fun resample(data: ByteArray, fromRate: Int, toRate: Int, channels: Int): ByteArray {
        val shortInput = ShortArray(data.size / 2)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortInput)

        val ratio = fromRate.toDouble() / toRate
        val outputLength = (shortInput.size / ratio).toInt()
        val shortOutput = ShortArray(outputLength)

        for (i in 0 until outputLength) {
            val srcIndex = (i * ratio).toInt().coerceIn(0, shortInput.size - 1)
            shortOutput[i] = shortInput[srcIndex]
        }

        val byteOutput = ByteArray(shortOutput.size * 2)
        ByteBuffer.wrap(byteOutput).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shortOutput)
        return byteOutput
    }

    private fun stereoToMono(data: ByteArray): ByteArray {
        val shortInput = ShortArray(data.size / 2)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortInput)

        val monoLength = shortInput.size / 2
        val monoOutput = ShortArray(monoLength)
        for (i in 0 until monoLength) {
            val left = shortInput[i * 2].toInt()
            val right = shortInput[i * 2 + 1].toInt()
            monoOutput[i] = ((left + right) / 2).toShort()
        }

        val byteOutput = ByteArray(monoOutput.size * 2)
        ByteBuffer.wrap(byteOutput).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(monoOutput)
        return byteOutput
    }

    private fun writeWav(path: String, pcmData: ByteArray, sampleRate: Int, channels: Int) {
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmData.size
        val fileSize = 36 + dataSize

        FileOutputStream(path).use { fos ->
            fos.write("RIFF".toByteArray())
            fos.write(intToLittleEndian(fileSize))
            fos.write("WAVE".toByteArray())
            fos.write("fmt ".toByteArray())
            fos.write(intToLittleEndian(16))
            fos.write(shortToLittleEndian(1))
            fos.write(shortToLittleEndian(channels.toShort()))
            fos.write(intToLittleEndian(sampleRate))
            fos.write(intToLittleEndian(byteRate))
            fos.write(shortToLittleEndian(blockAlign.toShort()))
            fos.write(shortToLittleEndian(bitsPerSample.toShort()))
            fos.write("data".toByteArray())
            fos.write(intToLittleEndian(dataSize))
            fos.write(pcmData)
        }
    }

    private fun intToLittleEndian(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

    private fun shortToLittleEndian(value: Short): ByteArray =
        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
}
