package com.github.a2kaido.go.agent

object AIFactory {
    fun createAgent(difficulty: AIDifficulty, config: AIConfig? = null): Agent {
        val actualConfig = config ?: AIConfig.forDifficulty(difficulty)
        
        return when (difficulty) {
            AIDifficulty.BEGINNER -> BeginnerBot(actualConfig)
            AIDifficulty.EASY -> EasyBot(actualConfig)
            AIDifficulty.MEDIUM -> MediumBot(actualConfig)
            AIDifficulty.HARD -> MediumBot(actualConfig) // Use MediumBot for now, can implement HardBot later
        }
    }
    
    fun createRandomAgent(): Agent = RandomBot()
}