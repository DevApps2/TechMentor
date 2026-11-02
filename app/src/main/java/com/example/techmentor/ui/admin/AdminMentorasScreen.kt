package com.example.techmentor.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.techmentor.data.MentoraResponse
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.mentoria.AvatarMentora
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.mentoria.TagArea
import com.example.techmentor.ui.mentoria.cargoEEmpresa
import com.example.techmentor.ui.theme.ManropeFamily


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMentorasScreen(
    navController: NavController,
    viewModel: AdminMentorasViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    // Carrega ao abrir e ao voltar do formulário (onde alguém pode ter sido cadastrada, editada ou excluída)
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
            titulo = "Mentoras",
            subtitulo = "Cadastre e cuide do perfil de quem mentora na plataforma."
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
        ) {

            GradientButton(
                text = "Cadastrar mentora",
                onClick = { navController.navigate("admin/mentoras/nova") }
            )
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

                mentoras == null -> MentorasCarregando()

                else -> PullToRefreshBox(
                    isRefreshing = estado.atualizando,
                    onRefresh = { viewModel.carregar(manual = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    ListaDeMentoras(
                        mentoras = mentoras,
                        onAbrir = { id -> navController.navigate("admin/mentoras/$id") }
                    )
                }
            }
        }

        AdminBottomBar(
            navController = navController,
            rotaAtual = "admin/mentoras"
        )
    }
}


@Composable
private fun ListaDeMentoras(
    mentoras: List<MentoraResponse>,
    onAbrir: (Long) -> Unit
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        if (mentoras.isEmpty()) {

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Nenhuma mentora cadastrada",
                        fontFamily = ManropeFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF20202A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Toque em \"Cadastrar mentora\" para criar a primeira.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF797486)
                    )
                }
            }

        } else {

            item {
                Text(
                    text = if (mentoras.size == 1) "1 mentora" else "${mentoras.size} mentoras",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8C8796)
                )
            }

            items(
                items = mentoras,
                key = { it.id }
            ) { mentora ->
                MentoraAdminCard(
                    mentora = mentora,
                    onClick = { onAbrir(mentora.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MentoraAdminCard(
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
            tamanho = 48.dp,
            raio = 16.dp,
            tamanhoDaInicial = 20.sp
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

            if (areas.isNotEmpty()) {

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    areas.forEach { area ->
                        TagArea(texto = area)
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

@Composable
private fun MentorasCarregando() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        EsqueletoBloco(altura = 100.dp)
        EsqueletoBloco(altura = 100.dp)
        EsqueletoBloco(altura = 100.dp)
    }
}
