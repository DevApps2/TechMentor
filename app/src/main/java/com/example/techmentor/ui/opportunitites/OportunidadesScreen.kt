package com.example.techmentor.ui.oportunidades

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OportunidadesScreen(
    navController: NavController,
    viewModel: OportunidadesViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    // Carrega ao abrir e ao voltar para a tela
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        TopBarMarca()

        Box(
            modifier = Modifier.weight(1f)
        ) {

            val erro = estado.erro
            val lista = estado.oportunidades

            when {
                erro != null -> EstadoErro(
                    mensagem = erro,
                    onTentarDeNovo = { viewModel.carregar() }
                )

                lista == null -> OportunidadesCarregando()

                else -> PullToRefreshBox(
                    isRefreshing = estado.atualizando,
                    onRefresh = { viewModel.carregar(manual = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    ListaDeOportunidades(
                        navController = navController,
                        oportunidades = lista,
                        filtro = estado.filtro,
                        onFiltro = { viewModel.selecionarFiltro(it) },
                        onAbrir = { _ ->
                            // TODO: quando existir a tela de detalhes, navegar para ela:
                            // navController.navigate("opportunities/$id")
                        }
                    )
                }
            }
        }

        BottomNavigationBar(
            navController = navController,
            rotaAtual = "opportunities"
        )
    }
}


// ======================================================
// LISTA (o cabeçalho rola junto, como no Figma)
// ======================================================

@Composable
private fun ListaDeOportunidades(
    navController: NavController,
    oportunidades: List<OportunidadeResponse>,
    filtro: FiltroTipo,
    onFiltro: (FiltroTipo) -> Unit,
    onAbrir: (Long) -> Unit
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
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
                    text = "Oportunidades",
                    fontFamily = ManropeFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.8).sp,
                    color = MentoriaCores.TextoEscuro
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Seu próximo passo começa aqui.",
                    fontSize = 15.sp,
                    color = MentoriaCores.TextoMudo
                )
            }
        }

        item { AbasExplorar(navController = navController) }

        item { AvisoFicticio() }

        item { FiltrosPorTipo(selecionado = filtro, onSelecionar = onFiltro) }

        item {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MentoriaCores.TextoEscuro)) {
                        append(if (oportunidades.size == 1) "1 oportunidade" else "${oportunidades.size} oportunidades")
                    }
                    withStyle(SpanStyle(color = MentoriaCores.TextoMudo)) {
                        append(" para você")
                    }
                },
                fontSize = 14.sp
            )
        }

        if (oportunidades.isEmpty()) {

            item { SemOportunidades() }

        } else {

            items(
                items = oportunidades,
                key = { it.id }
            ) { oportunidade ->
                OportunidadeCard(
                    oportunidade = oportunidade,
                    onClick = { onAbrir(oportunidade.id) }
                )
            }
        }
    }
}


// ======================================================
// ABAS: Mentoria | Oportunidades | Segurança
// ======================================================

@Composable
private fun AbasExplorar(navController: NavController) {

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

            val ativa = rota == "opportunities"
            val forma = RoundedCornerShape(16.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(forma)
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
// AVISO + FILTROS
// ======================================================

@Composable
private fun AvisoFicticio() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF1EEFB))
    ) {

        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(MentoriaCores.Roxo)
        )

        Text(
            text = "Vagas e organizações fictícias, sem parcerias reais ou inscrições externas.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MentoriaCores.TextoMudo
        )
    }
}

@Composable
private fun FiltrosPorTipo(
    selecionado: FiltroTipo,
    onSelecionar: (FiltroTipo) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        FiltroTipo.values().forEach { filtro ->
            ChipOpcao(
                texto = filtro.rotulo,
                selecionado = filtro == selecionado,
                onClick = { onSelecionar(filtro) }
            )
        }
    }
}


// ======================================================
// CARD
// ======================================================

private class EstiloTipo(val rotulo: String, val fundo: Color, val icone: Color)

private fun estiloDoTipo(tipo: String): EstiloTipo = when (tipo.uppercase()) {
    "ESTAGIO" -> EstiloTipo("ESTÁGIO", Color(0xFFEDE9FE), Color(0xFF7C3AED))
    "BOLSA" -> EstiloTipo("BOLSA", Color(0xFFE3EEFF), Color(0xFF2563EB))
    "EMPREGO" -> EstiloTipo("EMPREGO", Color(0xFFFCE7F3), Color(0xFFDB2777))
    "EVENTO" -> EstiloTipo("EVENTO", Color(0xFFFEF3C7), Color(0xFFD97706))
    else -> EstiloTipo(tipo.uppercase(), Color(0xFFEDE9FE), Color(0xFF7C3AED))
}

/** "2026-11-18" -> "Até 18 de nov. de 2026". Sem prazo, não mostra nada. */
private fun formatarPrazo(prazo: String?): String? {

    if (prazo.isNullOrBlank()) return null

    return try {
        val data = SimpleDateFormat("yyyy-MM-dd", LOCALE_BR).parse(prazo) ?: return prazo
        "Até " + SimpleDateFormat("d 'de' MMM 'de' yyyy", LOCALE_BR).format(data)
    } catch (e: ParseException) {
        prazo
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OportunidadeCard(
    oportunidade: OportunidadeResponse,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(26.dp)
    val estilo = estiloDoTipo(oportunidade.tipo)
    val prazo = formatarPrazo(oportunidade.prazoInscricao)

    // área, nível e local viram as etiquetas cinzas do card
    val etiquetas = listOf(oportunidade.area, oportunidade.nivel, oportunidade.localidade)
        .mapNotNull { it?.trim()?.takeIf { texto -> texto.isNotEmpty() } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(16.dp)
    ) {

        Row(
            verticalAlignment = Alignment.Top
        ) {

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(estilo.fundo),
                contentAlignment = Alignment.Center
            ) {
                IconeLinha(
                    icone = Icone.MALETA,
                    cor = estilo.icone,
                    tamanho = 26.dp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MentoriaCores.Roxo
                    )

                    if (prazo != null) {
                        Text(
                            text = prazo,
                            fontSize = 13.sp,
                            color = MentoriaCores.TextoMudo
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = oportunidade.titulo,
                    fontFamily = ManropeFamily,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Medium,
                    color = MentoriaCores.TextoEscuro
                )

                if (!oportunidade.organizacao.isNullOrBlank()) {

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = oportunidade.organizacao,
                        fontSize = 15.sp,
                        color = MentoriaCores.TextoMudo
                    )
                }
            }
        }

        if (etiquetas.isNotEmpty()) {

            Spacer(modifier = Modifier.height(16.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                etiquetas.forEach { EtiquetaCinza(it) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEEEAFD))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "Ver oportunidade  ›",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MentoriaCores.Roxo
            )
        }
    }
}

@Composable
private fun EtiquetaCinza(texto: String) {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF1F0F5))
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {

        Text(
            text = texto,
            fontSize = 14.sp,
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
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        EsqueletoBloco(altura = 150.dp)
        EsqueletoBloco(altura = 220.dp)
        EsqueletoBloco(altura = 220.dp)
    }
}
