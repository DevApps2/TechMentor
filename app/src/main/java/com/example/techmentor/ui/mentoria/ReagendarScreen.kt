package com.example.techmentor.ui.mentoria

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.HorarioResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.components.AvisoDemo
import com.example.techmentor.ui.home.BottomNavigationBar
import com.example.techmentor.ui.home.parseDataHora
import com.example.techmentor.ui.mentora.BarraInferiorMentora
import com.example.techmentor.ui.theme.ManropeFamily
import java.text.SimpleDateFormat
import java.util.Locale

private val LocaleBr: Locale = Locale.forLanguageTag("pt-BR")

/** Escolher um novo horário para uma mentoria já marcada. Serve para a aluna e para a mentora. */
@Composable
fun ReagendarScreen(
    navController: NavController,
    agendamentoId: Long,
    mentoraId: Long,
    viewModel: ReagendarViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(mentoraId) {
        viewModel.carregar(mentoraId)
    }

    // Deu certo: volta para a tela de quem abriu (ela recarrega sozinha ao reaparecer)
    LaunchedEffect(estado.concluido) {
        if (estado.concluido) navController.popBackStack()
    }

    LaunchedEffect(estado.sessaoExpirada) {
        if (estado.sessaoExpirada) {
            Sessao.sair()
            navController.navigate("login") {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        CabecalhoVoltar(
            titulo = "Reagendar mentoria",
            onVoltar = { navController.popBackStack() }
        )

        when {

            estado.carregando -> Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EsqueletoBloco(altura = 56.dp)
                EsqueletoBloco(altura = 56.dp)
                EsqueletoBloco(altura = 56.dp)
            }

            estado.erro != null && estado.horarios.isEmpty() -> Column(modifier = Modifier.weight(1f)) {
                EstadoErro(
                    mensagem = estado.erro.orEmpty(),
                    onTentarDeNovo = { viewModel.carregar(mentoraId) }
                )
            }

            else -> Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Column {

                    Text(
                        text = "Escolha um novo horário",
                        fontFamily = ManropeFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.4).sp,
                        color = MentoriaCores.TextoEscuro
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "O horário atual volta a ficar livre.",
                        fontSize = 13.sp,
                        color = Color(0xFF797486)
                    )
                }

                // Quem reagenda define quem precisa confirmar
                AvisoDemo(
                    texto = if (Sessao.ehMentora) {
                        "A aluna será avisada do novo horário e a mentoria continua confirmada."
                    } else {
                        "A mentora será avisada e precisará confirmar o novo horário."
                    }
                )

                if (estado.horarios.isEmpty()) {

                    Text(
                        text = "Não há outros horários livres agora. Tente de novo mais tarde ou cancele a mentoria.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MentoriaCores.TextoMudo
                    )

                } else {

                    estado.horarios.forEach { horario ->
                        LinhaDeHorarioParaEscolher(
                            horario = horario,
                            selecionado = estado.selecionadoId == horario.id,
                            onClick = { viewModel.selecionar(horario.id) }
                        )
                    }

                    if (estado.erroEnvio != null) {
                        Text(
                            text = estado.erroEnvio.orEmpty(),
                            fontSize = 11.sp,
                            color = AuthColors.Error
                        )
                    }

                    GradientButton(
                        text = if (estado.enviando) "Reagendando..." else "Confirmar novo horário",
                        onClick = { viewModel.confirmar(agendamentoId, mentoraId) }
                    )
                }
            }
        }

        if (Sessao.ehMentora) {
            BarraInferiorMentora(navController = navController, rotaAtual = "mentora")
        } else {
            BottomNavigationBar(navController = navController, rotaAtual = "home")
        }
    }
}

@Composable
private fun LinhaDeHorarioParaEscolher(
    horario: HorarioResponse,
    selecionado: Boolean,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(14.dp)
    val inicio = parseDataHora(horario.inicio)
    val fim = parseDataHora(horario.fim)

    val dia = inicio?.let {
        SimpleDateFormat("EEE, d 'de' MMM", LocaleBr).format(it).replaceFirstChar { c -> c.uppercase() }
    } ?: horario.inicio

    val faixa = if (inicio != null && fim != null) {
        val formato = SimpleDateFormat("HH:mm", LocaleBr)
        "${formato.format(inicio)} às ${formato.format(fim)}"
    } else {
        ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(if (selecionado) MentoriaCores.RoxoClaro else Color.White)
            .border(1.dp, if (selecionado) MentoriaCores.Roxo else MentoriaCores.Borda, forma)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(modifier = Modifier.weight(1f)) {

            Text(
                text = dia,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20202A)
            )

            Text(
                text = faixa,
                fontSize = 11.sp,
                color = MentoriaCores.TextoMudo
            )
        }

        if (selecionado) {
            Text(
                text = "✓",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MentoriaCores.Roxo
            )
        }
    }
}