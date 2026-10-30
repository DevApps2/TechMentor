package com.example.techmentor.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.techmentor.ui.home.HomeScreen
import com.example.techmentor.ui.login.LoginScreen
import com.example.techmentor.ui.mentoria.AgendamentoScreen
import com.example.techmentor.ui.mentoria.MentoraPerfilScreen
import com.example.techmentor.ui.mentoria.MentoriaScreen
import com.example.techmentor.ui.register.RegisterScreen
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.esquecisenha.EsqueciSenhaScreen
import com.example.techmentor.ui.esquecisenha.RedefinirSenhaScreen
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
            MentoriaScreen(navController = navController)
        }

        composable(
            route = "mentorship/{mentoraId}",
            arguments = listOf(navArgument("mentoraId") { type = NavType.LongType })
        ) { entrada ->
            MentoraPerfilScreen(
                navController = navController,
                mentoraId = entrada.arguments?.getLong("mentoraId") ?: 0L
            )
        }

        composable(
            route = "mentorship/{mentoraId}/agendar",
            arguments = listOf(navArgument("mentoraId") { type = NavType.LongType })
        ) { entrada ->
            AgendamentoScreen(
                navController = navController,
                mentoraId = entrada.arguments?.getLong("mentoraId") ?: 0L
            )
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

        composable("esqueci-senha") {
            EsqueciSenhaScreen(navController = navController)
        }

        composable(
            route = "redefinir-senha/{email}",
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { entrada ->
            RedefinirSenhaScreen(
                navController = navController,
                email = entrada.arguments?.getString("email").orEmpty()
            )
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Text(text = title)
}
