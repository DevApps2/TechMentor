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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.auth.OutlinedAuthButton
import com.example.techmentor.ui.components.AbaExplorar
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.components.ExplorarCabecalho
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha
import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.home.BottomNavigationBar
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily

/** As "páginas" da Segurança. Ficam todas aqui dentro, sem precisar de rota nova no NavGraph. */
internal enum class Destino {
    HOME, DENUNCIAR, MINHAS_DENUNCIAS, BLOQUEADOS, ORIENTACOES, CANAIS
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

    // Volta ao login se a sessão não existe mais (ex.: o banco foi reiniciado)
    LaunchedEffect(estado.sessaoExpirada) {
        if (estado.sessaoExpirada) {
            Sessao.sair()
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    // Carrega ao abrir e ao voltar para a tela; fora isso, só puxando para atualizar
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar()
    }

    // Com a tela aberta, atualiza sozinha de tempos em tempos
    AtualizarAutomaticamente { viewModel.carregar() }

    val voltar = {
        viewModel.limparAcao()
        destino = Destino.HOME
    }

    // Botão voltar do celular: das subtelas volta para a Segurança
    if (destino != Destino.HOME) {
        BackHandler { voltar() }
    }

    when (destino) {

        Destino.HOME -> SegurancaInicio(
            navController = navController,
            estado = estado,
            onAtualizar = { viewModel.carregar(manual = true) },
            onIrPara = { destino = it }
        )

        Destino.DENUNCIAR -> DenunciarUsuarioTela(
            navController = navController,
            estado = estado,
            onBuscar = { viewModel.buscarUsuarios(it) },
            onEnviar = { usuarioId, motivo, descricao -> viewModel.denunciar(usuarioId, motivo, descricao) },
            onBloquear = { usuarioId, aoTerminar -> viewModel.bloquear(usuarioId, aoTerminar) },
            onVoltar = voltar
        )

        Destino.MINHAS_DENUNCIAS -> MinhasDenunciasTela(
            navController = navController,
            estado = estado,
            onVoltar = voltar
        )

        Destino.BLOQUEADOS -> UsuariosBloqueadosTela(
            navController = navController,
            estado = estado,
            onBuscar = { viewModel.buscarUsuarios(it) },
            onBloquear = { usuarioId, aoTerminar -> viewModel.bloquear(usuarioId, aoTerminar) },
            onDesbloquear = { viewModel.desbloquear(it) },
            onVoltar = voltar
        )

        Destino.ORIENTACOES -> OrientacoesTela(
            navController = navController,
            estado = estado,
            onTentarDeNovo = { viewModel.carregar() },
            onVoltar = voltar
        )

        Destino.CANAIS -> CanaisDeAjudaTela(
            navController = navController,
            estado = estado,
            onTentarDeNovo = { viewModel.carregar() },
            onVoltar = voltar
        )
    }
}


// ======================================================
// INÍCIO DA SEGURANÇA
// ======================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegurancaInicio(
    navController: NavController,
    estado: SegurancaUiState,
    onAtualizar: () -> Unit,
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

        TopBarMarca(navController = navController)

        ExplorarCabecalho(
            navController = navController,
            aba = AbaExplorar.SEGURANCA
        )

        PullToRefreshBox(
            isRefreshing = estado.atualizando,
            onRefresh = onAtualizar,
            modifier = Modifier.weight(1f)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                CartaoDestaque()

                LinhaDeAcao(
                    titulo = "Denunciar usuário",
                    subtitulo = "Registre um relato com segurança",
                    fundoIcone = Color(0xFFFCE7F3),
                    icone = { IconeBandeira(cor = Color(0xFFDB2777)) },
                    onClick = { onIrPara(Destino.DENUNCIAR) }
                )

                LinhaDeAcao(
                    titulo = "Minhas denúncias",
                    subtitulo = estado.minhasDenuncias?.let {
                        if (it.isEmpty()) "Você ainda não fez nenhuma" else "${it.size} registrada(s)"
                    } ?: "Acompanhe o andamento",
                    fundoIcone = Color(0xFFFEF3C7),
                    icone = { IconeBandeira(cor = Color(0xFFD97706)) },
                    onClick = { onIrPara(Destino.MINHAS_DENUNCIAS) }
                )

                LinhaDeAcao(
                    titulo = "Usuários bloqueados",
                    subtitulo = if (estado.bloqueados.isEmpty()) {
                        "Gerencie quem você bloqueou"
                    } else {
                        "${estado.bloqueados.size} bloqueado(s)"
                    },
                    fundoIcone = Color(0xFFE6EFFF),
                    icone = { IconeLinha(icone = Icone.USUARIOS, cor = Color(0xFF2563EB), tamanho = 22.dp) },
                    onClick = { onIrPara(Destino.BLOQUEADOS) }
                )

                LinhaDeAcao(
                    titulo = "Orientações de segurança",
                    subtitulo = "Dicas para uma experiência positiva",
                    fundoIcone = Color(0xFFEDE9FE),
                    icone = { IconeLinha(icone = Icone.LIVRO, cor = Color(0xFF7C3AED), tamanho = 22.dp) },
                    onClick = { onIrPara(Destino.ORIENTACOES) }
                )

                CartaoDicas(dicas = dicas)

                OutlinedAuthButton(
                    text = "Ver canais de ajuda  ›",
                    onClick = { onIrPara(Destino.CANAIS) }
                )
            }
        }

        BottomNavigationBar(
            navController = navController,
            rotaAtual = "safety"
        )
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
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF33298A), Color(0xFF5B21B6))))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            IconeLinha(icone = Icone.ESCUDO, cor = Color.White, tamanho = 26.dp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {

            Text(
                text = "Um espaço de cuidado",
                fontFamily = ManropeFamily,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Conheça os recursos de segurança do TechMentor.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
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

    val forma = RoundedCornerShape(17.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(fundoIcone),
            contentAlignment = Alignment.Center
        ) {
            icone()
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {

            Text(
                text = titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20202A)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitulo,
                fontSize = 12.sp,
                color = MentoriaCores.TextoMudo
            )
        }

        IconeLinha(
            icone = Icone.SETA,
            cor = MentoriaCores.TextoEscuro,
            tamanho = 17.dp
        )
    }
}

@Composable
private fun CartaoDicas(dicas: List<String>) {

    val forma = RoundedCornerShape(17.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color(0xFFFFFBE6))
            .border(1.dp, Color(0xFFF8E7A0), forma)
            .padding(14.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            IconeCadeado(cor = Color(0xFF92400E))

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Cuide das suas informações",
                fontFamily = ManropeFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF92400E)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        dicas.forEach { dica ->
            Text(
                text = dica,
                modifier = Modifier.padding(start = 28.dp, top = 3.dp),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFF6B4E16)
            )
        }
    }
}
