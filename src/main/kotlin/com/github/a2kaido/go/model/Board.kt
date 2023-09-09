package com.github.a2kaido.go.model

data class Board(
    val numRows: Int,
    val numCols: Int,
    private val grid: MutableMap<Point, GoString>,
) {
    fun placeStone(player: Player, point: Point) {
        assert(isOnGrid(point))
        assert(grid[point] != null)

        val liberties = mutableListOf<Point>()
        val adjacentSameColor = mutableListOf<GoString>()
        val adjacentOppositeColor = mutableListOf<GoString>()
        point.neighbors().forEach { neighbor ->
            if (isOnGrid(neighbor)) return@forEach

            val neighborString = grid[neighbor]
            if (neighborString == null) {
                liberties.add(neighbor)
            } else if (neighborString.color == player) {
                if ((neighborString in adjacentSameColor).not()) {
                    adjacentSameColor.add(neighborString)
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
            adjacentOppositeColor.forEach {
                it.removeLiberty(point)
            }
            adjacentOppositeColor.forEach {
                if (it.numLiberties() == 0) {
                    removeString(it)
                }
            }
        }
    }

    private fun isOnGrid(point: Point): Boolean {
        if (point.row < 1 || point.row > numRows) return false
        if (point.col < 1 || point.col > numCols) return false
        return true
    }

    private fun removeString(string: GoString) {
        string.stones.forEach { point ->
            point.neighbors().forEach loop@ { neighbor ->
                val neighborString = grid[neighbor] ?: return@loop
                if (neighborString != string) {
                    neighborString.addLiberty(point)
                }
            }
            grid.remove(point)
        }
    }
}
