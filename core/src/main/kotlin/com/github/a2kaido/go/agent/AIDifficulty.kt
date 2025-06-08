package com.github.a2kaido.go.agent

enum class AIDifficulty {
    BEGINNER,
    EASY,
    MEDIUM,
    HARD,
    ;

    fun getDisplayName(): String = when (this) {
        BEGINNER -> "Beginner"
        EASY -> "Easy"
        MEDIUM -> "Medium"
        HARD -> "Hard"
    }

    fun getDescription(): String = when (this) {
        BEGINNER -> "Random moves, avoids obvious captures"
        EASY -> "Basic pattern matching"
        MEDIUM -> "Territory consideration, basic tactics"
        HARD -> "Advanced patterns, opening knowledge"
    }
}