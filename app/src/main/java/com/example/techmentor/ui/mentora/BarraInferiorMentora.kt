package com.example.techmentor.ui.mentora

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.home.BottomNavItem

/** Barra inferior da mentora: Início, Agenda e Perfil. */
@Composable
fun BarraInferiorMentora(
    navController: NavController,
    rotaAtual: String
) {

    fun irPara(rota: String) {
        navController.navigate(rota) {
            popUpTo("mentora") { inclusive = false }
            launchSingleTop = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE9E7EE))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(76.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {

            BottomNavItem(
                icone = Icone.CASA,
                label = "Início",
                selected = rotaAtual == "mentora",
                onClick = { irPara("mentora") }
            )

            BottomNavItem(
                icone = Icone.LIVRO,
                label = "Agenda",
                selected = rotaAtual == "mentora/agenda",
                onClick = { irPara("mentora/agenda") }
            )

            BottomNavItem(
                icone = Icone.PERFIL,
                label = "Perfil",
                selected = rotaAtual == "mentora/perfil",
                onClick = { irPara("mentora/perfil") }
            )
        }
    }
}