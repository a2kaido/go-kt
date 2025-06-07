package com.github.a2kaido.go

import com.github.a2kaido.go.model.Player
import com.github.a2kaido.go.model.Point
import kotlin.random.Random

fun main() {
    val MAX63 = 0x7fffffffffffffff
    val table = mutableMapOf<Pair<Point, Player>, Long>()

    (1..20).forEach { row ->
        (1..20).forEach { col ->
            listOf(Player.Black, Player.White).forEach { color ->
                val code = Random.nextLong(0, MAX63)
                table[Point(row, col) to color] = code
            }
        }
    }

    table.forEach { (point, color), code ->
        println("Point(${point.row}, ${point.col}) to Player.${color.name} to $code,")
    }
}
