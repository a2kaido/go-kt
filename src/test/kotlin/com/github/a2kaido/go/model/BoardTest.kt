package com.github.a2kaido.go.model

import com.github.a2kaido.go.ui.printBoard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BoardTest {

    @Test
    fun `placeStone one stone`() {
        val oneStone = Point(3, 3)

        val board = Board(13, 13, mutableMapOf())

        board.placeStone(Player.Black, oneStone)

        val player = board.get(oneStone)
        assertEquals(
            Player.Black,
            player,
        )
        assertNull(board.get(Point(2, 3)))

        val goString = board.getGoString(oneStone)
        assertEquals(
            Player.Black,
            goString?.color,
        )
        assertEquals(
            listOf(oneStone),
            goString?.stones,
        )
        assertEquals(
            mutableListOf(Point(2, 3), Point(3, 2), Point(4, 3), Point(3, 4)),
            goString?.liberties,
        )
    }

    @Test
    fun `placeStone two black stones`() {
        val oneStone = Point(3, 3)
        val twoStone = Point(3, 4)

        val board = Board(13, 13, mutableMapOf())
        board.placeStone(Player.Black, oneStone)
        board.placeStone(Player.Black, twoStone)

        assertEquals(
            Player.Black,
            board.get(oneStone),
        )
        assertEquals(
            Player.Black,
            board.get(twoStone),
        )

        val goString = board.getGoString(oneStone)
        assertEquals(
            Player.Black,
            goString?.color,
        )
        assertEquals(
            listOf(twoStone, oneStone),
            goString?.stones,
        )
        assertEquals(
            mutableListOf(
                Point(2, 4),
                Point(4, 4),
                Point(3, 5),
                Point(2, 3),
                Point(3, 2),
                Point(4, 3),
            ),
            goString?.liberties,
        )
    }

    @Test
    fun `placeStone two stones different colors`() {
        val oneStone = Point(3, 3)
        val twoStone = Point(3, 4)

        val board = Board(13, 13, mutableMapOf())
        board.placeStone(Player.Black, oneStone)
        board.placeStone(Player.White, twoStone)

        assertEquals(
            Player.Black,
            board.get(oneStone),
        )
        assertEquals(
            Player.White,
            board.get(twoStone),
        )

        val goString1 = board.getGoString(oneStone)
        assertEquals(
            Player.Black,
            goString1?.color,
        )
        assertEquals(
            listOf(oneStone),
            goString1?.stones,
        )
        assertEquals(
            mutableListOf(
                Point(2, 3),
                Point(3, 2),
                Point(4, 3),
            ),
            goString1?.liberties,
        )

        val goString2 = board.getGoString(twoStone)
        assertEquals(
            Player.White,
            goString2?.color,
        )
        assertEquals(
            listOf(twoStone),
            goString2?.stones,
        )
        assertEquals(
            mutableListOf(
                Point(2, 4),
                Point(4, 4),
                Point(3, 5),
            ),
            goString2?.liberties,
        )
    }

    @Test
    fun `placeStone get stone by surrounded`() {
        val surroundedStone = Point(3, 3)

        val board = Board(13, 13, mutableMapOf())
        board.placeStone(Player.Black, surroundedStone)
        board.placeStone(Player.White, Point(2, 3))
        board.placeStone(Player.White, Point(3, 2))
        board.placeStone(Player.White, Point(3, 4))

        assertEquals(
            Player.Black,
            board.get(surroundedStone),
        )

        // when get black stone
        board.placeStone(Player.White, Point(4, 3))

        // then null
        assertNull(board.get(surroundedStone))
    }
}
