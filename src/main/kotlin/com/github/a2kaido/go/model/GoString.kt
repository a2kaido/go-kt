package com.github.a2kaido.go.model

class GoString(
    val color: Player,
    val stones: List<Point>,
    val liberties: List<Point>,
) {
    fun withoutLiberty(point: Point) = GoString(
        color = color,
        stones = stones,
        liberties = liberties - point,
    )

    fun withLiberty(point: Point) = GoString(
        color = color,
        stones = stones,
        liberties = liberties + point,
    )

//    fun addLiberty(point: Point) {
//        liberties.add(point)
//    }
//
//    fun removeLiberty(point: Point) {
//        liberties.remove(point)
//    }

    fun mergedWith(goString: GoString) : GoString {
        assert(goString.color == this.color)

        val combinedStones = stones + goString.stones
        return GoString(
            color = this.color,
            stones = combinedStones,
            liberties = ((liberties + goString.liberties).distinct() - combinedStones.toSet())
                .toMutableList(),
        )
    }

    fun numLiberties() = liberties.size

    fun copy() = GoString(
        color = color,
        stones = stones.toMutableList(),
        liberties = liberties.toMutableList(),
    )
}
