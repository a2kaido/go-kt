package com.github.a2kaido.go.model

class GoString(
    val color: Player,
    val stones: List<Point>,
    val liberties: MutableList<Point>,
) {
    fun addLiberty(point: Point) {
        liberties.add(point)
    }

    fun removeLiberty(point: Point) {
        liberties.remove(point)
    }

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
}
