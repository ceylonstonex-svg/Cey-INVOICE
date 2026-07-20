package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

object ZenMusicManager {
    private const val TAG = "ZenMusicManager"
    private const val SAMPLE_RATE = 22050

    private var audioTrack: AudioTrack? = null
    private var synthThread: Thread? = null
    
    private val _isPlayingFlow = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow

    @Volatile
    private var isRunning = false

    enum class ZenSoundscape(val title: String, val desc: String) {
        CEYLON_MIST("Ceylon Mist", "Deep warm drone, slow breeze & chimes"),
        SPICE_GARDEN("Spice Garden", "Light pentatonic scale, twinkles & flow"),
        TEMPLE_CALM("Temple Calm", "Slow meditative resonances & gongs")
    }

    private val _currentSoundscape = MutableStateFlow(ZenSoundscape.CEYLON_MIST)
    val currentSoundscape: StateFlow<ZenSoundscape> = _currentSoundscape

    fun setSoundscape(soundscape: ZenSoundscape) {
        _currentSoundscape.value = soundscape
    }

    fun togglePlay() {
        if (_isPlayingFlow.value) {
            stop()
        } else {
            start()
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        _isPlayingFlow.value = true

        synthThread = Thread {
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        android.media.AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }

                audioTrack = track
                track.play()

                val buffer = ShortArray(bufferSize)
                
                // Pentatonic scales
                val mistScale = doubleArrayOf(220.0, 246.94, 293.66, 329.63, 392.00, 440.00) // A minor pentatonic (A3-A4)
                val gardenScale = doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25) // C major pentatonic (C4-C5)
                val templeScale = doubleArrayOf(146.83, 164.81, 196.00, 220.00, 293.66) // D minor deep pentatonic (D3-D4)

                var phaseDrone1 = 0.0
                var phaseDrone2 = 0.0
                var phaseMelody = 0.0
                var phaseChime = 0.0

                var melodyFreq = 0.0
                var melodyNoteRemaining = 0
                
                var chimeFreq = 0.0
                var chimeRemaining = 0
                var chimeType = 0 // 0 = standard chime, 1 = metallic ring

                while (isRunning) {
                    val currentLandscape = _currentSoundscape.value
                    
                    val scale = when (currentLandscape) {
                        ZenSoundscape.CEYLON_MIST -> mistScale
                        ZenSoundscape.SPICE_GARDEN -> gardenScale
                        ZenSoundscape.TEMPLE_CALM -> templeScale
                    }

                    val d1Freq = when (currentLandscape) {
                        ZenSoundscape.CEYLON_MIST -> 110.0
                        ZenSoundscape.SPICE_GARDEN -> 130.81
                        ZenSoundscape.TEMPLE_CALM -> 73.42
                    }
                    val d2Freq = when (currentLandscape) {
                        ZenSoundscape.CEYLON_MIST -> 165.0
                        ZenSoundscape.SPICE_GARDEN -> 196.0
                        ZenSoundscape.TEMPLE_CALM -> 110.0
                    }

                    val melodyDuration = when (currentLandscape) {
                        ZenSoundscape.CEYLON_MIST -> 3.5
                        ZenSoundscape.SPICE_GARDEN -> 2.0
                        ZenSoundscape.TEMPLE_CALM -> 6.0
                    }
                    val melodySamplesTotal = (SAMPLE_RATE * melodyDuration).toInt()

                    for (i in buffer.indices) {
                        if (melodyNoteRemaining <= 0) {
                            melodyFreq = if (Random.nextFloat() < 0.85f) {
                                scale[Random.nextInt(scale.size)]
                            } else {
                                0.0
                            }
                            melodyNoteRemaining = melodySamplesTotal
                        }

                        if (chimeRemaining <= 0) {
                            if (Random.nextFloat() < 0.08f) {
                                chimeFreq = scale[Random.nextInt(scale.size)] * 3.0
                                chimeRemaining = (SAMPLE_RATE * (0.8 + Random.nextFloat() * 1.5)).toInt()
                                chimeType = if (Random.nextBoolean()) 0 else 1
                            } else {
                                chimeFreq = 0.0
                                chimeRemaining = SAMPLE_RATE / 4
                            }
                        }

                        val melodyProgress = (melodySamplesTotal - melodyNoteRemaining).toDouble() / melodySamplesTotal
                        val melodyEnvelope = Math.sin(melodyProgress * Math.PI)

                        val chimeEnvelope = if (chimeFreq > 0.0) {
                            val progress = chimeRemaining.toDouble() / (SAMPLE_RATE * 2.0)
                            Math.pow(progress.coerceIn(0.0, 1.0), 3.0)
                        } else {
                            0.0
                        }

                        phaseDrone1 += 2.0 * Math.PI * d1Freq / SAMPLE_RATE
                        if (phaseDrone1 > 2.0 * Math.PI) phaseDrone1 -= 2.0 * Math.PI

                        phaseDrone2 += 2.0 * Math.PI * d2Freq / SAMPLE_RATE
                        if (phaseDrone2 > 2.0 * Math.PI) phaseDrone2 -= 2.0 * Math.PI

                        if (melodyFreq > 0.0) {
                            phaseMelody += 2.0 * Math.PI * melodyFreq / SAMPLE_RATE
                            if (phaseMelody > 2.0 * Math.PI) phaseMelody -= 2.0 * Math.PI
                        }

                        if (chimeFreq > 0.0) {
                            phaseChime += 2.0 * Math.PI * chimeFreq / SAMPLE_RATE
                            if (phaseChime > 2.0 * Math.PI) phaseChime -= 2.0 * Math.PI
                        }

                        var valDrone = Math.sin(phaseDrone1) * 0.16 + Math.sin(phaseDrone2) * 0.10
                        valDrone += Math.sin(phaseDrone1 * 2) * 0.03
                        
                        val valMelody = if (melodyFreq > 0.0) {
                            Math.sin(phaseMelody) * 0.14 * melodyEnvelope
                        } else {
                            0.0
                        }

                        var valChime = 0.0
                        if (chimeFreq > 0.0) {
                            if (chimeType == 0) {
                                valChime = Math.sin(phaseChime) * 0.10 * chimeEnvelope
                            } else {
                                val phaseChimeDetuned = phaseChime * 1.006
                                valChime = (Math.sin(phaseChime) + Math.sin(phaseChimeDetuned)) * 0.05 * chimeEnvelope
                            }
                        }

                        val rawNoise = (Random.nextDouble() - 0.5)
                        val windFactor = when (currentLandscape) {
                            ZenSoundscape.CEYLON_MIST -> 0.02
                            ZenSoundscape.SPICE_GARDEN -> 0.015
                            ZenSoundscape.TEMPLE_CALM -> 0.008
                        }
                        val valWind = rawNoise * windFactor

                        val combined = valDrone + valMelody + valChime + valWind
                        val sample = (combined.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                        buffer[i] = sample

                        melodyNoteRemaining--
                        chimeRemaining--
                    }

                    track.write(buffer, 0, buffer.size)
                }

                track.stop()
                track.release()
            } catch (e: Exception) {
                Log.e(TAG, "Synthesis failed", e)
            } finally {
                _isPlayingFlow.value = false
            }
        }
        synthThread?.start()
    }

    fun stop() {
        isRunning = false
        _isPlayingFlow.value = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Released
        }
        audioTrack = null
        synthThread = null
    }
}
