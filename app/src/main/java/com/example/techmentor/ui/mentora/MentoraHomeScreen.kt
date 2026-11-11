package com.example.techmentor.ui.mentora

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.AgendamentoResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.data.Sessao.sair
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.OutlinedAuthButton
import com.example.techmentor.ui.avaliacao.AvaliacoesPendentes
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.components.BotaoEntrarNaSala
import com.example.techmentor.ui.components.BotaoPrimario
import com.example.techmentor.ui.components.BotaoSecundario
import com.example.techmentor.ui.components.SeloDeStatus
import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.home.descricaoDaMentoria
import com.example.techmentor.ui.home.parseDataHora
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LocaleBr: Locale = Locale.forLanguageTag("pt-BR")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentoraHomeScreen(
    navController: NavController,
    viewModel: MentoraHomeViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()


    var paraCancelar by remember { mutableStateOf<AgendamentoResponse?>(null) }

    var confirmandoSaida by remember { mutableStateOf(false) }

    fun sair() {
        Sessao.sair()
        navController.navigate("login") {
            // limpa tudo: a mentora não tem a "home" da aluna na pilha
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    // Volta ao login se a sessão não existe mais (ex.: o banco foi reiniciado)
    LaunchedEffect(estado.sessaoExpirada) {
        if (estado.sessaoExpirada) sair()
    }

    // Mentora sem perfil: abre o formulário de primeiro acesso
    LaunchedEffect(estado.semPerfil) {
        if (estado.semPerfil) {
            navController.navigate("mentora/completar-perfil")
        }
    }

    // Carrega ao abrir e ao voltar para a tela; fora isso, só puxando para atualizar
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar()
    }

    // Com a tela aberta, atualiza sozinha de tempos em tempos
    AtualizarAutomaticamente { viewModel.carregar() }

    // ---- Diálogo de cancelar ----

    paraCancelar?.let { mentoria ->
        AlertDialog(
            onDismissRequest = { paraCancelar = null },
            title = { Text("Cancelar esta mentoria?") },
            text = {
                Text("${mentoria.alunaNome} será avisada e o horário volta a ficar livre na sua agenda.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelar(mentoria.id)
                    paraCancelar = null
                }) {
                    Text("Cancelar mentoria", color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { paraCancelar = null }) { Text("Voltar") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        TopBarMarca(navController = navController)

        Box(
            modifier = Modifier.weight(1f)
        ) {

            val usuario = estado.usuario

            when {
                usuario != null -> PullToRefreshBox(
                    isRefreshing = estado.atualizando,
                    onRefresh = { viewModel.carregar(manual = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Conteudo(
                        nome = usuario.nome,
                        proximas = estado.proximas,
                        realizadas = estado.realizadas,
                        acaoEmAndamentoId = estado.acaoEmAndamentoId,
                        erroAcao = estado.erroAcao,
                        onConfirmar = { viewModel.confirmar(it) },
                        onReagendar = { navController.navigate("reagendar/${it.id}/${it.mentoraId}") },
                        onPedirCancelamento = { paraCancelar = it },
                        onSair = { confirmandoSaida = true },
                        avaliacoesPendentes = { AvaliacoesPendentes(navController) }
                    )
                }

                estado.carregando || estado.sessaoExpirada -> Carregando()

                else -> EstadoErro(
                    mensagem = estado.erro ?: "Não foi possível carregar.",
                    onTentarDeNovo = { viewModel.carregar() }
                )
            }
        }

        BarraInferiorMentora(
            navController = navController,
            rotaAtual = "mentora"
        )
    }
}

@Composable
private fun Conteudo(
    nome: String,
    proximas: List<AgendamentoResponse>,
    realizadas: Int,
    acaoEmAndamentoId: Long?,
    erroAcao: String?,
    onConfirmar: (Long) -> Unit,
    onReagendar: (AgendamentoResponse) -> Unit,
    onPedirCancelamento: (AgendamentoResponse) -> Unit,
    onSair: () -> Unit,
    avaliacoesPendentes: @Composable () -> Unit
) {

    val primeiroNome = nome.trim().split(" ").firstOrNull().orEmpty().ifEmpty { "você" }
    val data = SimpleDateFormat("EEEE, d 'DE' MMMM", LocaleBr).format(Date()).uppercase()
    var confirmandoSaida by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Column {

            Text(
                text = data,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8C8796)
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Olá, $primeiroNome!",
                fontFamily = ManropeFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
                color = MentoriaCores.TextoEscuro,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Veja quem vai conversar com você.",
                fontSize = 13.sp,
                color = Color(0xFF797486)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Resumo(
                modifier = Modifier.weight(1f),
                valor = proximas.size.toString(),
                rotulo = "próximas"
            )

            Resumo(
                modifier = Modifier.weight(1f),
                valor = realizadas.toString(),
                rotulo = "realizadas"
            )
        }

        avaliacoesPendentes()

        Text(
            text = "Próximas mentorias",
            modifier = Modifier.padding(top = 6.dp),
            fontFamily = ManropeFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            color = MentoriaCores.TextoEscuro
        )

        if (erroAcao != null) {
            Text(
                text = erroAcao,
                fontSize = 11.sp,
                color = AuthColors.Error
            )
        }

        if (proximas.isEmpty()) {

            Text(
                text = "Nenhuma mentoria marcada por enquanto. Cadastre horários na sua agenda para as alunas poderem agendar.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MentoriaCores.TextoMudo
            )

        } else {

            proximas.forEach { mentoria ->
                CartaoDeMentoria(
                    mentoria = mentoria,
                    ocupado = acaoEmAndamentoId == mentoria.id,
                    onConfirmar = { onConfirmar(mentoria.id) },
                    onReagendar = { onReagendar(mentoria) },
                    onCancelar = { onPedirCancelamento(mentoria) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (confirmandoSaida) {
            AlertDialog(
                onDismissRequest = { confirmandoSaida = false },
                title = { Text("Sair da conta?") },
                text = { Text("Você vai precisar entrar de novo com seu e-mail e senha.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmandoSaida = false
                        sair()
                    }) { Text("Sair", color = Color(0xFFDC2626)) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmandoSaida = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
private fun Resumo(
    modifier: Modifier,
    valor: String,
    rotulo: String
) {

    val forma = RoundedCornerShape(17.dp)

    Column(
        modifier = modifier
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = valor,
            fontFamily = ManropeFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MentoriaCores.RoxoEscuro
        )

        Text(
            text = rotulo,
            fontSize = 10.sp,
            color = Color(0xFF928D98)
        )
    }
}

@Composable
private fun CartaoDeMentoria(
    mentoria: AgendamentoResponse,
    ocupado: Boolean,
    onConfirmar: () -> Unit,
    onReagendar: () -> Unit,
    onCancelar: () -> Unit
) {

    val forma = RoundedCornerShape(17.dp)
    val data = parseDataHora(mentoria.inicio)
    val dia = data?.let { SimpleDateFormat("d", LocaleBr).format(it) } ?: "–"
    val mes = data?.let { SimpleDateFormat("MMM", LocaleBr).format(it).uppercase().trimEnd('.') } ?: ""
    val aguardando = mentoria.status == "AGENDADA"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(14.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0EAFF))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = dia,
                    fontFamily = ManropeFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MentoriaCores.RoxoEscuro
                )

                Text(
                    text = mes,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MentoriaCores.RoxoEscuro
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = mentoria.alunaNome,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF20202A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!mentoria.assunto.isNullOrBlank()) {
                    Text(
                        text = mentoria.assunto,
                        fontSize = 12.sp,
                        color = MentoriaCores.TextoMudo,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = descricaoDaMentoria(mentoria),
                    fontSize = 10.sp,
                    color = Color(0xFF85808C)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Situação: aguardando a confirmação da mentora ou já confirmada
        SeloDeStatus(
            texto = if (aguardando) "Aguardando sua confirmação" else "Confirmada",
            fundo = if (aguardando) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
            cor = if (aguardando) Color(0xFFB45309) else Color(0xFF15803D)
        )

        BotaoEntrarNaSala(
            status = mentoria.status,
            inicio = mentoria.inicio,
            fim = mentoria.fim,
            linkSala = mentoria.linkSala,
            minutosAntes = mentoria.minutosParaAbrirSala
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (aguardando) {

            BotaoPrimario(
                texto = if (ocupado) "Aguarde..." else "Confirmar",
                habilitado = !ocupado,
                modifier = Modifier.fillMaxWidth(),
                onClick = onConfirmar
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            BotaoSecundario(
                texto = "Reagendar",
                cor = MentoriaCores.Roxo,
                habilitado = !ocupado,
                modifier = Modifier.weight(1f),
                onClick = onReagendar
            )

            BotaoSecundario(
                texto = "Cancelar",
                cor = Color(0xFFDC2626),
                habilitado = !ocupado,
                modifier = Modifier.weight(1f),
                onClick = onCancelar
            )
        }
    }
}

@Composable
private fun Carregando() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EsqueletoBloco(altura = 56.dp)
        EsqueletoBloco(altura = 72.dp)
        EsqueletoBloco(altura = 90.dp)
        EsqueletoBloco(altura = 90.dp)
    }
}