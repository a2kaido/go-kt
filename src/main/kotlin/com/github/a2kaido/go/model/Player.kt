package com.github.a2kaido.go.model

enum class Player {
    Black,
    White,
    ;

    fun other() = when (this) {
        Black -> White
        White -> Black
    }
}
