package com.github.a2kaido.go.model

import com.github.a2kaido.go.game.GameState
import kotlin.test.Test
import kotlin.test.assertFalse

class GameStateTest {

    @Test
    fun `self capture`() {
        val game = GameState.newGame(9)
            .applyMove(Move.play(Point(2, 1)))
            .applyMove(Move.play(Point(3, 3)))
            .applyMove(Move.play(Point(3, 2)))
            .applyMove(Move.play(Point(3, 4)))
            .applyMove(Move.play(Point(4, 2)))
            .applyMove(Move.play(Point(4, 1)))
            .applyMove(Move.play(Point(5, 1)))
        assertFalse(game.isValidMove(Move.play(Point(3, 1))))
    }
}
