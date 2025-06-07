package com.github.a2kaido.go

import com.github.a2kaido.go.agent.RandomBot
import com.github.a2kaido.go.game.GameState
import com.github.a2kaido.go.model.Player
import com.github.a2kaido.go.ui.printBoard
import com.github.a2kaido.go.ui.printMove

fun main() {
    val boardSize = 9
    var game = GameState.newGame(boardSize)

    val bots = mapOf(
        Player.Black to RandomBot(),
        Player.White to RandomBot(),
    )

    while (game.isOver().not()) {
        Thread.sleep(300)
        println(Char(27) + "[2J")
        printBoard(game.board)
        val botMove = bots[game.nextPlayer]!!.selectMove(game)
        printMove(game.nextPlayer, botMove)
        game = game.applyMove(botMove)
    }
}
