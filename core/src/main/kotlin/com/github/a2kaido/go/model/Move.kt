package com.github.a2kaido.go.model

data class Move(
    val action: MoveAction,
) {
    companion object {
        fun play(point: Point) = Move(MoveAction.Play(point))
        fun pass() = Move(MoveAction.Pass)
        fun resign() = Move(MoveAction.Resign)
    }
}

sealed interface MoveAction {
    data object Pass : MoveAction
    data object Resign : MoveAction
    data class Play(val point: Point) : MoveAction
}
