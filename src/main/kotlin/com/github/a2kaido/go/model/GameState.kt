package com.github.a2kaido.go.model

/**
 * Represents a player in the game of Go.
 */
enum class Player {
    BLACK, WHITE;

    /**
     * Returns the other player.
     */
    fun other(): Player {
        return when (this) {
            BLACK -> WHITE
            WHITE -> BLACK
        }
    }
}

/**
 * Represents a move made by a player.
 *
 * @property player The player who made the move.
 * @property x The 0-indexed x-coordinate (column) of the move.
 * @property y The 0-indexed y-coordinate (row) of the move.
 */
data class Move(val player: Player, val x: Int, val y: Int)

/**
 * Represents the state of the Go board.
 *
 * @property size The size of the board (e.g., 19 for a 19x19 board).
 * @property stones A map where the key is a Pair<Int, Int> representing (x,y)
 *                  coordinates (0-indexed), and the value is the Player
 *                  who has a stone at that position.
 */
data class Board(
    val size: Int,
    val stones: Map<Pair<Int, Int>, Player>
) {
    companion object {
        /**
         * Creates a new empty Go board of the given size.
         *
         * @param size The size of the board (e.g., 19 for a 19x19 board).
         * @return A new Board instance with no stones placed.
         */
        fun empty(size: Int): Board {
            if (size <= 0) {
                throw IllegalArgumentException("Board size must be positive.")
            }
            return Board(size = size, stones = emptyMap())
        }
    }

    /**
     * Checks if a given (x,y) coordinate is within the board boundaries.
     * Coordinates are 0-indexed.
     *
     * @param x The x-coordinate (column).
     * @param y The y-coordinate (row).
     * @return True if the coordinate is on the board, false otherwise.
     */
    fun isOnBoard(x: Int, y: Int): Boolean {
        return x >= 0 && x < size && y >= 0 && y < size
    }

    /**
     * Gets the player (stone) at a given (x,y) coordinate.
     *
     * @param x The x-coordinate (column).
     * @param y The y-coordinate (row).
     * @return The Player at the given coordinate, or null if the point is empty.
     */
    fun getStone(x: Int, y: Int): Player? {
        return stones[Pair(x, y)]
    }

    /**
     * Places a stone on the board for the given player at the specified (x,y) coordinate.
     * Returns a new Board instance with the stone added.
     * This does not check for game rules (e.g., ko, suicide).
     *
     * @param player The player placing the stone.
     * @param x The x-coordinate (column).
     * @param y The y-coordinate (row).
     * @return A new Board instance with the stone placed.
     * @throws IllegalArgumentException if the coordinates are off the board or the point is already occupied.
     */
    fun placeStone(player: Player, x: Int, y: Int): Board {
        if (!isOnBoard(x, y)) {
            throw IllegalArgumentException("Coordinates ($x, $y) are off the board (size $size).")
        }
        if (stones.containsKey(Pair(x, y))) {
            throw IllegalArgumentException("Point ($x, $y) is already occupied.")
        }
        val newStones = stones + (Pair(x, y) to player)
        return this.copy(stones = newStones)
    }
}
