package com.example.techmentor.ui.notificacoes

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.NotificacaoResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha
import com.example.techmentor.ui.home.parseDataHora
import com.example.techmentor.ui.mentoria.CabecalhoVoltar
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily
import java.text.SimpleDateFormat
import java.util.Locale

private val LocaleBr: Locale = Locale.forLanguageTag("pt-BR")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacoesScreen(
    navController: NavController,
    viewModel: NotificacoesViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    // Volta ao login se a sessão não existe mais (ex.: o banco foi reiniciado)
    LaunchedEffect(estado.sessaoExpirada) {
        if (estado.sessaoExpirada) {
            Sessao.sair()
            navController.navigate("login") {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    // Carrega ao abrir e ao voltar para a tela; fora isso, só puxando para atualizar
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        CabecalhoVoltar(
            titulo = "Notificações",
            onVoltar = { navController.popBackStack() }
        )

        Box(modifier = Modifier.weight(1f)) {

            val lista = estado.lista

            when {
                lista != null -> PullToRefreshBox(
                    isRefreshing = estado.atualizando,
                    onRefresh = { viewModel.carregar(manual = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Lista(
                        lista = lista,
                        onAbrir = { viewModel.marcarComoLida(it) },
                        onMarcarTodas = { viewModel.marcarTodasComoLidas() }
                    )
                }

                estado.carregando || estado.sessaoExpirada -> Carregando()

                else -> EstadoErro(
                    mensagem = estado.erro ?: "Não foi possível carregar suas notificações.",
                    onTentarDeNovo = { viewModel.carregar() }
                )
            }
        }
    }
}


@Composable
private fun Lista(
    lista: List<NotificacaoResponse>,
    onAbrir: (NotificacaoResponse) -> Unit,
    onMarcarTodas: () -> Unit
) {

    val naoLidas = lista.count { !it.lida }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = when (naoLidas) {
                        0 -> "Tudo em dia"
                        1 -> "1 não lida"
                        else -> "$naoLidas não lidas"
                    },
                    fontFamily = ManropeFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MentoriaCores.TextoEscuro
                )

                if (naoLidas > 0) {
                    Text(
                        text = "Marcar todas como lidas",
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button, onClick = onMarcarTodas)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MentoriaCores.Roxo
                    )
                }
            }
        }

        if (lista.isEmpty()) {
            item { Vazio() }
        } else {
            items(lista, key = { it.id }) { notificacao ->
                CartaoDeNotificacao(
                    notificacao = notificacao,
                    onClick = { onAbrir(notificacao) }
                )
            }
        }
    }
}


@Composable
private fun CartaoDeNotificacao(
    notificacao: NotificacaoResponse,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(16.dp)
    val naoLida = !notificacao.lida

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            // as não lidas ficam com um fundo lilás e a bolinha roxa
            .background(if (naoLida) Color(0xFFF5F0FF) else Color.White)
            .border(1.dp, if (naoLida) Color(0xFFDDD0FB) else MentoriaCores.Borda, forma)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (naoLida) MentoriaCores.RoxoClaro else Color(0xFFF1F0F5)),
            contentAlignment = Alignment.Center
        ) {

            IconeLinha(
                icone = iconeDaNotificacao(notificacao.titulo),
                cor = if (naoLida) MentoriaCores.Roxo else Color(0xFF9A96A3),
                tamanho = 20.dp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Text(
                    text = notificacao.titulo,
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    fontWeight = if (naoLida) FontWeight.Bold else FontWeight.SemiBold,
                    color = MentoriaCores.TextoEscuro
                )

                if (naoLida) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MentoriaCores.Roxo)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = notificacao.mensagem,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MentoriaCores.TextoMudo
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tempoRelativo(notificacao.criadaEm),
                fontSize = 10.sp,
                color = Color(0xFF98939E)
            )
        }
    }
}

/** O backend só manda título e mensagem, então o ícone é escolhido pelo assunto do título. */
private fun iconeDaNotificacao(titulo: String): Icone {
    val t = titulo.lowercase(LocaleBr)
    return when {
        "mentoria" in t -> Icone.USUARIOS
        "denúncia" in t -> Icone.ESCUDO
        "curso" in t -> Icone.LIVRO
        "conquista" in t || "nível" in t || "desafio" in t -> Icone.TROFEU
        else -> Icone.SINO
    }
}

private fun tempoRelativo(criadaEm: String?): String {
    val data = parseDataHora(criadaEm) ?: return ""
    val minutos = (System.currentTimeMillis() - data.time) / 60_000
    return when {
        minutos < 1 -> "agora"
        minutos < 60 -> "há $minutos min"
        minutos < 24 * 60 -> "há ${minutos / 60} h"
        else -> SimpleDateFormat("dd/MM 'às' HH:mm", LocaleBr).format(data)
    }
}

@Composable
private fun Vazio() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        IconeLinha(icone = Icone.SINO, cor = Color(0xFFC9C5D1), tamanho = 40.dp)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Nenhuma notificação por aqui",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MentoriaCores.TextoEscuro
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Quando algo acontecer, como uma mentoria confirmada ou uma conquista, avisamos você aqui.",
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            color = MentoriaCores.TextoMudo
        )
    }
}

@Composable
private fun Carregando() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        EsqueletoBloco(altura = 30.dp)
        EsqueletoBloco(altura = 84.dp)
        EsqueletoBloco(altura = 84.dp)
        EsqueletoBloco(altura = 84.dp)
    }
}