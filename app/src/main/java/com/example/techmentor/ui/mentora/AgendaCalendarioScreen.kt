package com.example.techmentor.ui.mentora

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.components.BotaoEntrarNaSala
import com.example.techmentor.ui.components.BotaoPrimario
import com.example.techmentor.ui.components.BotaoSecundario
import com.example.techmentor.ui.components.SeloDeStatus
import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.home.mentoriaAindaVale
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val LocaleBr: Locale = Locale.forLanguageTag("pt-BR")

private val CorConfirmada = Color(0xFF7C3AED)
private val CorAguardando = Color(0xFFF59E0B)
private val CorLivre = Color(0xFF3B82F6)
private val CorRealizada = Color(0xFFA19BAA)
private val Vermelho = Color(0xFFDC2626)

/**
 * A agenda da mentora em calendário: o mês com um ponto colorido em cada dia que tem algo, a lista do dia escolhido
 * e, ao tocar num item, uma folha com os detalhes e as ações (confirmar, reagendar, cancelar, entrar na sala, remover).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaCalendarioScreen(
    navController: NavController,
    viewModel: AgendaCalendarioViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    // O mês visível (guardado como o primeiro dia dele) e o dia escolhido ("2026-11-12")
    var mesMillis by rememberSaveable { mutableStateOf(primeiroDiaDoMes(Date()).timeInMillis) }
    var diaEscolhido by rememberSaveable { mutableStateOf(chaveDoDia(Date())) }

    var mentoriaAbertaId by rememberSaveable { mutableStateOf<Long?>(null) }
    var horarioAbertoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pedindoCancelar by rememberSaveable { mutableStateOf<Long?>(null) }
    var pedindoRemover by rememberSaveable { mutableStateOf<Long?>(null) }

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
        if (estado.semPerfil) navController.navigate("mentora/completar-perfil")
    }

    // Carrega ao abrir e ao voltar para a tela; com ela aberta, atualiza sozinha de tempos em tempos
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.carregar() }
    AtualizarAutomaticamente { viewModel.carregar() }

    val mes = calendarioDe(mesMillis)
    val hojeChave = chaveDoDia(Date())

    // Os itens abertos são procurados de novo a cada atualização, então a folha acompanha o que mudou
    val todos = estado.itensPorDia.values.flatten()
    val mentoriaAberta = todos.filterIsInstance<ItemMentoria>().firstOrNull { it.agendamento.id == mentoriaAbertaId }
    val horarioAberto = todos.filterIsInstance<ItemHorarioLivre>().firstOrNull { it.horario.id == horarioAbertoId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        TopBarMarca()

        when {

            estado.carregando -> Column(
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EsqueletoBloco(altura = 56.dp)
                EsqueletoBloco(altura = 300.dp)
                EsqueletoBloco(altura = 90.dp)
            }

            estado.erro != null && estado.itensPorDia.isEmpty() -> Column(modifier = Modifier.weight(1f)) {
                EstadoErro(
                    mensagem = estado.erro.orEmpty(),
                    onTentarDeNovo = { viewModel.carregar() }
                )
            }

            else -> PullToRefreshBox(
                isRefreshing = estado.atualizando,
                onRefresh = { viewModel.carregar(manual = true) },
                modifier = Modifier.weight(1f)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // CABEÇALHO
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Minha agenda",
                                fontFamily = ManropeFamily,
                                fontSize = 26.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.5).sp,
                                color = MentoriaCores.TextoEscuro
                            )
                            Text(
                                text = "Suas mentorias e horários livres, dia a dia.",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF797486)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        BotaoSecundario(
                            texto = "Horários",
                            cor = CorConfirmada,
                            modifier = Modifier.width(96.dp),
                            onClick = { navController.navigate("mentora/horarios") }
                        )
                    }

                    // CALENDÁRIO
                    CartaoDoCalendario(
                        mes = mes,
                        itensPorDia = estado.itensPorDia,
                        diaEscolhido = diaEscolhido,
                        hojeChave = hojeChave,
                        aoEscolherDia = { diaEscolhido = it },
                        aoMudarMes = { delta ->
                            mesMillis = (mes.clone() as Calendar).apply { add(Calendar.MONTH, delta) }.timeInMillis
                        },
                        aoIrParaHoje = {
                            mesMillis = primeiroDiaDoMes(Date()).timeInMillis
                            diaEscolhido = hojeChave
                        }
                    )

                    // O DIA ESCOLHIDO
                    val itensDoDia = estado.itensPorDia[diaEscolhido].orEmpty()

                    SecaoDoDia(
                        chave = diaEscolhido,
                        itens = itensDoDia,
                        podeAdicionar = diaEscolhido >= hojeChave,
                        aoAbrirMentoria = { mentoriaAbertaId = it },
                        aoAbrirHorario = { horarioAbertoId = it },
                        aoAdicionarHorario = { navController.navigate("mentora/horarios") }
                    )
                }
            }
        }

        BarraInferiorMentora(
            navController = navController,
            rotaAtual = "mentora/agenda"
        )
    }

    // ---- Folha de detalhes: mentoria ----

    if (mentoriaAberta != null) {
        ModalBottomSheet(
            onDismissRequest = {
                mentoriaAbertaId = null
                viewModel.limparErroAcao()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            FolhaDaMentoria(
                item = mentoriaAberta,
                ocupado = estado.acaoEmAndamentoId == mentoriaAberta.agendamento.id,
                erro = estado.erroAcao,
                aoConfirmar = { viewModel.confirmar(mentoriaAberta.agendamento.id) },
                aoReagendar = {
                    val a = mentoriaAberta.agendamento
                    mentoriaAbertaId = null
                    navController.navigate("reagendar/${a.id}/${a.mentoraId}")
                },
                aoCancelar = { pedindoCancelar = mentoriaAberta.agendamento.id }
            )
        }
    }

    // ---- Folha de detalhes: horário livre ----

    if (horarioAberto != null) {
        ModalBottomSheet(
            onDismissRequest = {
                horarioAbertoId = null
                viewModel.limparErroAcao()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            FolhaDoHorario(
                item = horarioAberto,
                ocupado = estado.acaoEmAndamentoId == horarioAberto.horario.id,
                erro = estado.erroAcao,
                aoRemover = { pedindoRemover = horarioAberto.horario.id }
            )
        }
    }

    // ---- Confirmações ----

    pedindoCancelar?.let { id ->
        val aluna = todos.filterIsInstance<ItemMentoria>().firstOrNull { it.agendamento.id == id }?.agendamento?.alunaNome
        AlertDialog(
            onDismissRequest = { pedindoCancelar = null },
            containerColor = Color.White,
            title = { Text("Cancelar esta mentoria?", fontWeight = FontWeight.Bold) },
            text = { Text("${aluna ?: "A aluna"} será avisada e o horário volta a ficar livre.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelar(id)
                    pedindoCancelar = null
                    mentoriaAbertaId = null
                }) { Text("Cancelar mentoria", color = Vermelho, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pedindoCancelar = null }) { Text("Voltar") } }
        )
    }

    pedindoRemover?.let { id ->
        AlertDialog(
            onDismissRequest = { pedindoRemover = null },
            containerColor = Color.White,
            title = { Text("Remover este horário?", fontWeight = FontWeight.Bold) },
            text = { Text("Ele deixa de aparecer para as alunas.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removerHorario(id)
                    pedindoRemover = null
                    horarioAbertoId = null
                }) { Text("Remover", color = Vermelho, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pedindoRemover = null }) { Text("Voltar") } }
        )
    }
}


// ======================================================
// CALENDÁRIO
// ======================================================

@Composable
private fun CartaoDoCalendario(
    mes: Calendar,
    itensPorDia: Map<String, List<ItemDaAgenda>>,
    diaEscolhido: String,
    hojeChave: String,
    aoEscolherDia: (String) -> Unit,
    aoMudarMes: (Int) -> Unit,
    aoIrParaHoje: () -> Unit
) {

    val forma = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(14.dp)
    ) {

        // Navegação do mês
        Row(verticalAlignment = Alignment.CenterVertically) {

            BotaoDeSeta(texto = "‹", descricao = "Mês anterior") { aoMudarMes(-1) }

            Text(
                text = SimpleDateFormat("MMMM 'de' yyyy", LocaleBr).format(mes.time)
                    .replaceFirstChar { it.uppercase() },
                modifier = Modifier.weight(1f),
                fontFamily = ManropeFamily,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MentoriaCores.TextoEscuro,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            BotaoDeSeta(texto = "›", descricao = "Próximo mês") { aoMudarMes(1) }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Ir para hoje",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = aoIrParaHoje)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = CorConfirmada
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Dias da semana
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { letra ->
                Text(
                    text = letra,
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9A96A3),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Semanas
        celulasDoMes(mes).chunked(7).forEach { semana ->
            Row(modifier = Modifier.fillMaxWidth()) {
                semana.forEach { dia ->
                    val chave = dia?.let { chaveDoDia(it) }
                    CelulaDoDia(
                        dia = dia,
                        itens = chave?.let { itensPorDia[it] }.orEmpty(),
                        selecionado = chave != null && chave == diaEscolhido,
                        hoje = chave != null && chave == hojeChave,
                        aoClicar = { if (chave != null) aoEscolherDia(chave) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legenda
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PontoELegenda(CorConfirmada, "Confirmada")
            PontoELegenda(CorAguardando, "Aguardando")
            PontoELegenda(CorLivre, "Livre")
            PontoELegenda(CorRealizada, "Realizada")
        }
    }
}

@Composable
private fun BotaoDeSeta(texto: String, descricao: String, aoClicar: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MentoriaCores.RoxoClaro)
            .clickable(role = Role.Button, onClick = aoClicar),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MentoriaCores.RoxoEscuro
        )
    }
}

@Composable
private fun CelulaDoDia(
    dia: Date?,
    itens: List<ItemDaAgenda>,
    selecionado: Boolean,
    hoje: Boolean,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {

        if (dia == null) return@Box

        val forma = RoundedCornerShape(12.dp)
        val numero = Calendar.getInstance().apply { time = dia }.get(Calendar.DAY_OF_MONTH)
        val cores = itens.map { corDoItem(it) }.distinct().take(3)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(forma)
                .background(
                    if (selecionado) Brush.linearGradient(listOf(CorConfirmada, Color(0xFF2563EB)))
                    else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                )
                .then(if (hoje && !selecionado) Modifier.border(1.5.dp, CorConfirmada, forma) else Modifier)
                .clickable(role = Role.Button, onClick = aoClicar),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = numero.toString(),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = if (hoje || selecionado) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (selecionado) Color.White else MentoriaCores.TextoEscuro
            )

            Row(
                modifier = Modifier.height(7.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                cores.forEach { cor ->
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (selecionado) Color.White else cor)
                    )
                }
            }
        }
    }
}

@Composable
private fun PontoELegenda(cor: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(cor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = texto, fontSize = 10.sp, lineHeight = 13.sp, color = Color(0xFF797486))
    }
}


// ======================================================
// O DIA ESCOLHIDO
// ======================================================

@Composable
private fun SecaoDoDia(
    chave: String,
    itens: List<ItemDaAgenda>,
    podeAdicionar: Boolean,
    aoAbrirMentoria: (Long) -> Unit,
    aoAbrirHorario: (Long) -> Unit,
    aoAdicionarHorario: () -> Unit
) {

    val data = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(chave) }.getOrNull()
    val titulo = data?.let { SimpleDateFormat("EEEE, d 'de' MMMM", LocaleBr).format(it) }
        ?.replaceFirstChar { it.uppercase() } ?: chave

    val mentorias = itens.count { it is ItemMentoria }
    val livres = itens.count { it is ItemHorarioLivre }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Column {
            Text(
                text = titulo,
                fontFamily = ManropeFamily,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MentoriaCores.TextoEscuro
            )
            Text(
                text = when {
                    itens.isEmpty() -> "Nada marcado"
                    else -> listOfNotNull(
                        if (mentorias > 0) "$mentorias ${if (mentorias == 1) "mentoria" else "mentorias"}" else null,
                        if (livres > 0) "$livres ${if (livres == 1) "horário livre" else "horários livres"}" else null
                    ).joinToString(" · ")
                },
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color(0xFF797486)
            )
        }

        if (itens.isEmpty()) {

            val forma = RoundedCornerShape(16.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(forma)
                    .background(Color.White)
                    .border(1.dp, MentoriaCores.Borda, forma)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (podeAdicionar) "Nenhuma mentoria ou horário neste dia." else "Nenhuma mentoria neste dia.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF797486)
                )

                if (podeAdicionar) {
                    BotaoSecundario(
                        texto = "Adicionar horário",
                        cor = CorConfirmada,
                        modifier = Modifier.width(180.dp),
                        onClick = aoAdicionarHorario
                    )
                }
            }

        } else {

            itens.forEach { item ->
                CartaoDoItem(
                    item = item,
                    aoClicar = {
                        when (item) {
                            is ItemMentoria -> aoAbrirMentoria(item.agendamento.id)
                            is ItemHorarioLivre -> aoAbrirHorario(item.horario.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CartaoDoItem(item: ItemDaAgenda, aoClicar: () -> Unit) {

    val forma = RoundedCornerShape(16.dp)
    val hora = SimpleDateFormat("HH:mm", LocaleBr)

    val titulo: String
    val subtitulo: String
    when (item) {
        is ItemMentoria -> {
            titulo = item.agendamento.alunaNome
            subtitulo = item.agendamento.assunto?.takeIf { it.isNotBlank() } ?: "Mentoria"
        }
        is ItemHorarioLivre -> {
            titulo = "Horário livre"
            subtitulo = "Disponível para agendamento"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .clickable(role = Role.Button, onClick = aoClicar)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.width(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = hora.format(item.inicio),
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MentoriaCores.TextoEscuro
            )
            item.fim?.let {
                Text(text = hora.format(it), fontSize = 11.sp, lineHeight = 14.sp, color = Color(0xFF9A96A3))
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .width(4.dp)
                .height(46.dp)
                .clip(CircleShape)
                .background(corDoItem(item))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = titulo,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MentoriaCores.TextoEscuro,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = subtitulo,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color(0xFF797486),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            SeloDoItem(item)
        }

        Text(text = "›", fontSize = 24.sp, lineHeight = 26.sp, color = Color(0xFF9A96A3))
    }
}

@Composable
private fun SeloDoItem(item: ItemDaAgenda) {
    when (item) {
        is ItemHorarioLivre -> SeloDeStatus("Livre", Color(0xFFDBEAFE), Color(0xFF1D4ED8))
        is ItemMentoria -> when (item.agendamento.status) {
            "AGENDADA" -> SeloDeStatus("Aguardando confirmação", Color(0xFFFEF3C7), Color(0xFFB45309))
            "CONFIRMADA" -> SeloDeStatus("Confirmada", Color(0xFFDCFCE7), Color(0xFF15803D))
            "CONCLUIDA" -> SeloDeStatus("Realizada", Color(0xFFF1F0F4), Color(0xFF6F6A76))
            else -> SeloDeStatus(item.agendamento.status, Color(0xFFF1F0F4), Color(0xFF6F6A76))
        }
    }
}


// ======================================================
// FOLHAS DE DETALHES
// ======================================================

@Composable
private fun FolhaDaMentoria(
    item: ItemMentoria,
    ocupado: Boolean,
    erro: String?,
    aoConfirmar: () -> Unit,
    aoReagendar: () -> Unit,
    aoCancelar: () -> Unit
) {

    val a = item.agendamento
    val aindaVale = mentoriaAindaVale(a)
    val aguardando = a.status == "AGENDADA"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF2563EB)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iniciaisDoNome(a.alunaNome),
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = a.alunaNome,
                    fontFamily = ManropeFamily,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MentoriaCores.TextoEscuro
                )
                Text(
                    text = descricaoDoHorario(item),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF797486)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SeloDoItem(item)
            if (!a.linkSala.isNullOrBlank()) {
                SeloDeStatus("Google Meet", Color(0xFFEDE9FE), Color(0xFF6D28D9))
            }
        }

        if (!a.assunto.isNullOrBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "ASSUNTO",
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    color = CorConfirmada
                )
                Text(
                    text = a.assunto,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MentoriaCores.TextoEscuro
                )
            }
        }

        BotaoEntrarNaSala(
            status = a.status,
            inicio = a.inicio,
            fim = a.fim,
            linkSala = a.linkSala,
            minutosAntes = a.minutosParaAbrirSala
        )

        if (aindaVale) {

            if (aguardando) {
                BotaoPrimario(
                    texto = if (ocupado) "Confirmando..." else "Confirmar mentoria",
                    habilitado = !ocupado,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = aoConfirmar
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotaoSecundario(
                    texto = "Reagendar",
                    cor = CorConfirmada,
                    habilitado = !ocupado,
                    modifier = Modifier.weight(1f),
                    onClick = aoReagendar
                )
                BotaoSecundario(
                    texto = "Cancelar",
                    cor = Vermelho,
                    habilitado = !ocupado,
                    modifier = Modifier.weight(1f),
                    onClick = aoCancelar
                )
            }
        }

        if (erro != null) {
            Text(text = erro, fontSize = 12.sp, lineHeight = 16.sp, color = Vermelho)
        }
    }
}

@Composable
private fun FolhaDoHorario(
    item: ItemHorarioLivre,
    ocupado: Boolean,
    erro: String?,
    aoRemover: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Column {
            Text(
                text = "Horário livre",
                fontFamily = ManropeFamily,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MentoriaCores.TextoEscuro
            )
            Text(
                text = descricaoDoHorario(item),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color(0xFF797486)
            )
        }

        SeloDoItem(item)

        Text(
            text = "Ninguém reservou este horário ainda. As alunas já conseguem vê-lo e agendar.",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = MentoriaCores.TextoEscuro
        )

        BotaoSecundario(
            texto = if (ocupado) "Removendo..." else "Remover horário",
            cor = Vermelho,
            habilitado = !ocupado,
            modifier = Modifier.fillMaxWidth(),
            onClick = aoRemover
        )

        if (erro != null) {
            Text(text = erro, fontSize = 12.sp, lineHeight = 16.sp, color = Vermelho)
        }
    }
}


// ======================================================
// APOIO
// ======================================================

private fun corDoItem(item: ItemDaAgenda): Color = when (item) {
    is ItemHorarioLivre -> CorLivre
    is ItemMentoria -> when (item.agendamento.status) {
        "AGENDADA" -> CorAguardando
        "CONCLUIDA" -> CorRealizada
        else -> CorConfirmada
    }
}

/** "Quinta, 12 de novembro · 14:00 – 15:00" */
private fun descricaoDoHorario(item: ItemDaAgenda): String {
    val dia = SimpleDateFormat("EEEE, d 'de' MMMM", LocaleBr).format(item.inicio).replaceFirstChar { it.uppercase() }
    val hora = SimpleDateFormat("HH:mm", LocaleBr)
    val faixa = item.fim?.let { "${hora.format(item.inicio)} – ${hora.format(it)}" } ?: hora.format(item.inicio)
    return "$dia · $faixa"
}

private fun iniciaisDoNome(nome: String): String {
    val partes = nome.trim().split(" ").filter { it.isNotBlank() }
    return when {
        partes.isEmpty() -> "?"
        partes.size == 1 -> partes[0].take(2).uppercase()
        else -> "${partes.first().first()}${partes.last().first()}".uppercase()
    }
}

/** O primeiro dia do mês da data, à meia-noite. */
private fun primeiroDiaDoMes(data: Date): Calendar =
    Calendar.getInstance().apply {
        time = data
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

private fun calendarioDe(millis: Long): Calendar =
    Calendar.getInstance().apply { timeInMillis = millis }

/** As células do mês em semanas de domingo a sábado: vazios (null) antes do dia 1 e depois do último dia. */
private fun celulasDoMes(primeiroDia: Calendar): List<Date?> {
    val vazios = primeiroDia.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val total = primeiroDia.getActualMaximum(Calendar.DAY_OF_MONTH)

    val dias: List<Date?> = (1..total).map { numero ->
        (primeiroDia.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, numero) }.time
    }

    val comInicio: List<Date?> = List<Date?>(vazios) { null } + dias
    return comInicio + List<Date?>((7 - comInicio.size % 7) % 7) { null }
}