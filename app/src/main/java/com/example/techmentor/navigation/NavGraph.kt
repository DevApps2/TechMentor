package com.example.techmentor.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.techmentor.ui.home.HomeScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }

        composable("learning") {
            PlaceholderScreen("Aprendizado")
        }

        composable("mentorship") {
            PlaceholderScreen("Mentoria")
        }

        composable("opportunities") {
            PlaceholderScreen("Oportunidades")
        }

        composable("achievements") {
            PlaceholderScreen("Conquistas")
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Text(text = title)
}