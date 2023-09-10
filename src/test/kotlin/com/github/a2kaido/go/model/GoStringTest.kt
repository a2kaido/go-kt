package com.github.a2kaido.go.model

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class GoStringTest {

    @Test
    fun `mergeWith same color`() {
        val goString1 = GoString(
            color = Player.Black,
            stones = mutableListOf(Point(1, 1)),
            liberties = mutableListOf(
                Point(0, 1),
                Point(1, 0),
                Point(1, 2),
                Point(2, 1),
            )
        )

        val goString2 = GoString(
            color = Player.Black,
            stones = mutableListOf(Point(2, 1)),
            liberties = mutableListOf(
                Point(1, 1),
                Point(2, 0),
                Point(2, 2),
                Point(3, 1),
            )
        )

        val mergedString = goString1.mergedWith(goString2)
        assertEquals(
            listOf(Point(1, 1), Point(2, 1)),
            mergedString.stones,
        )
        assertEquals(
            6,
            mergedString.numLiberties(),
        )
    }

    @Test
    fun `mergeWith other color`() {
        val goString1 = GoString(
            color = Player.Black,
            stones = mutableListOf(Point(1, 1)),
            liberties = mutableListOf(
                Point(0, 1),
                Point(1, 0),
                Point(1, 2),
                Point(2, 1),
            )
        )

        val goString2 = GoString(
            color = Player.White,
            stones = mutableListOf(Point(2, 1)),
            liberties = mutableListOf(
                Point(1, 1),
                Point(2, 0),
                Point(2, 2),
                Point(3, 1),
            )
        )

        assertThrows<AssertionError> {
            goString1.mergedWith(goString2)
        }
    }
}
