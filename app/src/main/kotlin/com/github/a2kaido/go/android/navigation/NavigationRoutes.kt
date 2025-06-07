package com.github.a2kaido.go.android.navigation

sealed class NavigationRoutes(val route: String) {
    data object MainMenu : NavigationRoutes("main_menu")
    data object GameSetup : NavigationRoutes("game_setup")
    data object Game : NavigationRoutes("game")
    data object Settings : NavigationRoutes("settings")
    data object GameOver : NavigationRoutes("game_over/{winner}/{score}") {
        fun createRoute(winner: String, score: String): String {
            return "game_over/$winner/$score"
        }
    }
}