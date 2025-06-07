package com.github.a2kaido.go.model

import com.github.a2kaido.go.agent.zobristEmpty
import com.github.a2kaido.go.agent.zobristHash

data class Board(
    val numRows: Int,
    val numCols: Int,
    val grid: MutableMap<Point, GoString>,
    private var hash: Long = zobristEmpty,
) {
    fun deepCopy() = Board(
        numRows = numRows,
        numCols = numCols,
        grid = grid.mapValues { (_, goString) ->
            goString.copy()
        }.toMutableMap(),
        hash = hash,
    )

    fun placeStone(player: Player, point: Point) {
        assert(isOnGrid(point))
        assert(grid[point] == null)

        val liberties = mutableListOf<Point>()
        val adjacentSameColor = mutableListOf<GoString>()
        val adjacentOppositeColor = mutableListOf<GoString>()
        point.neighbors().forEach { neighbor ->
            if (isOnGrid(neighbor).not()) return@forEach

            val neighborString = grid[neighbor]
            if (neighborString == null) {
                liberties.add(neighbor)
            } else if (neighborString.color == player) {
                if ((neighborString in adjacentSameColor).not()) {
                    adjacentSameColor.add(neighborString)
                }
            } else {
                if ((neighborString in adjacentOppositeColor).not()) {
                    adjacentOppositeColor.add(neighborString)
                }
            }
        }
        var newString = GoString(player, listOf(point), liberties)
        adjacentSameColor.forEach {
            newString = newString.mergedWith(it)
        }
        newString.stones.forEach {
            grid[it] = newString
        }
        hash = hash.xor(zobristHash[point to player]!!)
        adjacentOppositeColor.forEach {
            val replacement = it.withoutLiberty(point)
            if (replacement.numLiberties() > 0) {
                replaceString(it.withoutLiberty(point))
            } else {
                removeString(it)
            }
        }
        adjacentOppositeColor.forEach {
            if (it.numLiberties() == 0) {
                removeString(it)
            }
        }
    }

    fun isOnGrid(point: Point): Boolean {
        if (point.row < 1 || point.row > numRows) return false
        if (point.col < 1 || point.col > numCols) return false
        return true
    }

    fun get(point: Point): Player? {
        return grid[point]?.color
    }

    fun getGoString(point: Point): GoString? {
        return grid[point]
    }

    private fun replaceString(string: GoString) {
        string.stones.forEach { point ->
            grid[point] = string
        }
    }

    private fun removeString(string: GoString) {
        string.stones.forEach { point ->
            point.neighbors().forEach loop@ { neighbor ->
                val neighborString = grid[neighbor] ?: return@loop
                if (neighborString != string) {
                    replaceString(neighborString.withLiberty(point))
                }
            }
            grid.remove(point)
            hash = hash.xor(zobristHash[point to string.color]!!)
        }
    }

    fun zobristHash() = hash
}
