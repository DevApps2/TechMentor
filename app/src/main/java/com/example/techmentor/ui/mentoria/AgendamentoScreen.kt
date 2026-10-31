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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.AgendamentoResponse
import com.example.techmentor.data.HorarioResponse
import com.example.techmentor.data.MentoraResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.AuthField
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.auth.OutlinedAuthButton
import com.example.techmentor.ui.home.descricaoDaMentoria
import com.example.techmentor.ui.theme.ManropeFamily

// Mesmo limite do backend para o assunto (@Size(max = 500))
private const val LIMITE_ASSUNTO = 500


@Composable
fun AgendamentoScreen(
    navController: NavController,
    mentoraId: Long,
    viewModel: AgendamentoViewModel = viewModel()
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

    // Carrega ao abrir e ao voltar para a tela (os horários mudam quando outra pessoa reserva)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar(mentoraId)
    }

    // Esta tela não tem a barra inferior (o teclado ocuparia o lugar dela), então ela mesma respeita a barra do sistema e o teclado
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {

        CabecalhoVoltar(
            titulo = "Agendar mentoria",
            onVoltar = { navController.popBackStack() }
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {

            val criado = estado.agendamentoCriado
            val mentora = estado.mentora

            when {
                criado != null -> AgendamentoSucesso(
                    agendamento = criado,
                    onVoltarAoInicio = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onVerMentoras = {
                        navController.navigate("mentorship") {
                            popUpTo("mentorship") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )

                mentora != null -> AgendamentoFormulario(
                    mentora = mentora,
                    horarios = estado.horarios,
                    horarioSelecionadoId = estado.horarioSelecionadoId,
                    agendando = estado.agendando,
                    erroAgendamento = estado.erroAgendamento,
                    onSelecionarHorario = viewModel::selecionarHorario,
                    onConfirmar = viewModel::agendar,
                    onVerMentoras = {
                        navController.navigate("mentorship") {
                            popUpTo("mentorship") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )

                estado.carregando -> AgendamentoCarregando()

                else -> EstadoErro(
                    mensagem = estado.erro ?: "Não foi possível carregar os horários.",
                    onTentarDeNovo = { viewModel.carregar(mentoraId) }
                )
            }
        }
    }
}


// ======================================================
// FORMULÁRIO
// ======================================================

@Composable
private fun AgendamentoFormulario(
    mentora: MentoraResponse,
    horarios: List<HorarioResponse>,
    horarioSelecionadoId: Long?,
    agendando: Boolean,
    erroAgendamento: String?,
    onSelecionarHorario: (Long) -> Unit,
    onConfirmar: (String) -> Unit,
    onVerMentoras: () -> Unit
) {

    var assunto by rememberSaveable { mutableStateOf("") }
    val foco = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp)
    ) {

        ResumoDaMentora(mentora = mentora)

        Spacer(modifier = Modifier.height(16.dp))

        SecaoCartao(titulo = "Escolha um horário") {

            if (horarios.isEmpty()) {

                Text(
                    text = "Sem horários disponíveis no momento. Volte em breve!",
                    fontSize = 13.sp,
                    color = MentoriaCores.TextoMudo
                )

            } else {

                SeletorDeHorarios(
                    horarios = horarios,
                    selecionadoId = horarioSelecionadoId,
                    onSelecionar = onSelecionarHorario
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sem horário livre não há o que confirmar: só dá para escolher outra mentora
        if (horarios.isEmpty()) {

            OutlinedAuthButton(
                text = "Ver outras mentoras",
                onClick = onVerMentoras
            )

        } else {

            AuthField(
                label = "Assunto (opcional)",
                value = assunto,
                onValueChange = { if (it.length <= LIMITE_ASSUNTO) assunto = it },
                placeholder = "Ex.: dúvidas sobre transição de carreira",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                keyboardActions = KeyboardActions.Default,
                multilinha = true
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${assunto.length}/$LIMITE_ASSUNTO",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                color = Color(0xFF8A8691)
            )

            if (erroAgendamento != null) {

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = erroAgendamento,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuthColors.Error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            GradientButton(
                text = if (agendando) "Agendando..." else "Confirmar agendamento",
                onClick = {
                    foco.clearFocus()
                    onConfirmar(assunto)
                }
            )
        }
    }
}

@Composable
private fun ResumoDaMentora(mentora: MentoraResponse) {

    val forma = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
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
                text = "Mentoria com ${mentora.nome}",
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
        }
    }
}

/** Os horários agrupados por dia; o escolhido fica roxo. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeletorDeHorarios(
    horarios: List<HorarioResponse>,
    selecionadoId: Long?,
    onSelecionar: (Long) -> Unit
) {

    // a lista já vem em ordem; "2026-11-12T10:00:00".take(10) = "2026-11-12" separa os dias
    val porDia = horarios.groupBy { it.inicio.take(10) }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        porDia.values.forEach { doDia ->

            Column {

                Text(
                    text = formatarDia(doDia.first().inicio),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MentoriaCores.TextoMudo
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    doDia.forEach { horario ->
                        ChipOpcao(
                            texto = formatarFaixaDeHorario(horario.inicio, horario.fim),
                            selecionado = horario.id == selecionadoId,
                            onClick = { onSelecionar(horario.id) }
                        )
                    }
                }
            }
        }
    }
}


// ======================================================
// SUCESSO
// ======================================================

@Composable
private fun AgendamentoSucesso(
    agendamento: AgendamentoResponse,
    onVoltarAoInicio: () -> Unit,
    onVerMentoras: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFDCFCE7)),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "✓",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = AuthColors.Success
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Mentoria agendada!",
            fontFamily = ManropeFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MentoriaCores.TextoEscuro
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Mentoria com ${agendamento.mentoraNome}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFF20202A)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = descricaoDaMentoria(agendamento),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = MentoriaCores.TextoMudo
        )

        Spacer(modifier = Modifier.height(28.dp))

        GradientButton(
            text = "Voltar ao início",
            onClick = onVoltarAoInicio
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedAuthButton(
            text = "Ver outras mentoras",
            onClick = onVerMentoras
        )
    }
}

@Composable
private fun AgendamentoCarregando() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        EsqueletoBloco(altura = 78.dp)
        EsqueletoBloco(altura = 170.dp)
        EsqueletoBloco(altura = 90.dp)
    }
}
