package com.example.techmentor.ui.mentora

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.HorarioResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.components.AvisoDemo
import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.home.parseDataHora
import com.example.techmentor.ui.mentoria.ChipOpcao
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val LocaleBr: Locale = Locale.forLanguageTag("pt-BR")
private val UTC: TimeZone = TimeZone.getTimeZone("UTC")
private val DURACOES = listOf(30, 45, 60, 90)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgendaMentoraScreen(
    navController: NavController,
    viewModel: AgendaMentoraViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    // Campos do formulário "Novo horário"
    var dataMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var hora by rememberSaveable { mutableStateOf<Int?>(null) }
    var minuto by rememberSaveable { mutableStateOf<Int?>(null) }
    var duracao by rememberSaveable { mutableStateOf(60) }
    var tentou by rememberSaveable { mutableStateOf(false) }
    var mostrarData by rememberSaveable { mutableStateOf(false) }
    var mostrarHora by rememberSaveable { mutableStateOf(false) }

    fun sair() {
        Sessao.sair()
        navController.navigate("login") {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    LaunchedEffect(estado.sessaoExpirada) {
        if (estado.sessaoExpirada) sair()
    }

    // Mentora sem perfil: abre o formulário de primeiro acesso
    LaunchedEffect(estado.semPerfil) {
        if (estado.semPerfil) {
            navController.navigate("mentora/completar-perfil")
        }
    }

    // Carrega ao abrir e ao voltar para a tela; fora isso, só puxando para atualizar
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar()
    }

    // Com a tela aberta, atualiza sozinha de tempos em tempos
    AtualizarAutomaticamente { viewModel.carregar() }

    // ---- Seletores de data e hora ----

    if (mostrarData) {
        val estadoData = rememberDatePickerState(
            initialSelectedDateMillis = dataMillis,
            selectableDates = SomenteDeHojeEmDiante
        )

        DatePickerDialog(
            onDismissRequest = { mostrarData = false },
            confirmButton = {
                TextButton(onClick = {
                    dataMillis = estadoData.selectedDateMillis
                    mostrarData = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarData = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estadoData)
        }
    }

    if (mostrarHora) {
        val estadoHora = rememberTimePickerState(
            initialHour = hora ?: 9,
            initialMinute = minuto ?: 0,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { mostrarHora = false },
            confirmButton = {
                TextButton(onClick = {
                    hora = estadoHora.hour
                    minuto = estadoHora.minute
                    mostrarHora = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarHora = false }) { Text("Cancelar") }
            },
            text = { TimePicker(state = estadoHora) }
        )
    }

    // ---- Tela ----

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        TopBarMarca(navController = navController)

        Box(modifier = Modifier.weight(1f)) {

            when {
                estado.carregando -> Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EsqueletoBloco(altura = 56.dp)
                    EsqueletoBloco(altura = 150.dp)
                    EsqueletoBloco(altura = 70.dp)
                }

                estado.erro != null && estado.mentoraId == null -> EstadoErro(
                    mensagem = estado.erro.orEmpty(),
                    onTentarDeNovo = { viewModel.carregar() }
                )

                else -> PullToRefreshBox(
                    isRefreshing = estado.atualizando,
                    onRefresh = { viewModel.carregar(manual = true) },
                    modifier = Modifier.fillMaxSize()
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Column {

                            Text(
                                text = "Sua agenda",
                                fontFamily = ManropeFamily,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.5).sp,
                                color = MentoriaCores.TextoEscuro
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = "Horários que as alunas podem agendar com você.",
                                fontSize = 13.sp,
                                color = Color(0xFF797486)
                            )
                        }

                        // ---- Novo horário ----

                        val forma = RoundedCornerShape(17.dp)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(forma)
                                .background(Color.White)
                                .border(1.dp, MentoriaCores.Borda, forma)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            Text(
                                text = "Novo horário",
                                fontFamily = ManropeFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MentoriaCores.TextoEscuro
                            )

                            CampoDeEscolha(
                                rotulo = "Data",
                                valor = dataMillis?.let { formatarDataEscolhida(it) },
                                dica = "Escolher a data",
                                onClick = { mostrarData = true }
                            )

                            CampoDeEscolha(
                                rotulo = "Hora de início",
                                valor = if (hora != null && minuto != null) "%02d:%02d".format(hora, minuto) else null,
                                dica = "Escolher a hora",
                                onClick = { mostrarHora = true }
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                                Text(
                                    text = "Duração",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF514C58)
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DURACOES.forEach { minutos ->
                                        ChipOpcao(
                                            texto = "$minutos min",
                                            selecionado = duracao == minutos,
                                            onClick = { duracao = minutos }
                                        )
                                    }
                                }
                            }

                            if (tentou && (dataMillis == null || hora == null || minuto == null)) {
                                Mensagem("Escolha a data e a hora.")
                            }

                            if (estado.erroAcao != null) Mensagem(estado.erroAcao.orEmpty())

                            GradientButton(
                                text = if (estado.adicionando) "Adicionando..." else "Adicionar horário",
                                onClick = {
                                    tentou = true
                                    val d = dataMillis
                                    val h = hora
                                    val m = minuto

                                    if (d != null && h != null && m != null && !estado.adicionando) {
                                        val (inicio, fim) = montarIntervalo(d, h, m, duracao)
                                        viewModel.adicionar(inicio, fim) {
                                            dataMillis = null
                                            hora = null
                                            minuto = null
                                            tentou = false
                                        }
                                    }
                                }
                            )
                        }

                        // ---- Lista ----

                        Text(
                            text = "Horários livres",
                            modifier = Modifier.padding(top = 6.dp),
                            fontFamily = ManropeFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.3).sp,
                            color = MentoriaCores.TextoEscuro
                        )

                        AvisoDemo(texto = "Os horários já reservados saem desta lista e aparecem em Início, em Próximas mentorias.")

                        if (estado.horarios.isEmpty()) {

                            Text(
                                text = "Você ainda não tem horários livres. Adicione um acima para as alunas poderem agendar.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MentoriaCores.TextoMudo
                            )

                        } else {

                            estado.horarios.forEach { horario ->
                                LinhaDeHorario(
                                    horario = horario,
                                    removendo = estado.removendoId == horario.id,
                                    onRemover = { viewModel.remover(horario.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        BarraInferiorMentora(
            navController = navController,
            rotaAtual = "mentora/agenda"
        )
    }
}


// ======================================================
// PEDAÇOS DA TELA
// ======================================================

@Composable
private fun CampoDeEscolha(
    rotulo: String,
    valor: String?,
    dica: String,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(12.dp)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        Text(
            text = rotulo,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AuthColors.TextLabel
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(47.dp)
                .clip(forma)
                .background(AuthColors.FieldBackground)
                .border(1.dp, AuthColors.FieldBorder, forma)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 13.dp),
            contentAlignment = Alignment.CenterStart
        ) {

            Text(
                text = valor ?: dica,
                fontSize = 13.sp,
                fontWeight = if (valor != null) FontWeight.Bold else FontWeight.Normal,
                color = if (valor != null) AuthColors.TextLabel else AuthColors.TextHint
            )
        }
    }
}

@Composable
private fun LinhaDeHorario(
    horario: HorarioResponse,
    removendo: Boolean,
    onRemover: () -> Unit
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
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(horizontal = 14.dp, vertical = 10.dp),
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

        Text(
            text = if (removendo) "Removendo..." else "Remover",
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = !removendo, role = Role.Button, onClick = onRemover)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFDC2626)
        )
    }
}

