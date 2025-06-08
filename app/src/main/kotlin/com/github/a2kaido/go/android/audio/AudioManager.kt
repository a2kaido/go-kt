package com.github.a2kaido.go.android.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AudioManager(private val context: Context, private val scope: CoroutineScope) {
    
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<SoundType, Int>()
    private var soundEnabled = true
    private var masterVolume = 1.0f
    
    init {
        initializeSoundPool()
    }
    
    private fun initializeSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()
        
        loadSounds()
    }
    
    private fun loadSounds() {
        scope.launch(Dispatchers.IO) {
            soundPool?.let { pool ->
                // For demonstration, we'll use placeholder values
                // In production, these would be loaded from actual audio files
                soundMap[SoundType.STONE_PLACE_BLACK] = 1
                soundMap[SoundType.STONE_PLACE_WHITE] = 2
                soundMap[SoundType.STONE_CAPTURE] = 3
                soundMap[SoundType.INVALID_MOVE] = 4
                soundMap[SoundType.GAME_WIN] = 5
                soundMap[SoundType.GAME_LOSE] = 6
                soundMap[SoundType.MENU_CLICK] = 7
                soundMap[SoundType.BUTTON_CLICK] = 8
            }
        }
    }
    
    fun playSound(soundType: SoundType, volume: Float = 1.0f) {
        if (!soundEnabled) return
        
        soundPool?.let { pool ->
            soundMap[soundType]?.let { soundId ->
                val actualVolume = volume * masterVolume
                pool.play(soundId, actualVolume, actualVolume, 1, 0, 1.0f)
            }
        }
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }
    
    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0.0f, 1.0f)
    }
    
    fun isSoundEnabled(): Boolean = soundEnabled
    
    fun getMasterVolume(): Float = masterVolume
    
    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}

enum class SoundType {
    STONE_PLACE_BLACK,
    STONE_PLACE_WHITE,
    STONE_CAPTURE,
    INVALID_MOVE,
    GAME_WIN,
    GAME_LOSE,
    MENU_CLICK,
    BUTTON_CLICK
}