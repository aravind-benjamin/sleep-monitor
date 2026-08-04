package com.example.sleepmonitor.service

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Writes 16-bit mono PCM short arrays out as a playable .wav file. */
object WavWriter {

    fun write(file: File, samples: ShortArray, sampleRate: Int) {
        val byteData = ByteArray(samples.size * 2)
        val buffer = ByteBuffer.wrap(byteData).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) buffer.putShort(s)

        FileOutputStream(file).use { out ->
            val totalDataLen = byteData.size + 36
            val header = ByteArray(44)
            val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

            // RIFF header
            header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
            bb.putInt(4, totalDataLen)
            header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()

            // fmt chunk
            header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
            bb.putInt(16, 16)              // subchunk size
            bb.putShort(20, 1)             // PCM format
            bb.putShort(22, 1)             // mono
            bb.putInt(24, sampleRate)
            bb.putInt(28, sampleRate * 2)  // byte rate
            bb.putShort(32, 2)             // block align
            bb.putShort(34, 16)            // bits per sample

            // data chunk
            header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
            bb.putInt(40, byteData.size)

            out.write(header)
            out.write(byteData)
        }
    }
}
