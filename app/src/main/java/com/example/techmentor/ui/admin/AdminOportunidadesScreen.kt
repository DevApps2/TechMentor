package com.example.techmentor.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.AdminOportunidade
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.mentoria.ChipOpcao
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.mentoria.TagArea
import com.example.techmentor.ui.theme.ManropeFamily


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOportunidadesScreen(
    navController: NavController,
    viewModel: AdminOportunidadesViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    // Carrega ao abrir e ao voltar do formulário (onde algo pode ter sido criado, editado ou excluído)
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

        TituloDeAba(
            titulo = "Oportunidades",
            subtitulo = "Estágios, empregos, bolsas e eventos da plataforma."
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
        ) {

            GradientButton(
                text = "Nova oportunidade",
                onClick = { navController.navigate("admin/oportunidades/nova") }
            )
        }

        Box(
            modifier = Modifier.weight(1f)
        ) {

            val erro = estado.erro
            val oportunidades = estado.oportunidades

            when {
                erro != null -> EstadoErro(
                    mensagem = erro,
                    onTentarDeNovo = { viewModel.carregar() }
                )

                oportunidades == null -> OportunidadesCarregando()

                else -> Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    FiltrosDeTipo(
                        selecionado = estado.filtro,
                        onSelecionar = { viewModel.selecionarFiltro(it) }
                    )

                    PullToRefreshBox(
                        isRefreshing = estado.atualizando,
                        onRefresh = { viewModel.carregar(manual = true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        ListaDeOportunidades(
                            oportunidades = oportunidades,
                            filtro = estado.filtro,
                            onAbrir = { id -> navController.navigate("admin/oportunidades/$id") },
                            onVerCandidaturas = { id -> navController.navigate("admin/oportunidades/$id/candidaturas") }
                        )
                    }
                }
            }
        }

        AdminBottomBar(
            navController = navController,
            rotaAtual = "admin/oportunidades"
        )
    }
}


@Composable
private fun FiltrosDeTipo(
    selecionado: String?,
    onSelecionar: (String?) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        ChipOpcao(
            texto = "Todas",
            selecionado = selecionado == null,
            onClick = { onSelecionar(null) }
        )

        TIPOS_DE_OPORTUNIDADE.forEach { tipo ->
            ChipOpcao(
                texto = nomeDoTipoDeOportunidade(tipo),
                selecionado = selecionado == tipo,
                onClick = { onSelecionar(tipo) }
            )
        }
    }
}

@Composable
private fun ListaDeOportunidades(
    oportunidades: List<AdminOportunidade>,
    filtro: String?,
    onAbrir: (Long) -> Unit,
    onVerCandidaturas: (Long) -> Unit
) {

    val visiveis = oportunidades.filter { filtro == null || it.tipo == filtro }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        if (visiveis.isEmpty()) {

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 40.dp),
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
                        text = if (filtro == null) "Toque em \"Nova oportunidade\" para cadastrar a primeira." else "Não há oportunidades desse tipo.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF797486)
                    )
                }
            }

        } else {

            items(
                items = visiveis,
                key = { it.id }
            ) { oportunidade ->
                OportunidadeCard(
                    oportunidade = oportunidade,
                    onClick = { onAbrir(oportunidade.id) },
                    onVerCandidaturas = { onVerCandidaturas(oportunidade.id) }
                )
            }
        }
    }
}

@Composable
private fun OportunidadeCard(
    oportunidade: AdminOportunidade,
    onClick: () -> Unit,
    onVerCandidaturas: () -> Unit
) {

    val forma = RoundedCornerShape(17.dp)
    val encerrada = prazoEncerrado(oportunidade.prazoInscricao)

    val detalhes = listOfNotNull(
        oportunidade.organizacao?.trim()?.takeIf { it.isNotEmpty() },
        oportunidade.area?.trim()?.takeIf { it.isNotEmpty() }
    ).joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            TagArea(texto = nomeDoTipoDeOportunidade(oportunidade.tipo))

            Spacer(modifier = Modifier.width(8.dp))

            if (encerrada) {
                TagEncerrada()
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "›",
                fontSize = 26.sp,
                color = Color(0xFF797486)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = oportunidade.titulo,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF20202A),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (detalhes.isNotEmpty()) {

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = detalhes,
                fontSize = 12.sp,
                color = MentoriaCores.TextoMudo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (oportunidade.prazoInscricao.isNullOrBlank()) "Sem prazo definido" else "Prazo: ${formatarData(oportunidade.prazoInscricao)}",
            fontSize = 11.sp,
            color = Color(0xFF8A8691)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ver candidaturas  ›",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MentoriaCores.Roxo,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onVerCandidaturas)
                .padding(vertical = 4.dp)
        )
    }
}

/** Avisa que o prazo passou (a oportunidade continua ativa até alguém excluir). */
@Composable
private fun TagEncerrada() {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFEE2E2))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {

        Text(
            text = "Prazo encerrado",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFDC2626)
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

        EsqueletoBloco(altura = 40.dp)
        EsqueletoBloco(altura = 100.dp)
        EsqueletoBloco(altura = 100.dp)
        EsqueletoBloco(altura = 100.dp)
    }
}
