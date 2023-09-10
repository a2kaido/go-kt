package com.github.a2kaido.go.agent

import com.github.a2kaido.go.game.GameState
import com.github.a2kaido.go.model.Move

interface Agent {

    fun selectMove(
        gameState: GameState,
    ): Move
}