@Composable
private fun Mensagem(texto: String) {
    Text(
        text = texto,
        fontSize = 11.sp,
        color = AuthColors.Error
    )
}


// ======================================================
// DATAS
// ======================================================

/** O seletor de data só deixa escolher de hoje em diante. */
@OptIn(ExperimentalMaterial3Api::class)
private object SomenteDeHojeEmDiante : SelectableDates {

    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= inicioDeHojeEmUtc()

    override fun isSelectableYear(year: Int): Boolean = year >= Calendar.getInstance().get(Calendar.YEAR)
}

/** O seletor trabalha com a meia-noite UTC do dia; este é o "hoje" nesse mesmo formato. */
private fun inicioDeHojeEmUtc(): Long {
    val local = Calendar.getInstance()
    return Calendar.getInstance(UTC).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** "Qui, 12 de nov de 2026" */
private fun formatarDataEscolhida(utcMillis: Long): String =
    SimpleDateFormat("EEE, d 'de' MMM 'de' yyyy", LocaleBr)
        .apply { timeZone = UTC }
        .format(Date(utcMillis))
        .replaceFirstChar { it.uppercase() }

/** Junta a data (do seletor), a hora e a duração nos dois textos que a API espera: "2026-11-10T14:00:00-03:00". */
private fun montarIntervalo(dataUtcMillis: Long, hora: Int, minuto: Int, duracaoEmMinutos: Int): Pair<String, String> {

    val dia = Calendar.getInstance(UTC).apply { timeInMillis = dataUtcMillis }

    val calendario = Calendar.getInstance().apply {
        clear()
        set(dia.get(Calendar.YEAR), dia.get(Calendar.MONTH), dia.get(Calendar.DAY_OF_MONTH), hora, minuto, 0)
    }

    // XXX = o fuso do celular ("-03:00"): a API converte para o fuso do TechMentor
    val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)

    val inicio = formato.format(calendario.time)
    calendario.add(Calendar.MINUTE, duracaoEmMinutos)
    val fim = formato.format(calendario.time)

    return inicio to fim
}