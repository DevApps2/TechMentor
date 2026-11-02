package com.example.techmentor.ui.oportunidades

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.OportunidadeResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.components.AbaExplorar
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.components.AvisoDemo
import com.example.techmentor.ui.components.ExplorarCabecalho
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha
import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.home.BottomNavigationBar
import com.example.techmentor.ui.mentoria.ChipOpcao
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

private val LOCALE_BR: Locale = Locale.forLanguageTag("pt-BR")


/**
 * Tela de Oportunidades. Mostra a lista; ao tocar em um card mostra os detalhes
 * ali mesmo (sem precisar de rota nova no NavGraph).
 */
@Composable
fun OportunidadesScreen(
    navController: NavController,
    viewModel: OportunidadesViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    // Volta ao login se a sessão não existe mais (ex.: o banco foi reiniciado)
    LaunchedEffect(estado.sessaoExpirada) {
        if (estado.sessaoExpirada) {
            Sessao.sair()
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    var selecionadaId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selecionada = estado.oportunidades?.firstOrNull { it.id == selecionadaId }

    if (selecionada != null) {

        val fecharDetalhes = {
            viewModel.limparErroEnvio()
            selecionadaId = null
        }

        // Botão "voltar" do celular: fecha a confirmação; se não houver, fecha os detalhes
        BackHandler {
            if (estado.confirmadaId != null) viewModel.fecharConfirmacao() else fecharDetalhes()
        }

        OportunidadeDetalhe(
            navController = navController,
            oportunidade = selecionada,
            statusParticipacao = estado.participacoes[selecionada.id],
            enviando = estado.enviandoId == selecionada.id,
            erroEnvio = estado.erroEnvio,
            confirmada = estado.confirmadaId == selecionada.id,
            onParticipar = { viewModel.participar(selecionada) },
            onVoltarAosDetalhes = { viewModel.fecharConfirmacao() },
            onVoltar = fecharDetalhes,
            onVoltarAsOportunidades = {
                viewModel.fecharConfirmacao()
                selecionadaId = null
            }
        )

    } else {

        ListagemOportunidades(
            navController = navController,
            viewModel = viewModel,
            onAbrir = { selecionadaId = it }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListagemOportunidades(
    navController: NavController,
    viewModel: OportunidadesViewModel,
    onAbrir: (Long) -> Unit
) {

    val estado by viewModel.estado.collectAsState()

    // Carrega ao abrir e ao voltar para a tela; fora isso, só puxando para atualizar ou filtrando
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar()
    }

    // Com a tela aberta, atualiza sozinha de tempos em tempos
    AtualizarAutomaticamente { viewModel.carregar() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        TopBarMarca()

        ExplorarCabecalho(
            navController = navController,
            aba = AbaExplorar.OPORTUNIDADES
        )

        FiltrosPorTipo(
            selecionado = estado.filtro,
            onSelecionar = { viewModel.selecionarFiltro(it) }
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {

            val lista = estado.oportunidades
            val erro = estado.erro

            when {
                // Se a atualização falhar, a lista que já estava na tela continua lá
                lista != null -> PullToRefreshBox(
                    isRefreshing = estado.atualizando,
                    onRefresh = { viewModel.carregar(manual = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    ListaDeOportunidades(
                        oportunidades = lista,
                        participacoes = estado.participacoes,
                        onAbrir = onAbrir
                    )
                }

                erro != null -> EstadoErro(
                    mensagem = erro,
                    onTentarDeNovo = { viewModel.carregar() }
                )

                else -> OportunidadesCarregando()
            }
        }

        BottomNavigationBar(
            navController = navController,
            rotaAtual = "opportunities"
        )
    }
}


// ======================================================
// FILTROS
// ======================================================

@Composable
private fun FiltrosPorTipo(
    selecionado: FiltroTipo,
    onSelecionar: (FiltroTipo) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        FiltroTipo.entries.forEach { filtro ->
            ChipOpcao(
                texto = filtro.rotulo,
                selecionado = filtro == selecionado,
                onClick = { onSelecionar(filtro) }
            )
        }
    }
}


// ======================================================
// LISTA
// ======================================================

@Composable
private fun ListaDeOportunidades(
    oportunidades: List<OportunidadeResponse>,
    participacoes: Map<Long, String>,
    onAbrir: (Long) -> Unit
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item { AvisoDemo(texto = "Vagas e organizações fictícias, sem parcerias reais ou inscrições externas.") }

        if (oportunidades.isEmpty()) {

            item { SemOportunidades() }

        } else {

            item {
                Text(
                    text = if (oportunidades.size == 1) "1 oportunidade" else "${oportunidades.size} oportunidades",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8C8796)
                )
            }

            items(
                items = oportunidades,
                key = { it.id }
            ) { oportunidade ->
                OportunidadeCard(
                    oportunidade = oportunidade,
                    participou = participacoes.containsKey(oportunidade.id),
                    onClick = { onAbrir(oportunidade.id) }
                )
            }
        }
    }
}


// ======================================================
// CARD
// ======================================================

internal class EstiloTipo(val rotulo: String, val fundo: Color, val icone: Color)

internal fun estiloDoTipo(tipo: String): EstiloTipo = when (tipo.uppercase()) {
    "ESTAGIO" -> EstiloTipo("ESTÁGIO", Color(0xFFEDE9FE), Color(0xFF6D28D9))
    "BOLSA" -> EstiloTipo("BOLSA", Color(0xFFE6EFFF), Color(0xFF2563EB))
    "EMPREGO" -> EstiloTipo("EMPREGO", Color(0xFFFCE7F3), Color(0xFFDB2777))
    "EVENTO" -> EstiloTipo("EVENTO", Color(0xFFFEF3C7), Color(0xFFD97706))
    else -> EstiloTipo(tipo.uppercase(), Color(0xFFEDE9FE), Color(0xFF6D28D9))
}

/** "2026-11-18" -> "Até 18 nov. 2026". Sem prazo, não mostra nada. */
private fun formatarPrazo(prazo: String?): String? {

    if (prazo.isNullOrBlank()) return null

    return try {
        val data = SimpleDateFormat("yyyy-MM-dd", LOCALE_BR).parse(prazo) ?: return prazo
        "Até " + SimpleDateFormat("d MMM yyyy", LOCALE_BR).format(data)
    } catch (e: ParseException) {
        prazo
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OportunidadeCard(
    oportunidade: OportunidadeResponse,
    participou: Boolean,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(17.dp)
    val estilo = estiloDoTipo(oportunidade.tipo)
    val prazo = formatarPrazo(oportunidade.prazoInscricao)

    // área, nível e local viram as etiquetas cinzas do card
    val etiquetas = listOf(oportunidade.area, oportunidade.nivel, oportunidade.localidade)
        .mapNotNull { it?.trim()?.takeIf { texto -> texto.isNotEmpty() } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(estilo.fundo),
            contentAlignment = Alignment.Center
        ) {
            IconeLinha(
                icone = Icone.MALETA,
                cor = estilo.icone,
                tamanho = 22.dp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = estilo.rotulo,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    color = MentoriaCores.Roxo
                )

                if (participou) {
                    // já enviada: no lugar do prazo aparece a marca verde
                    Text(
                        text = if (oportunidade.tipo.equals("EVENTO", ignoreCase = true)) "✓ Inscrita" else "✓ Enviada",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                } else if (prazo != null) {
                    Text(
                        text = prazo,
                        fontSize = 10.sp,
                        color = Color(0xFF8C8796)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = oportunidade.titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20202A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!oportunidade.organizacao.isNullOrBlank()) {

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = oportunidade.organizacao,
                    fontSize = 12.sp,
                    color = MentoriaCores.TextoMudo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (etiquetas.isNotEmpty()) {

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    etiquetas.forEach { EtiquetaCinza(it) }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconeLinha(
            icone = Icone.SETA,
            cor = MentoriaCores.TextoEscuro,
            tamanho = 17.dp
        )
    }
}

@Composable
private fun EtiquetaCinza(texto: String) {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF1F0F5))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {

        Text(
            text = texto,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF514C58)
        )
    }
}


// ======================================================
// SEM RESULTADOS E CARREGANDO
// ======================================================

@Composable
private fun SemOportunidades() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Nenhuma oportunidade por aqui",
            fontFamily = ManropeFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = Color(0xFF20202A)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tente escolher outro tipo de oportunidade.",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF797486)
        )
    }
}

@Composable
private fun OportunidadesCarregando() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        EsqueletoBloco(altura = 56.dp)
        EsqueletoBloco(altura = 110.dp)
        EsqueletoBloco(altura = 110.dp)
        EsqueletoBloco(altura = 110.dp)
    }
}
