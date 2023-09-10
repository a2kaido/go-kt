package com.github.a2kaido.go.agent

import com.github.a2kaido.go.model.Board
import com.github.a2kaido.go.model.Player
import com.github.a2kaido.go.model.Point

fun isPointAnEye(board: Board, point: Point, color: Player): Boolean {
    if (board.get(point) != null) return false

    point.neighbors().forEach { neighbor ->
        if (board.isOnGrid(neighbor) && board.get(neighbor) != color) {
            return false
        }
    }

    var friendlyCorners = 0
    var offBoardCorners = 0
    val corners = listOf(
        Point(point.row - 1, point.col - 1),
        Point(point.row - 1, point.col + 1),
        Point(point.row + 1, point.col - 1),
        Point(point.row + 1, point.col + 1),
    )

    corners.forEach { corner ->
        if (board.isOnGrid(corner)) {
            if (board.get(corner) == color) {
                friendlyCorners += 1
            }
        } else {
            offBoardCorners += 1
        }
    }

    if (offBoardCorners > 0) {
        return offBoardCorners + friendlyCorners == 4
    }
    return friendlyCorners > 3
}
