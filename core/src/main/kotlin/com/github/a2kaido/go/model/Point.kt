package com.github.a2kaido.go.model

data class Point(
    val row: Int,
    val col: Int,
){
    fun neighbors() = listOf(
        Point(row - 1, col),
        Point(row, col - 1),
        Point(row + 1, col),
        Point(row, col + 1),
    )
}
