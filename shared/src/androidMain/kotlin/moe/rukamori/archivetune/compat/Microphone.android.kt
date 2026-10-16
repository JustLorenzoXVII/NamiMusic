package moe.rukamori.archivetune.compat

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

@SuppressLint("MissingPermission")
actual suspend fun platformRecordMicrophone(
    sampleRate: Int,
    durationMs: Long,
): ShortArray {
    val channel = AudioFormat.CHANNEL_IN_MONO
    val encoding = AudioFormat.ENCODING_PCM_16BIT
    val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channel, encoding).coerceAtLeast(4_096)
    val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channel, encoding, bufferSize)
    if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
        audioRecord.release()
        error("AudioRecord failed to initialize")
    }
    val output = ShortArray((durationMs * sampleRate / 1_000L).toInt())
    val buffer = ShortArray(bufferSize / Short.SIZE_BYTES)
    try {
        audioRecord.startRecording()
        var written = 0
        while (written < output.size) {
            coroutineContext.ensureActive()
            val read = audioRecord.read(buffer, 0, minOf(buffer.size, output.size - written))
            if (read < 0) error("AudioRecord read failed with code $read")
            if (read == 0) continue
            buffer.copyInto(output, destinationOffset = written, endIndex = read)
            written += read
        }
        return output.copyOf(written)
    } finally {
        runCatching { audioRecord.stop() }
        audioRecord.release()
    }
}
