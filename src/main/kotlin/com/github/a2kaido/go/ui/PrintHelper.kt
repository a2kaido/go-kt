package com.github.a2kaido.go.ui

import com.github.a2kaido.go.model.*

const val COLS = "ABCDEFGHJKLMNOPQRST"

val STONE_TO_CHAR: Map<Player?, String> = mapOf(
    null to ".",
    Player.Black to "x",
    Player.White to "o",
)

fun printMove(player: Player, move: Move) {
    val moveString = when (move.action) {
        MoveAction.Pass -> "passes"
        MoveAction.Resign -> "resigns"
        is MoveAction.Play -> "${COLS[move.action.point.col - 1]}, ${move.action.point.row}"
    }
    println("$player, $moveString")
}

fun printBoard(board: Board) {
    (1..board.numRows).forEach { row ->
        val bump = if (row <= 9) " " else ""
        val line = mutableListOf<String>()
        (1..(board.numCols)).forEach { col ->
            val stone = board.get(Point(row, col))
            line.add(STONE_TO_CHAR[stone].orEmpty())
        }
        println("$bump $row ${line.joinToString("")}")
    }
    println("    ${COLS.dropLast(19 - board.numCols)}")
}
