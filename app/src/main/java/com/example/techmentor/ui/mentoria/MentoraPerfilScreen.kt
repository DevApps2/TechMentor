package com.example.techmentor.ui.mentoria

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.HorarioResponse
import com.example.techmentor.data.MentoraResponse
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.home.BottomNavigationBar
import com.example.techmentor.ui.theme.ManropeFamily

// Quantos horários aparecem como prévia; a lista completa fica na tela de agendamento
private const val HORARIOS_NA_PREVIA = 3


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentoraPerfilScreen(
    navController: NavController,
    mentoraId: Long,
    viewModel: MentoraPerfilViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    // Carrega ao abrir e ao voltar para a tela (ex.: depois de agendar, os horários mudam)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar(mentoraId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        CabecalhoVoltar(
            titulo = "Perfil da mentora",
            onVoltar = { navController.popBackStack() }
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {

            val mentora = estado.mentora

            when {
                mentora != null -> PullToRefreshBox(
                    isRefreshing = estado.atualizando,
                    onRefresh = { viewModel.carregar(mentoraId, manual = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    PerfilConteudo(
                        mentora = mentora,
                        horarios = estado.horarios,
                        onAgendar = { navController.navigate("mentorship/$mentoraId/agendar") }
                    )
                }

                estado.carregando -> PerfilCarregando()

                else -> EstadoErro(
                    mensagem = estado.erro ?: "Não foi possível carregar o perfil.",
                    onTentarDeNovo = { viewModel.carregar(mentoraId) }
                )
            }
        }

        BottomNavigationBar(
            navController = navController,
            rotaAtual = "mentorship"
        )
    }
}


// ======================================================
// CONTEÚDO
// ======================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PerfilConteudo(
    mentora: MentoraResponse,
    horarios: List<HorarioResponse>?,
    onAgendar: () -> Unit
) {

    val areas = mentora.areas.orEmpty()
    val temAvaliacoes = mentora.totalAvaliacoes > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        AvatarMentora(
            nome = mentora.nome,
            fotoUrl = mentora.fotoUrl,
            tamanho = 88.dp,
            raio = 28.dp,
            tamanhoDaInicial = 34.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = mentora.nome,
            fontFamily = ManropeFamily,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            color = MentoriaCores.TextoEscuro
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = cargoEEmpresa(mentora),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = MentoriaCores.TextoMudo
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Estatistica(
                modifier = Modifier.weight(1f),
                valor = textoAnos(mentora.anosExperiencia),
                rotulo = "Experiência"
            )

            Estatistica(
                modifier = Modifier.weight(1f),
                valor = if (temAvaliacoes) "★ ${formatarNota(mentora.notaMedia)}" else "Nova",
                rotulo = "Nota média"
            )

            Estatistica(
                modifier = Modifier.weight(1f),
                valor = mentora.totalAvaliacoes.toString(),
                rotulo = if (mentora.totalAvaliacoes == 1) "Avaliação" else "Avaliações"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SecaoCartao(titulo = "Sobre") {

            Text(
                text = mentora.bio?.takeIf { it.isNotBlank() }
                    ?: "Esta mentora ainda não escreveu uma apresentação.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color(0xFF514C58)
            )
        }

        if (areas.isNotEmpty()) {

            Spacer(modifier = Modifier.height(12.dp))

            SecaoCartao(titulo = "Áreas de atuação") {

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    areas.forEach { area ->
                        TagArea(texto = area)
                    }
                }
            }
        }

        // horarios == null: não deu para consultar, então a tela não diz nada sobre disponibilidade
        if (horarios != null) {

            Spacer(modifier = Modifier.height(12.dp))

            SecaoCartao(titulo = "Próximos horários") {

                if (horarios.isEmpty()) {

                    Text(
                        text = "Sem horários disponíveis no momento. Volte em breve!",
                        fontSize = 13.sp,
                        color = MentoriaCores.TextoMudo
                    )

                } else {

                    horarios.take(HORARIOS_NA_PREVIA).forEach { horario ->

                        Text(
                            text = "${formatarDia(horario.inicio)} · ${formatarFaixaDeHorario(horario.inicio, horario.fim)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MentoriaCores.TextoEscuro,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (horarios.size > HORARIOS_NA_PREVIA) {

                        val restantes = horarios.size - HORARIOS_NA_PREVIA

                        Text(
                            text = if (restantes == 1) "e mais 1 horário" else "e mais $restantes horários",
                            fontSize = 12.sp,
                            color = MentoriaCores.TextoMudo
                        )
                    }
                }
            }
        }

        if (horarios == null || horarios.isNotEmpty()) {

            Spacer(modifier = Modifier.height(22.dp))

            GradientButton(
                text = "Agendar mentoria",
                onClick = onAgendar
            )
        }
    }
}

@Composable
private fun Estatistica(
    modifier: Modifier,
    valor: String,
    rotulo: String
) {

    val forma = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = valor,
            fontFamily = ManropeFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MentoriaCores.TextoEscuro,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = rotulo,
            fontSize = 10.sp,
            color = Color(0xFF8A8691),
            maxLines = 1
        )
    }
}

@Composable
private fun PerfilCarregando() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        EsqueletoBloco(altura = 140.dp)
        EsqueletoBloco(altura = 64.dp)
        EsqueletoBloco(altura = 110.dp)
        EsqueletoBloco(altura = 80.dp)
    }
}
