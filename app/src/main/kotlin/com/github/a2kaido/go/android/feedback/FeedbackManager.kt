package com.github.a2kaido.go.android.feedback

import android.content.Context
import com.github.a2kaido.go.android.audio.AudioManager
import com.github.a2kaido.go.android.audio.SoundType
import com.github.a2kaido.go.android.haptic.HapticFeedbackType
import com.github.a2kaido.go.android.haptic.HapticIntensity
import com.github.a2kaido.go.android.haptic.HapticManager
import com.github.a2kaido.go.model.Player
import kotlinx.coroutines.CoroutineScope

class FeedbackManager(context: Context, scope: CoroutineScope) {
    
    private val audioManager = AudioManager(context, scope)
    private val hapticManager = HapticManager(context)
    
    // Game feedback methods
    fun onStonePlace(player: Player) {
        val soundType = when (player) {
            Player.Black -> SoundType.STONE_PLACE_BLACK
            Player.White -> SoundType.STONE_PLACE_WHITE
        }
        audioManager.playSound(soundType)
        hapticManager.performHapticFeedback(HapticFeedbackType.STONE_PLACE)
    }
    
    fun onStoneCapture() {
        audioManager.playSound(SoundType.STONE_CAPTURE)
        hapticManager.performHapticFeedback(HapticFeedbackType.STONE_CAPTURE)
    }
    
    fun onInvalidMove() {
        audioManager.playSound(SoundType.INVALID_MOVE)
        hapticManager.performHapticFeedback(HapticFeedbackType.INVALID_MOVE)
    }
    
    fun onGameWin() {
        audioManager.playSound(SoundType.GAME_WIN)
        hapticManager.performHapticFeedback(HapticFeedbackType.GAME_WIN)
    }
    
    fun onGameLose() {
        audioManager.playSound(SoundType.GAME_LOSE)
        hapticManager.performHapticFeedback(HapticFeedbackType.GAME_LOSE)
    }
    
    // UI feedback methods
    fun onMenuClick() {
        audioManager.playSound(SoundType.MENU_CLICK)
        hapticManager.performHapticFeedback(HapticFeedbackType.MENU_CLICK)
    }
    
    fun onButtonClick() {
        audioManager.playSound(SoundType.BUTTON_CLICK)
        hapticManager.performHapticFeedback(HapticFeedbackType.BUTTON_CLICK)
    }
    
    // Settings methods
    fun setSoundEnabled(enabled: Boolean) {
        audioManager.setSoundEnabled(enabled)
    }
    
    fun setHapticEnabled(enabled: Boolean) {
        hapticManager.setHapticEnabled(enabled)
    }
    
    fun setMasterVolume(volume: Float) {
        audioManager.setMasterVolume(volume)
    }
    
    fun setHapticIntensity(intensity: HapticIntensity) {
        hapticManager.setHapticIntensity(intensity)
    }
    
    // Getters
    fun isSoundEnabled(): Boolean = audioManager.isSoundEnabled()
    fun isHapticEnabled(): Boolean = hapticManager.isHapticEnabled()
    fun getMasterVolume(): Float = audioManager.getMasterVolume()
    fun getHapticIntensity(): HapticIntensity = hapticManager.getHapticIntensity()
    fun isHapticSupported(): Boolean = hapticManager.isHapticSupported()
    
    fun release() {
        audioManager.release()
    }
}