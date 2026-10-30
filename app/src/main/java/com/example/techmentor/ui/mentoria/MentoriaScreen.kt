package com.example.techmentor.ui.mentoria

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.MentoraResponse
import com.example.techmentor.ui.auth.AuthField
import com.example.techmentor.ui.auth.OutlinedAuthButton
import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.home.BottomNavigationBar
import com.example.techmentor.ui.theme.ManropeFamily

private const val MAXIMO_DE_AREAS_NO_CARD = 3


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentoriaScreen(
    navController: NavController,
    viewModel: MentoriaViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()
    val foco = LocalFocusManager.current

    // O texto do campo fica aqui, e não só no ViewModel, para a digitação não "pular".
    // Começa com o que o ViewModel já tem (ao voltar do perfil da mentora a busca continua lá).
    var texto by remember { mutableStateOf(estado.busca) }

    // Carrega ao abrir e ao voltar para a tela; fora isso, só puxando para atualizar, buscando ou filtrando
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp)
        ) {

            Text(
                text = "Mentoria",
                fontFamily = ManropeFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
                color = MentoriaCores.TextoEscuro
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Converse com quem já trilhou o caminho.",
                fontSize = 13.sp,
                color = Color(0xFF797486)
            )

            Spacer(modifier = Modifier.height(14.dp))

            AuthField(
                label = "Buscar mentora",
                value = texto,
                onValueChange = {
                    texto = it
                    viewModel.aoDigitarBusca(it)
                },
                placeholder = "Nome, cargo ou área",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.buscarAgora()
                        foco.clearFocus()
                    }
                )
            )
        }

        FiltrosPorArea(
            areas = estado.areas,
            selecionada = estado.areaSelecionada,
            onSelecionar = { viewModel.selecionarArea(it) }
        )

        // faixa fina que mostra que uma busca ou filtro está carregando (a altura fica reservada para a tela não pular)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
        ) {

            if (estado.buscando) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MentoriaCores.Roxo,
                    trackColor = MentoriaCores.RoxoClaro
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f)
        ) {

            val erro = estado.erro
            val mentoras = estado.mentoras

            when {
                erro != null -> EstadoErro(
                    mensagem = erro,
                    onTentarDeNovo = { viewModel.carregar() }
                )

                mentoras == null -> MentoriaCarregando()

                else -> PullToRefreshBox(
                    isRefreshing = estado.atualizando,
                    onRefresh = { viewModel.carregar(manual = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    ListaDeMentoras(
                        mentoras = mentoras,
                        temFiltro = estado.temFiltro,
                        onAbrir = { id -> navController.navigate("mentorship/$id") },
                        onLimparFiltros = {
                            texto = ""
                            foco.clearFocus()
                            viewModel.limparFiltros()
                        }
                    )
                }
            }
        }

        BottomNavigationBar(
            navController = navController,
            rotaAtual = "mentorship"
        )
    }
}


// ======================================================
// FILTRO POR ÁREA
// ======================================================

@Composable
private fun FiltrosPorArea(
    areas: List<String>,
    selecionada: String?,
    onSelecionar: (String?) -> Unit
) {

    if (areas.isEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        ChipOpcao(
            texto = "Todas",
            selecionado = selecionada == null,
            onClick = { onSelecionar(null) }
        )

        areas.forEach { area ->
            ChipOpcao(
                texto = area,
                selecionado = selecionada == area,
                onClick = { onSelecionar(area) }
            )
        }
    }
}


// ======================================================
// LISTA
// ======================================================

@Composable
private fun ListaDeMentoras(
    mentoras: List<MentoraResponse>,
    temFiltro: Boolean,
    onAbrir: (Long) -> Unit,
    onLimparFiltros: () -> Unit
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        if (mentoras.isEmpty()) {

            item {
                SemResultados(
                    temFiltro = temFiltro,
                    onLimparFiltros = onLimparFiltros
                )
            }

        } else {

            item {
                Text(
                    text = if (mentoras.size == 1) "1 mentora encontrada" else "${mentoras.size} mentoras encontradas",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8C8796)
                )
            }

            items(
                items = mentoras,
                key = { it.id }
            ) { mentora ->
                MentoraCard(
                    mentora = mentora,
                    onClick = { onAbrir(mentora.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MentoraCard(
    mentora: MentoraResponse,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(17.dp)
    val areas = mentora.areas.orEmpty()

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

        AvatarMentora(
            nome = mentora.nome,
            fotoUrl = mentora.fotoUrl,
            tamanho = 54.dp,
            raio = 18.dp,
            tamanhoDaInicial = 22.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = mentora.nome,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20202A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = cargoEEmpresa(mentora),
                fontSize = 12.sp,
                color = MentoriaCores.TextoMudo,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinhaDeNota(mentora = mentora)

            if (areas.isNotEmpty()) {

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    areas.take(MAXIMO_DE_AREAS_NO_CARD).forEach { area ->
                        TagArea(texto = area)
                    }

                    if (areas.size > MAXIMO_DE_AREAS_NO_CARD) {
                        TagArea(texto = "+${areas.size - MAXIMO_DE_AREAS_NO_CARD}")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = "›",
            fontSize = 26.sp,
            color = Color(0xFF797486)
        )
    }
}

/** Ex.: "★ 5,0 (1) · 8 anos de experiência"; sem avaliações aparece "Nova mentora". */
@Composable
private fun LinhaDeNota(mentora: MentoraResponse) {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        if (mentora.totalAvaliacoes > 0) {

            Text(
                text = "★",
                fontSize = 12.sp,
                color = MentoriaCores.Estrela
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = formatarNota(mentora.notaMedia),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MentoriaCores.TextoEscuro
            )

            Spacer(modifier = Modifier.width(3.dp))

            Text(
                text = "(${mentora.totalAvaliacoes})",
                fontSize = 11.sp,
                color = Color(0xFF8A8691)
            )

        } else {

            Text(
                text = "Nova mentora",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MentoriaCores.Roxo
            )
        }

        Text(
            text = "  ·  ${textoAnos(mentora.anosExperiencia)} de experiência",
            modifier = Modifier.weight(1f, fill = false),
            fontSize = 11.sp,
            color = Color(0xFF8A8691),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


// ======================================================
// SEM RESULTADOS E CARREGANDO
// ======================================================

@Composable
private fun SemResultados(
    temFiltro: Boolean,
    onLimparFiltros: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = if (temFiltro) "Nenhuma mentora encontrada" else "Ainda não há mentoras por aqui",
            fontFamily = ManropeFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = Color(0xFF20202A)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (temFiltro) {
                "Tente buscar por outro nome ou escolher outra área."
            } else {
                "Volte em breve: novas mentoras estão chegando."
            },
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF797486)
        )

        if (temFiltro) {

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedAuthButton(
                text = "Limpar filtros",
                onClick = onLimparFiltros
            )
        }
    }
}

@Composable
private fun MentoriaCarregando() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        EsqueletoBloco(altura = 118.dp)
        EsqueletoBloco(altura = 118.dp)
        EsqueletoBloco(altura = 118.dp)
        EsqueletoBloco(altura = 118.dp)
    }
}
