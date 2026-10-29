package com.example.techmentor.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.AgendamentoResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.data.UsuarioResponse
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.home.BottomNavigationBar
import com.example.techmentor.ui.home.formatarNumero
import com.example.techmentor.ui.home.parseDataHora
import com.example.techmentor.ui.theme.ManropeFamily
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

private val Fundo = Color(0xFFF8F8FB)
private val Roxo = Color(0xFF7C3AED)
private val RoxoEscuro = Color(0xFF6D28D9)
private val RoxoClaro = Color(0xFFF1EDFB)
private val TextoEscuro = Color(0xFF17151F)
private val TextoMudo = Color(0xFF6F6A76)
private val Borda = Color(0xFFEBE8EF)

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Volta ao login se a sessão não existe mais (ex.: o banco foi reiniciado)
    LaunchedEffect(estado.sessaoExpirada) {
        if (estado.sessaoExpirada) {
            Sessao.sair()
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    // Carrega ao abrir e recarrega ao voltar para a tela
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar()
    }

    // Enquanto a tela está aberta, atualiza sozinha a cada 30 segundos
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(30_000)
                viewModel.carregar()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fundo)
            .statusBarsPadding()
    ) {

        TopBarMarca()

        Box(
            modifier = Modifier.weight(1f)
        ) {

            val usuario = estado.usuario

            when {
                usuario != null -> ProfileContent(
                    estado = estado,
                    usuario = usuario,
                    navController = navController
                )

                estado.carregando || estado.sessaoExpirada -> ProfileCarregando()

                else -> ProfileErro(
                    mensagem = estado.erro ?: "Não foi possível carregar o perfil.",
                    onTentarDeNovo = { viewModel.carregar() }
                )
            }
        }

        BottomNavigationBar(
            navController = navController,
            rotaAtual = "profile"
        )
    }
}


// ======================================================
// CONTEÚDO
// ======================================================

@Composable
private fun ProfileContent(
    estado: ProfileUiState,
    usuario: UsuarioResponse,
    navController: NavController
) {

    var editando by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 23.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        AvatarPerfil(nome = usuario.nome)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = usuario.nome,
            fontFamily = ManropeFamily,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            color = TextoEscuro
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = usuario.bio?.takeIf { it.isNotBlank() } ?: "Conte um pouco sobre você",
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = TextoMudo
        )

        Spacer(modifier = Modifier.height(15.dp))

        BotaoEditar(
            editando = editando,
            onClick = { editando = !editando }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // (Parte 5: o card de edição entra aqui)

        Estatisticas(
            xp = usuario.xpTotal,
            nivel = usuario.nivel,
            medalhas = estado.totalConquistas
        )

        Spacer(modifier = Modifier.height(11.dp))

        AreasDeInteresse(areas = usuario.areasInteresse.orEmpty())

        Spacer(modifier = Modifier.height(10.dp))

        MinhaEvolucao(
            usuario = usuario,
            estado = estado,
            onAbrirConquistas = { navController.navigate("achievements") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        HistoricoDeMentorias(lista = estado.historicoMentorias)
    }
}

@Composable
private fun AvatarPerfil(nome: String) {

    val forma = RoundedCornerShape(31.dp)

    Box(
        modifier = Modifier
            .size(94.dp)
            .shadow(
                elevation = 10.dp,
                shape = forma,
                ambientColor = Color(0xFF281849).copy(alpha = 0.12f),
                spotColor = Color(0xFF281849).copy(alpha = 0.12f)
            )
            .clip(forma)
            .background(Color.White)
            .padding(4.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(Color(0xFFEDE9FE)),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = nome.trim().firstOrNull()?.uppercase() ?: "?",
            fontFamily = ManropeFamily,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = RoxoEscuro
        )
    }
}

@Composable
private fun BotaoEditar(
    editando: Boolean,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(11.dp)

    Box(
        modifier = Modifier
            .height(33.dp)
            .clip(forma)
            .border(BorderStroke(1.dp, Roxo), forma)
            .clickable { onClick() }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = if (editando) "✎  Concluir edição" else "✎  Editar perfil",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = RoxoEscuro
        )
    }
}

