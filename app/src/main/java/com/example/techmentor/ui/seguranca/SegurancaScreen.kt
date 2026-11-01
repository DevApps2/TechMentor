package com.example.techmentor.ui.seguranca

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha
import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.home.BottomNavigationBar
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily

/** As "páginas" da Segurança. Ficam todas aqui dentro, sem precisar de rota nova no NavGraph. */
internal enum class Destino {
    HOME, DENUNCIAR, BLOQUEADOS, ORIENTACOES, CANAIS
}

private val DICAS_PADRAO = listOf(
    "Nunca compartilhe senhas ou dados bancários.",
    "Prefira conversar dentro da plataforma.",
    "Confie nos seus instintos e denuncie abusos."
)


@Composable
fun SegurancaScreen(
    navController: NavController,
    viewModel: SegurancaViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    var destino by rememberSaveable { mutableStateOf(Destino.HOME) }

    // Botão voltar do celular: das subtelas volta para a Segurança
    if (destino != Destino.HOME) {
        BackHandler { destino = Destino.HOME }
    }

    val voltar = { destino = Destino.HOME }

    when (destino) {

        Destino.HOME -> SegurancaInicio(
            navController = navController,
            estado = estado,
            onIrPara = { destino = it }
        )

        Destino.DENUNCIAR -> DenunciarUsuarioTela(onVoltar = voltar)

        Destino.BLOQUEADOS -> UsuariosBloqueadosTela(
            bloqueados = estado.bloqueados,
            onBloquear = { viewModel.bloquear(it) },
            onDesbloquear = { viewModel.desbloquear(it) },
            onVoltar = voltar
        )

        Destino.ORIENTACOES -> OrientacoesTela(
            estado = estado,
            onTentarDeNovo = { viewModel.carregar() },
            onVoltar = voltar
        )

        Destino.CANAIS -> CanaisDeAjudaTela(
            estado = estado,
            onTentarDeNovo = { viewModel.carregar() },
            onVoltar = voltar
        )
    }
}


// ======================================================
// INÍCIO DA SEGURANÇA
// ======================================================

@Composable
private fun SegurancaInicio(
    navController: NavController,
    estado: SegurancaUiState,
    onIrPara: (Destino) -> Unit
) {

    // O card amarelo usa o texto do servidor; se não vier, usa o texto padrão do Figma
    val dicas = estado.orientacoes
        .firstOrNull { it.titulo.equals("Cuide das suas informações", ignoreCase = true) }
        ?.descricao
        ?.split(Regex("(?<=\\.)\\s+"))
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.takeIf { it.isNotEmpty() }
        ?: DICAS_PADRAO

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        TopBarMarca()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Column {
                Text(
                    text = "EXPLORE POSSIBILIDADES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MentoriaCores.Roxo
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Segurança e ajuda",
                    fontFamily = ManropeFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.8).sp,
                    color = MentoriaCores.TextoEscuro
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Conheça os recursos de cuidado.",
                    fontSize = 15.sp,
                    color = MentoriaCores.TextoMudo
                )
            }

            AbasExplorarSeguranca(navController = navController)

            CartaoDestaque()

            AvisoLateral(texto = "Fluxos demonstrativos: não há moderação ativa nem envio de denúncias.")

            LinhaDeAcao(
                titulo = "Denunciar usuário",
                subtitulo = "Simule um relato com segurança",
                fundoIcone = Color(0xFFFCE7F3),
                icone = { IconeBandeira(cor = Color(0xFFDB2777), tamanho = 26.dp) },
                onClick = { onIrPara(Destino.DENUNCIAR) }
            )

            LinhaDeAcao(
                titulo = "Usuários bloqueados",
                subtitulo = "Gerencie sua lista demonstrativa",
                fundoIcone = Color(0xFFE3EEFF),
                icone = { IconeLinha(icone = Icone.USUARIOS, cor = Color(0xFF2563EB), tamanho = 26.dp) },
                onClick = { onIrPara(Destino.BLOQUEADOS) }
            )

            LinhaDeAcao(
                titulo = "Orientações de segurança",
                subtitulo = "Dicas para uma experiência positiva",
                fundoIcone = Color(0xFFEDE9FE),
                icone = { IconeLinha(icone = Icone.LIVRO, cor = Color(0xFF7C3AED), tamanho = 26.dp) },
                onClick = { onIrPara(Destino.ORIENTACOES) }
            )

            CartaoDicas(dicas = dicas)

            BotaoContorno(
                texto = "Ver canais de ajuda  ›",
                onClick = { onIrPara(Destino.CANAIS) }
            )
        }

        BottomNavigationBar(
            navController = navController,
            rotaAtual = "safety"
        )
    }
}


// ======================================================
// ABAS: Mentoria | Oportunidades | Segurança
// ======================================================

@Composable
private fun AbasExplorarSeguranca(navController: NavController) {

    val abas = listOf(
        "Mentoria" to "mentorship",
        "Oportunidades" to "opportunities",
        "Segurança" to "safety"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFEDEBF2))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        abas.forEach { (texto, rota) ->

            val ativa = rota == "safety"

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (ativa) Color.White else Color.Transparent)
                    .clickable(role = Role.Tab, enabled = !ativa) {
                        navController.navigate(rota) {
                            popUpTo("home")
                            launchSingleTop = true
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = texto,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (ativa) MentoriaCores.Roxo else MentoriaCores.TextoMudo,
                    maxLines = 1
                )
            }
        }
    }
}


// ======================================================
// BLOCOS DO INÍCIO
// ======================================================

@Composable
private fun CartaoDestaque() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF33298A), Color(0xFF5B21B6))))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            IconeLinha(icone = Icone.ESCUDO, cor = Color.White, tamanho = 40.dp)
        }

        Spacer(modifier = Modifier.width(18.dp))

        Column {

            Text(
                text = "Um espaço de cuidado",
                fontFamily = ManropeFamily,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Conheça os fluxos de segurança deste protótipo.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFFE0D7FA)
            )
        }
    }
}

@Composable
private fun LinhaDeAcao(
    titulo: String,
    subtitulo: String,
    fundoIcone: Color,
    icone: @Composable () -> Unit,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(24.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(fundoIcone),
            contentAlignment = Alignment.Center
        ) {
            icone()
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {

            Text(
                text = titulo,
                fontFamily = ManropeFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MentoriaCores.TextoEscuro
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitulo,
                fontSize = 14.sp,
                color = MentoriaCores.TextoMudo
            )
        }

        Text(
            text = "›",
            fontSize = 30.sp,
            color = MentoriaCores.TextoEscuro
        )
    }
}

@Composable
private fun CartaoDicas(dicas: List<String>) {

    val forma = RoundedCornerShape(24.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color(0xFFFFFBE6))
            .border(1.dp, Color(0xFFF8E7A0), forma)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            IconeCadeado(cor = Color(0xFF92400E), tamanho = 26.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Cuide das suas informações",
                fontFamily = ManropeFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF92400E)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        dicas.forEach { dica ->
            Text(
                text = dica,
                modifier = Modifier.padding(start = 38.dp, top = 4.dp),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color(0xFF6B4E16)
            )
        }
    }
}
