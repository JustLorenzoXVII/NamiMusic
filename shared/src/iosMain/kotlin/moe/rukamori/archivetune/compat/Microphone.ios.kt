package moe.rukamori.archivetune.compat

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.coroutines.delay
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import kotlin.math.roundToInt

/**
 * Registra dal microfono con AVAudioEngine e ricampiona a [sampleRate] Hz mono 16 bit
 * (Shazam vuole 16 kHz).
 */
@OptIn(ExperimentalForeignApi::class)
actual suspend fun platformRecordMicrophone(
    sampleRate: Int,
    durationMs: Long,
): ShortArray {
    val session = AVAudioSession.sharedInstance()
    session.setCategory(AVAudioSessionCategoryPlayAndRecord, AVAudioSessionCategoryOptionDefaultToSpeaker or AVAudioSessionCategoryOptionMixWithOthers, null)
    session.setActive(true, null)
    val engine = AVAudioEngine()
    val input = engine.inputNode
    val format = input.outputFormatForBus(0u)
    val inputRate = format.sampleRate
    val collected = ArrayList<Float>()
    input.installTapOnBus(0u, bufferSize = 4096u, format = format) { buffer: AVAudioPCMBuffer?, _ ->
        val pcm = buffer ?: return@installTapOnBus
        val channels = pcm.floatChannelData ?: return@installTapOnBus
        val first = channels[0] ?: return@installTapOnBus
        val frames = pcm.frameLength.toInt()
        val chunk = FloatArray(frames) { first[it] }
        synchronizedAdd(collected, chunk)
    }
    try {
        engine.prepare()
        engine.startAndReturnError(null)
        delay(durationMs)
    } finally {
        input.removeTapOnBus(0u)
        engine.stop()
        session.setCategory(AVAudioSessionCategoryPlayback, null)
    }
    val samples = synchronizedSnapshot(collected)
    // Ricampionamento lineare a sampleRate.
    val ratio = inputRate / sampleRate
    val outLength = (samples.size / ratio).toInt()
    return ShortArray(outLength) { i ->
        val src = i * ratio
        val i0 = src.toInt().coerceIn(0, samples.lastIndex.coerceAtLeast(0))
        val i1 = (i0 + 1).coerceAtMost(samples.lastIndex.coerceAtLeast(0))
        val t = (src - i0).toFloat()
        val v = if (samples.isEmpty()) 0f else samples[i0] * (1 - t) + samples[i1] * t
        (v.coerceIn(-1f, 1f) * 32767f).roundToInt().toShort()
    }
}

private val lock = platform.Foundation.NSLock()

private fun synchronizedAdd(target: ArrayList<Float>, chunk: FloatArray) {
    lock.lock()
    try {
        chunk.forEach { target.add(it) }
    } finally {
        lock.unlock()
    }
}

private fun synchronizedSnapshot(source: ArrayList<Float>): FloatArray {
    lock.lock()
    try {
        return source.toFloatArray()
    } finally {
        lock.unlock()
    }
}