@Composable
private fun Estatisticas(
    xp: Int,
    nivel: Int,
    medalhas: Int
) {

    val forma = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFE9E6ED)), forma)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        CelulaDeNumero(
            modifier = Modifier.weight(1f),
            valor = formatarNumero(xp),
            rotulo = "XP total"
        )

        DivisorVertical()

        CelulaDeNumero(
            modifier = Modifier.weight(1f),
            valor = nivel.toString(),
            rotulo = "nível"
        )

        DivisorVertical()

        CelulaDeNumero(
            modifier = Modifier.weight(1f),
            valor = medalhas.toString(),
            rotulo = "medalhas"
        )
    }
}

@Composable
private fun CelulaDeNumero(
    modifier: Modifier,
    valor: String,
    rotulo: String
) {

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = valor,
            fontFamily = ManropeFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = RoxoEscuro
        )

        Text(
            text = rotulo,
            fontSize = 10.sp,
            color = Color(0xFF928D98)
        )
    }
}

@Composable
private fun DivisorVertical() {

    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(Color(0xFFE9E6ED))
    )
}


// ======================================================
// SEÇÕES BRANCAS
// ======================================================

@Composable
private fun SecaoBranca(
    titulo: String,
    conteudo: @Composable ColumnScope.() -> Unit
) {

    val forma = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(BorderStroke(1.dp, Borda), forma)
            .padding(17.dp)
    ) {

        Text(
            text = titulo,
            fontFamily = ManropeFamily,
            fontSize = 14.sp,
            color = TextoEscuro
        )

        Spacer(modifier = Modifier.height(10.dp))

        conteudo()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AreasDeInteresse(areas: List<String>) {

    SecaoBranca(titulo = "Áreas de interesse") {

        if (areas.isEmpty()) {

            Text(
                text = "Nenhuma área ainda. Toque em Editar perfil para adicionar.",
                fontSize = 11.sp,
                color = TextoMudo
            )

        } else {

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                areas.forEach { area ->
                    EtiquetaDeArea(texto = area)
                }
            }
        }
    }
}

@Composable
private fun EtiquetaDeArea(texto: String) {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(RoxoClaro)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {

        Text(
            text = texto,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = RoxoEscuro
        )
    }
}

@Composable
private fun MinhaEvolucao(
    usuario: UsuarioResponse,
    estado: ProfileUiState,
    onAbrirConquistas: () -> Unit
) {

    SecaoBranca(titulo = "Minha evolução") {

        LinhaDeEvolucao(
            simbolo = "✦",
            corDoSimbolo = RoxoEscuro,
            fundoDoSimbolo = Color(0xFFEDE9FE),
            rotulo = "NÍVEL",
            valor = if (usuario.tituloNivel.isNullOrBlank()) {
                "Nível ${usuario.nivel}"
            } else {
                "Nível ${usuario.nivel} · ${usuario.tituloNivel}"
            }
        )

        DivisorHorizontal()

        LinhaDeEvolucao(
            simbolo = "▣",
            corDoSimbolo = Color(0xFF2563EB),
            fundoDoSimbolo = Color(0xFFE6EFFF),
            rotulo = "CURSO CONCLUÍDO",
            valor = estado.ultimoCursoConcluido?.tituloCurso ?: "Nenhum curso concluído ainda"
        )

        DivisorHorizontal()

        LinhaDeEvolucao(
            simbolo = "★",
            corDoSimbolo = Color(0xFFD97706),
            fundoDoSimbolo = Color(0xFFFEF3C7),
            rotulo = "CONQUISTAS",
            valor = textoDeMedalhas(estado.totalConquistas),
            comSeta = true,
            onClick = onAbrirConquistas
        )
    }
}

