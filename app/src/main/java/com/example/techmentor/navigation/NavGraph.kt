package com.example.techmentor.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.techmentor.ui.home.HomeScreen
import com.example.techmentor.ui.login.LoginScreen
import com.example.techmentor.ui.register.RegisterScreen
import androidx.compose.runtime.remember
import com.example.techmentor.data.Sessao

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    // Quem já entrou antes abre direto na Home
    val inicio = remember { if (Sessao.usuarioId != null) "home" else "login" }

    NavHost(
        navController = navController,
        startDestination = inicio
    ) {
        composable("login") {
            LoginScreen(navController = navController)
        }

        composable("register") {
            RegisterScreen(navController = navController)
        }

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
        composable("achievements") {
            PlaceholderScreen("Conquistas")
        }

        composable("safety") {
            PlaceholderScreen("Segurança")
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Text(text = title)
}
