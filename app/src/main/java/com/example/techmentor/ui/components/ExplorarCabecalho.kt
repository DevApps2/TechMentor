package com.example.techmentor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.techmentor.ui.theme.ManropeFamily

/** As três abas da área "Explorar" do protótipo. Cada uma tem a sua rota e o seu título. */
enum class AbaExplorar(
    val rotulo: String,
    val rota: String,
    val titulo: String,
    val subtitulo: String
) {
    MENTORIA("Mentoria", "mentorship", "Encontre sua mentora", "Conecte-se com mulheres que inspiram."),
    OPORTUNIDADES("Oportunidades", "opportunities", "Oportunidades", "Seu próximo passo começa aqui."),
    SEGURANCA("Segurança", "safety", "Segurança e ajuda", "Conheça os recursos de cuidado.")
}

/**
 * Cabeçalho da área Explorar, igual ao do protótipo: "EXPLORE POSSIBILIDADES", o título da aba
 * e a faixa de abas Mentoria | Oportunidades | Segurança. Fica logo abaixo do [TopBarMarca].
 */
@Composable
fun ExplorarCabecalho(
    navController: NavController,
    aba: AbaExplorar
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp)
    ) {

        Text(
            text = "EXPLORE POSSIBILIDADES",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.4.sp,
            color = Color(0xFF7C3AED)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = aba.titulo,
            fontFamily = ManropeFamily,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
            color = Color(0xFF17151F)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = aba.subtitulo,
            fontSize = 13.sp,
            color = Color(0xFF797486)
        )

        Spacer(modifier = Modifier.height(14.dp))

        FaixaDeAbas(
            navController = navController,
            ativa = aba
        )
    }
}

@Composable
private fun FaixaDeAbas(
    navController: NavController,
    ativa: AbaExplorar
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEDEBF2))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        AbaExplorar.entries.forEach { aba ->

            val selecionada = aba == ativa

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selecionada) Color.White else Color.Transparent)
                    .clickable(role = Role.Tab, enabled = !selecionada) {
                        navController.navigate(aba.rota) {
                            popUpTo("home")
                            launchSingleTop = true
                        }
                    }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = aba.rotulo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selecionada) Color(0xFF7C3AED) else Color(0xFF797486),
                    maxLines = 1
                )
            }
        }
    }
}

/** Aviso de "dados de demonstração", com a faixa roxa na esquerda (igual ao card "Próximo passo" da Home). */
@Composable
fun AvisoDemo(
    texto: String,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF1EEFB))
    ) {

        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color(0xFF7C3AED))
        )

        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = Color(0xFF625D6C)
        )
    }
}