@Composable
private fun LinhaDeEvolucao(
    simbolo: String,
    corDoSimbolo: Color,
    fundoDoSimbolo: Color,
    rotulo: String,
    valor: String,
    comSeta: Boolean = false,
    onClick: (() -> Unit)? = null
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(fundoDoSimbolo),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = simbolo,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = corDoSimbolo
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = rotulo,
                fontSize = 10.sp,
                color = Color(0xFF98939D)
            )

            Text(
                text = valor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextoEscuro,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (comSeta) {
            Text(
                text = "›",
                fontSize = 22.sp,
                color = TextoEscuro
            )
        }
    }
}

@Composable
private fun DivisorHorizontal() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFF0EDF3))
    )
}

@Composable
private fun HistoricoDeMentorias(lista: List<AgendamentoResponse>) {

    SecaoBranca(titulo = "Histórico de mentorias") {

        if (lista.isEmpty()) {

            Text(
                text = "Você ainda não agendou mentorias.",
                fontSize = 11.sp,
                color = TextoMudo
            )

        } else {

            lista.forEachIndexed { indice, agendamento ->

                if (indice > 0) {
                    DivisorHorizontal()
                }

                LinhaDeMentoria(agendamento = agendamento)
            }
        }
    }
}

@Composable
private fun LinhaDeMentoria(agendamento: AgendamentoResponse) {

    val estilo = estiloDoStatus(agendamento.status)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(41.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFEDE9FE)),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = agendamento.mentoraNome.trim().firstOrNull()?.uppercase() ?: "?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = RoxoEscuro
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = agendamento.mentoraNome,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextoEscuro,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!agendamento.assunto.isNullOrBlank()) {
                Text(
                    text = agendamento.assunto,
                    fontSize = 10.sp,
                    color = Color(0xFF77727E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = formatarDataLonga(agendamento.inicio),
                fontSize = 10.sp,
                color = Color(0xFFA19CA5)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(estilo.fundo)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {

            Text(
                text = estilo.rotulo,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = estilo.texto
            )
        }
    }
}

private data class EstiloDoStatus(
    val rotulo: String,
    val fundo: Color,
    val texto: Color
)

private fun estiloDoStatus(status: String): EstiloDoStatus = when (status) {
    "CONCLUIDA" -> EstiloDoStatus("Concluída", Color(0xFFDCFCE7), Color(0xFF15803D))
    "CONFIRMADA" -> EstiloDoStatus("Confirmada", Color(0xFFE6EFFF), Color(0xFF2563EB))
    "AGENDADA" -> EstiloDoStatus("Agendada", Color(0xFFEDE9FE), Color(0xFF6D28D9))
    "CANCELADA" -> EstiloDoStatus("Cancelada", Color(0xFFFEE2E2), Color(0xFFB91C1C))
    else -> EstiloDoStatus(status, Color(0xFFF1F0F4), Color(0xFF6F6A76))
}

/** Ex.: "7 de outubro de 2026" */
private fun formatarDataLonga(inicio: String): String {
    val data = parseDataHora(inicio) ?: return inicio
    return SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR")).format(data)
}

private fun textoDeMedalhas(total: Int): String = when (total) {
    0 -> "Nenhuma medalha ainda"
    1 -> "1 medalha desbloqueada"
    else -> "$total medalhas desbloqueadas"
}


// ======================================================
// CARREGANDO E ERRO
// ======================================================

@Composable
private fun ProfileCarregando() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 23.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Box(
            modifier = Modifier
                .size(94.dp)
                .clip(RoundedCornerShape(31.dp))
                .background(Color(0xFFECEAF2))
        )

        EsqueletoBloco(altura = 64.dp)
        EsqueletoBloco(altura = 90.dp)
        EsqueletoBloco(altura = 150.dp)
    }
}

@Composable
private fun EsqueletoBloco(altura: androidx.compose.ui.unit.Dp) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(altura)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFECEAF2))
    )
}

@Composable
private fun ProfileErro(
    mensagem: String,
    onTentarDeNovo: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Ops!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF20202A)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = mensagem,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF797486)
        )

        Spacer(modifier = Modifier.height(24.dp))

        GradientButton(
            text = "Tentar novamente",
            onClick = onTentarDeNovo
        )
    }
}