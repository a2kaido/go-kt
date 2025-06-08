package com.github.a2kaido.go.android.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat

class HapticManager(private val context: Context) {
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = ContextCompat.getSystemService(context, VibratorManager::class.java)
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.getSystemService(context, Vibrator::class.java)
        }
    }
    
    private var hapticEnabled = true
    private var hapticIntensity = HapticIntensity.MEDIUM
    
    fun performHapticFeedback(feedbackType: HapticFeedbackType) {
        if (!hapticEnabled || vibrator?.hasVibrator() != true) return
        
        val effect = when (feedbackType) {
            HapticFeedbackType.STONE_PLACE -> createStonePlace()
            HapticFeedbackType.STONE_CAPTURE -> createStoneCapture()
            HapticFeedbackType.INVALID_MOVE -> createInvalidMove()
            HapticFeedbackType.MENU_CLICK -> createMenuClick()
            HapticFeedbackType.BUTTON_CLICK -> createButtonClick()
            HapticFeedbackType.GAME_WIN -> createGameWin()
            HapticFeedbackType.GAME_LOSE -> createGameLose()
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && effect != null) {
            vibrator?.vibrate(effect)
        } else {
            // Fallback for older Android versions
            @Suppress("DEPRECATION")
            vibrator?.vibrate(getLegacyDuration(feedbackType))
        }
    }
    
    private fun createStonePlace(): VibrationEffect? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createOneShot(
                getBaseDuration(50),
                getIntensityAmplitude(VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            null
        }
    }
    
    private fun createStoneCapture(): VibrationEffect? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(
                longArrayOf(0, 80, 40, 80),
                intArrayOf(0, getIntensityAmplitude(200), 0, getIntensityAmplitude(150)),
                -1
            )
        } else {
            null
        }
    }
    
    private fun createInvalidMove(): VibrationEffect? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(
                longArrayOf(0, 30, 30, 30, 30, 30),
                intArrayOf(0, getIntensityAmplitude(100), 0, getIntensityAmplitude(100), 0, getIntensityAmplitude(100)),
                -1
            )
        } else {
            null
        }
    }
    
    private fun createMenuClick(): VibrationEffect? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createOneShot(
                getBaseDuration(20),
                getIntensityAmplitude(100)
            )
        } else {
            null
        }
    }
    
    private fun createButtonClick(): VibrationEffect? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createOneShot(
                getBaseDuration(30),
                getIntensityAmplitude(120)
            )
        } else {
            null
        }
    }
    
    private fun createGameWin(): VibrationEffect? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(
                longArrayOf(0, 100, 50, 100, 50, 200),
                intArrayOf(0, getIntensityAmplitude(150), 0, getIntensityAmplitude(180), 0, getIntensityAmplitude(255)),
                -1
            )
        } else {
            null
        }
    }
    
    private fun createGameLose(): VibrationEffect? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createOneShot(
                getBaseDuration(300),
                getIntensityAmplitude(100)
            )
        } else {
            null
        }
    }
    
    private fun getBaseDuration(baseDuration: Long): Long {
        return when (hapticIntensity) {
            HapticIntensity.LIGHT -> (baseDuration * 0.7).toLong()
            HapticIntensity.MEDIUM -> baseDuration
            HapticIntensity.STRONG -> (baseDuration * 1.3).toLong()
        }
    }
    
    private fun getIntensityAmplitude(baseAmplitude: Int): Int {
        return when (hapticIntensity) {
            HapticIntensity.LIGHT -> (baseAmplitude * 0.6).toInt()
            HapticIntensity.MEDIUM -> baseAmplitude
            HapticIntensity.STRONG -> (baseAmplitude * 1.0).toInt() // Cap at max
        }.coerceIn(1, 255)
    }
    
    private fun getLegacyDuration(feedbackType: HapticFeedbackType): Long {
        return when (feedbackType) {
            HapticFeedbackType.STONE_PLACE -> getBaseDuration(50)
            HapticFeedbackType.STONE_CAPTURE -> getBaseDuration(160)
            HapticFeedbackType.INVALID_MOVE -> getBaseDuration(120)
            HapticFeedbackType.MENU_CLICK -> getBaseDuration(20)
            HapticFeedbackType.BUTTON_CLICK -> getBaseDuration(30)
            HapticFeedbackType.GAME_WIN -> getBaseDuration(400)
            HapticFeedbackType.GAME_LOSE -> getBaseDuration(300)
        }
    }
    
    fun setHapticEnabled(enabled: Boolean) {
        hapticEnabled = enabled
    }
    
    fun setHapticIntensity(intensity: HapticIntensity) {
        hapticIntensity = intensity
    }
    
    fun isHapticEnabled(): Boolean = hapticEnabled
    
    fun getHapticIntensity(): HapticIntensity = hapticIntensity
    
    fun isHapticSupported(): Boolean = vibrator?.hasVibrator() == true
}

enum class HapticFeedbackType {
    STONE_PLACE,
    STONE_CAPTURE,
    INVALID_MOVE,
    MENU_CLICK,
    BUTTON_CLICK,
    GAME_WIN,
    GAME_LOSE
}

enum class HapticIntensity {
    LIGHT,
    MEDIUM,
    STRONG
}