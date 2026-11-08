package com.example.techmentor.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.techmentor.ui.admin.AdminDenunciaDetalheScreen
import com.example.techmentor.ui.admin.AdminDenunciasScreen
import com.example.techmentor.ui.admin.AdminMentoraFormScreen
import com.example.techmentor.ui.admin.AdminMentorasScreen
import com.example.techmentor.ui.admin.AdminCandidaturasScreen
import com.example.techmentor.ui.admin.AdminOportunidadeFormScreen
import com.example.techmentor.ui.admin.AdminOportunidadesScreen
import com.example.techmentor.ui.admin.AdminPainelScreen
import com.example.techmentor.ui.admin.AdminPerfilScreen

/**
 * Telas da administradora. Ficam em um arquivo próprio para não dar conflito no NavGraph:
 * lá só existe a chamada adminNavGraph(navController) dentro do NavHost.
 */
fun NavGraphBuilder.adminNavGraph(navController: NavController) {

    composable("admin") {
        AdminPainelScreen(navController = navController)
    }

    // ---- Denúncias ----

    composable("admin/denuncias") {
        AdminDenunciasScreen(navController = navController)
    }

    composable(
        route = "admin/denuncias/{denunciaId}",
        arguments = listOf(navArgument("denunciaId") { type = NavType.LongType })
    ) { entrada ->
        AdminDenunciaDetalheScreen(
            navController = navController,
            denunciaId = entrada.arguments?.getLong("denunciaId") ?: 0L
        )
    }

    // ---- Oportunidades ("nova" não é número, então não se confunde com o {oportunidadeId}) ----

    composable("admin/oportunidades") {
        AdminOportunidadesScreen(navController = navController)
    }

    composable("admin/oportunidades/nova") {
        AdminOportunidadeFormScreen(navController = navController, oportunidadeId = null)
    }

    composable(
        route = "admin/oportunidades/{oportunidadeId}",
        arguments = listOf(navArgument("oportunidadeId") { type = NavType.LongType })
    ) { entrada ->
        AdminOportunidadeFormScreen(
            navController = navController,
            oportunidadeId = entrada.arguments?.getLong("oportunidadeId")
        )
    }

    composable(
        route = "admin/oportunidades/{oportunidadeId}/candidaturas",
        arguments = listOf(navArgument("oportunidadeId") { type = NavType.LongType })
    ) { entrada ->
        AdminCandidaturasScreen(
            navController = navController,
            oportunidadeId = entrada.arguments?.getLong("oportunidadeId") ?: 0L
        )
    }

    // ---- Mentoras ----

    composable("admin/mentoras") {
        AdminMentorasScreen(navController = navController)
    }

    composable("admin/mentoras/nova") {
        AdminMentoraFormScreen(navController = navController, mentoraId = null)
    }

    composable(
        route = "admin/mentoras/{mentoraId}",
        arguments = listOf(navArgument("mentoraId") { type = NavType.LongType })
    ) { entrada ->
        AdminMentoraFormScreen(
            navController = navController,
            mentoraId = entrada.arguments?.getLong("mentoraId")
        )
    }

    // ---- Perfil ----

    composable("admin/perfil") {
        AdminPerfilScreen(navController = navController)
    }
}
