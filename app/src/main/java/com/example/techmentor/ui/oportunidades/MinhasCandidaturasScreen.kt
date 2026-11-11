package com.example.techmentor.ui.oportunidades

import com.example.techmentor.ui.avaliacao.diaMesEAno
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.admin.DialogoDeConfirmacao
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.home.BottomNavigationBar
import com.example.techmentor.ui.mentoria.CabecalhoVoltar
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily

/** Tudo em que a usuária se candidatou ou se inscreveu, com a situação de cada uma. */
@Composable
fun MinhasCandidaturasScreen(
    navController: NavController,
    viewModel: MinhasCandidaturasViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    // candidatura que a pessoa tocou em "retirar" e ainda precisa confirmar
    var confirmandoId by remember { mutableStateOf<Long?>(null) }

    // Volta ao login se a sessão não existe mais
    LaunchedEffect(estado.sessaoExpirada) {
        if (estado.sessaoExpirada) {
            Sessao.sair()
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar()
    }

    // Com a tela aberta, atualiza sozinha de tempos em tempos (o status muda quando a equipe analisa)
    AtualizarAutomaticamente { viewModel.carregar() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        CabecalhoVoltar(
            titulo = "Minhas candidaturas",
            onVoltar = { navController.popBackStack() }
        )

        Box(modifier = Modifier.weight(1f)) {

            val itens = estado.itens
            val erro = estado.erro

            when {
                itens != null && itens.isEmpty() -> SemParticipacoes()

                itens != null -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val erroAoRetirar = estado.erroAoRetirar
                    if (erroAoRetirar != null) {
                        item {
                            Text(
                                text = erroAoRetirar,
                                fontSize = 13.sp,
                                color = AuthColors.Error
                            )
                        }
                    }

                    items(itens, key = { it.inscricao.id }) { item ->
                        CartaoDeParticipacao(
                            item = item,
                            retirando = estado.retirandoId == item.inscricao.id,
                            onRetirar = { confirmandoId = item.inscricao.id }
                        )
                    }
                }

                erro != null -> EstadoErro(
                    mensagem = erro,
                    onTentarDeNovo = { viewModel.carregar() }
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EsqueletoBloco(altura = 92.dp)
                    EsqueletoBloco(altura = 92.dp)
                    EsqueletoBloco(altura = 92.dp)
                }
            }
        }

        BottomNavigationBar(
            navController = navController,
            rotaAtual = "opportunities"
        )
    }

    val idParaConfirmar = confirmandoId
    if (idParaConfirmar != null) {
        val ehEvento = estado.itens?.firstOrNull { it.inscricao.id == idParaConfirmar }?.ehEvento == true

        DialogoDeConfirmacao(
            titulo = if (ehEvento) "Cancelar inscrição?" else "Retirar candidatura?",
            texto = "Você pode se candidatar de novo depois, enquanto o prazo estiver aberto.",
            textoDoBotao = if (ehEvento) "Cancelar inscrição" else "Retirar",
            onConfirmar = {
                confirmandoId = null
                viewModel.retirar(idParaConfirmar)
            },
            onCancelar = { confirmandoId = null }
        )
    }
}


@Composable
private fun CartaoDeParticipacao(
    item: ItemDeParticipacao,
    retirando: Boolean,
    onRetirar: () -> Unit
) {

    val forma = RoundedCornerShape(17.dp)
    val estilo = estiloDaSituacao(item.inscricao.status)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(16.dp)
    ) {

        Text(
            text = if (item.ehEvento) "INSCRIÇÃO EM EVENTO" else "CANDIDATURA",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            color = MentoriaCores.Roxo
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.inscricao.titulo,
            fontFamily = ManropeFamily,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold,
            color = MentoriaCores.TextoEscuro
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(estilo.fundo)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = estilo.rotulo,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = estilo.cor
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Enviada em ${dataCurta(item.inscricao.criadaEm)}",
                fontSize = 12.sp,
                color = MentoriaCores.TextoMudo
            )
        }

        // Depois da resposta da equipe (aprovada ou recusada) não dá mais para retirar
        if (item.inscricao.status == "ENVIADA" || item.inscricao.status == "EM_ANALISE") {

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = when {
                    retirando -> "Retirando..."
                    item.ehEvento -> "Cancelar inscrição"
                    else -> "Retirar candidatura"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AuthColors.Error,
                modifier = Modifier
                    .clickable(enabled = !retirando) { onRetirar() }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SemParticipacoes() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Você ainda não se candidatou a nada",
            fontFamily = ManropeFamily,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MentoriaCores.TextoEscuro
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Quando você se candidatar a uma vaga ou se inscrever em um evento, ele aparece aqui com a situação.",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            color = MentoriaCores.TextoMudo
        )
    }
}

private data class EstiloDaSituacao(val rotulo: String, val cor: Color, val fundo: Color)

private fun estiloDaSituacao(status: String): EstiloDaSituacao = when (status) {
    "ENVIADA" -> EstiloDaSituacao("Enviada", Color(0xFF6F6A76), Color(0xFFF1F0F4))
    "EM_ANALISE" -> EstiloDaSituacao("Em análise", Color(0xFFB45309), Color(0xFFFEF3C7))
    "APROVADA" -> EstiloDaSituacao("Aprovada", Color(0xFF15803D), Color(0xFFDCFCE7))
    "RECUSADA" -> EstiloDaSituacao("Recusada", Color(0xFFB91C1C), Color(0xFFFEE2E2))
    else -> EstiloDaSituacao(status, Color(0xFF6F6A76), Color(0xFFF1F0F4))
}

/** "2026-11-07T22:30:00" -> "07/11/2026". Se vier num formato inesperado, devolve como chegou. */
private fun dataCurta(iso: String?): String {
    return diaMesEAno(iso).ifEmpty { iso ?: "-" }
}