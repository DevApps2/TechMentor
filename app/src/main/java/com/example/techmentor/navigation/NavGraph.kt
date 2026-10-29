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
import com.example.techmentor.ui.profile.ProfileScreen
import com.example.techmentor.ui.splash.SplashScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    // Depois da abertura, quem já entrou antes vai direto para a Home
    val destinoAposAbertura = remember { if (Sessao.usuarioId != null) "home" else "login" }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onTerminar = {
                    navController.navigate(destinoAposAbertura) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

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

        composable("safety") {
            PlaceholderScreen("Segurança")
        }

        composable("profile") {
            ProfileScreen(navController = navController)
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Text(text = title)
}
