package com.github.a2kaido.go.agent

data class AIConfig(
    val difficulty: AIDifficulty = AIDifficulty.EASY,
    val thinkingTimeMs: Long = 1000L,
    val randomizationLevel: Float = 0.1f, // 0.0 = deterministic, 1.0 = fully random
    val enableHints: Boolean = false,
    val showThinking: Boolean = true
) {
    companion object {
        fun forDifficulty(difficulty: AIDifficulty): AIConfig = when (difficulty) {
            AIDifficulty.BEGINNER -> AIConfig(
                difficulty = difficulty,
                thinkingTimeMs = 500L,
                randomizationLevel = 0.3f,
                enableHints = true,
                showThinking = false
            )
            AIDifficulty.EASY -> AIConfig(
                difficulty = difficulty,
                thinkingTimeMs = 1000L,
                randomizationLevel = 0.2f,
                enableHints = false,
                showThinking = true
            )
            AIDifficulty.MEDIUM -> AIConfig(
                difficulty = difficulty,
                thinkingTimeMs = 2000L,
                randomizationLevel = 0.1f,
                enableHints = false,
                showThinking = true
            )
            AIDifficulty.HARD -> AIConfig(
                difficulty = difficulty,
                thinkingTimeMs = 5000L,
                randomizationLevel = 0.05f,
                enableHints = false,
                showThinking = true
            )
        }
    }
}