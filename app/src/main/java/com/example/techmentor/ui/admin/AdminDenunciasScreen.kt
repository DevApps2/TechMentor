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
import com.example.techmentor.data.AdminDenuncia
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.mentoria.ChipOpcao
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDenunciasScreen(
    navController: NavController,
    viewModel: AdminDenunciasViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    EfeitoSessaoExpirada(expirada = estado.sessaoExpirada, navController = navController)

    // Carrega ao abrir e ao voltar da tela de detalhe (onde o status pode ter mudado)
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
            titulo = "Denúncias",
            subtitulo = "Analise e resolva o que as usuárias reportaram."
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {

            val erro = estado.erro
            val denuncias = estado.denuncias

            when {
                erro != null -> EstadoErro(
                    mensagem = erro,
                    onTentarDeNovo = { viewModel.carregar() }
                )

                denuncias == null -> DenunciasCarregando()

                else -> Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    FiltrosDeStatus(
                        denuncias = denuncias,
                        selecionado = estado.filtro,
                        onSelecionar = { viewModel.selecionarFiltro(it) }
                    )

                    PullToRefreshBox(
                        isRefreshing = estado.atualizando,
                        onRefresh = { viewModel.carregar(manual = true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        ListaDeDenuncias(
                            denuncias = denuncias,
                            filtro = estado.filtro,
                            nomes = estado.nomes,
                            onAbrir = { id -> navController.navigate("admin/denuncias/$id") }
                        )
                    }
                }
            }
        }

        AdminBottomBar(
            navController = navController,
            rotaAtual = "admin/denuncias"
        )
    }
}


// ======================================================
// FILTRO POR STATUS
// ======================================================

@Composable
private fun FiltrosDeStatus(
    denuncias: List<AdminDenuncia>,
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
            texto = "Todas (${denuncias.size})",
            selecionado = selecionado == null,
            onClick = { onSelecionar(null) }
        )

        STATUS_DE_DENUNCIA.forEach { status ->
            ChipOpcao(
                texto = "${nomeDoStatus(status)} (${denuncias.count { it.status == status }})",
                selecionado = selecionado == status,
                onClick = { onSelecionar(status) }
            )
        }
    }
}


// ======================================================
// LISTA
// ======================================================

@Composable
private fun ListaDeDenuncias(
    denuncias: List<AdminDenuncia>,
    filtro: String?,
    nomes: Map<Long, String>,
    onAbrir: (Long) -> Unit
) {

    val visiveis = ordenarDenuncias(denuncias.filter { filtro == null || it.status == filtro })

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
                        text = if (filtro == null) "Nenhuma denúncia por aqui" else "Nenhuma denúncia ${nomeDoStatus(filtro).lowercase()}",
                        fontFamily = ManropeFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF20202A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Quando alguém denunciar, a denúncia aparece aqui.",
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
            ) { denuncia ->
                DenunciaCard(
                    denuncia = denuncia,
                    nomes = nomes,
                    onClick = { onAbrir(denuncia.id) }
                )
            }
        }
    }
}

@Composable
private fun DenunciaCard(
    denuncia: AdminDenuncia,
    nomes: Map<Long, String>,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(17.dp)

    val denunciante = nomeDaUsuaria(nomes, denuncia.denuncianteId)
    val resumo = if (denuncia.denunciadoId == null) {
        "$denunciante fez uma denúncia geral"
    } else {
        "$denunciante denunciou ${nomeDaUsuaria(nomes, denuncia.denunciadoId)}"
    }

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

            PilulaStatus(status = denuncia.status)

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = nomeDoMotivo(denuncia.motivo),
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20202A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "›",
                fontSize = 26.sp,
                color = Color(0xFF797486)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = resumo,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MentoriaCores.TextoEscuro,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (!denuncia.descricao.isNullOrBlank()) {

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = denuncia.descricao,
                fontSize = 12.sp,
                color = MentoriaCores.TextoMudo,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = formatarDataHora(denuncia.criadaEm),
            fontSize = 11.sp,
            color = Color(0xFF8A8691)
        )
    }
}

/** O nome da usuária; se ela não estiver na lista (ex.: conta desativada), mostra o número. */
fun nomeDaUsuaria(nomes: Map<Long, String>, id: Long): String = nomes[id] ?: "Usuária #$id"

@Composable
private fun DenunciasCarregando() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        EsqueletoBloco(altura = 40.dp)
        EsqueletoBloco(altura = 110.dp)
        EsqueletoBloco(altura = 110.dp)
        EsqueletoBloco(altura = 110.dp)
    }
}
