package com.example.techmentor.ui.parceira

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.ParceiraCursoRequest
import com.example.techmentor.data.ParceiraCursoResponse
import com.example.techmentor.data.ParceiraPerfilResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha
import com.example.techmentor.ui.notificacoes.SinoDeNotificacoes

private val Fundo = Color(0xFFF7F6FA)
private val Tinta = Color(0xFF17152A)
private val Roxo = Color(0xFF5630C5)
private val RoxoClaro = Color(0xFFEDE8FF)
private val Cinza = Color(0xFF777384)
private val Borda = Color(0xFFEEECF3)

private enum class AbaParceira(val rotulo: String, val icone: Icone) {
    INICIO("Início", Icone.CASA),
    CURSOS("Meus Cursos", Icone.LIVRO),
    ESTATISTICAS("Estatísticas", Icone.ESTATISTICAS),
    CONFIGURACOES("Configurações", Icone.CONFIGURACOES)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParceiraHomeScreen(
    navController: NavController,
    cursoIdInicial: Long? = null,
    viewModel: ParceiraHomeViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    var aba by remember { mutableStateOf(AbaParceira.INICIO) }
    var mostrarCriacao by remember { mutableStateOf(false) }
    var cursoSelecionado by remember { mutableStateOf<ParceiraCursoResponse?>(null) }
    var cursoParaNovoModulo by remember { mutableStateOf<Long?>(null) }
    var moduloParaNovaAula by remember { mutableStateOf<Long?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.carregar() }

    LaunchedEffect(cursoIdInicial, estado.carregando) {
        val id = cursoIdInicial
        if (id != null && !estado.carregando && cursoSelecionado == null) {
            estado.cursos.firstOrNull { it.id == id }?.let {
                cursoSelecionado = it
                aba = AbaParceira.CURSOS
            }
        }
    }

    LaunchedEffect(estado.sessaoExpirada) {
        if (estado.sessaoExpirada) {
            Sessao.sair()
            navController.navigate("login") { popUpTo(0) { inclusive = true } }
        }
    }
    LaunchedEffect(estado.cursoCriado) {
        if (estado.cursoCriado) {
            mostrarCriacao = false
            aba = AbaParceira.CURSOS
            viewModel.limparResultadoCriacao()
        }
    }

    cursoSelecionado?.let { selecionado ->
        val curso = estado.cursos.firstOrNull { it.id == selecionado.id } ?: selecionado
        DetalheCursoDialog(
            curso = curso,
            editavel = estado.perfil?.situacao == "ATIVA" && curso.situacao == "RASCUNHO",
            enviando = estado.acaoCursoEmAndamento,
            erro = estado.erroAcaoCurso,
            aoAdicionarModulo = { cursoParaNovoModulo = curso.id },
            aoAdicionarAula = { moduloParaNovaAula = it },
            aoEnviarRevisao = { viewModel.enviarParaRevisao(curso.id) },
            aoFechar = { cursoSelecionado = null; viewModel.limparErroAcaoCurso() }
        )
    }

    cursoParaNovoModulo?.let { cursoId ->
        DialogoNovoModulo(
            enviando = estado.acaoCursoEmAndamento,
            erro = estado.erroAcaoCurso,
            aoSalvar = { titulo -> viewModel.criarModulo(cursoId, titulo); cursoParaNovoModulo = null },
            aoFechar = { cursoParaNovoModulo = null; viewModel.limparErroAcaoCurso() }
        )
    }

    moduloParaNovaAula?.let { moduloId ->
        DialogoNovaAula(
            enviando = estado.acaoCursoEmAndamento,
            erro = estado.erroAcaoCurso,
            aoSalvar = { titulo, minutos, url -> viewModel.criarAula(moduloId, titulo, minutos, url); moduloParaNovaAula = null },
            aoFechar = { moduloParaNovaAula = null; viewModel.limparErroAcaoCurso() }
        )
    }

    if (mostrarCriacao) {
        DialogoNovoCurso(
            enviando = estado.criandoCurso,
            erro = estado.erroCriacao,
            aoCriar = viewModel::criarCurso,
            aoFechar = {
                mostrarCriacao = false
                viewModel.limparResultadoCriacao()
            }
        )
    }

    Column(Modifier.fillMaxSize().background(Fundo)) {
        Box(Modifier.weight(1f)) {
            when {
                estado.perfil != null -> PullToRefreshBox(
                    isRefreshing = estado.atualizando,
                    onRefresh = { viewModel.carregar(manual = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    ConteudoParceira(
                        estado = estado,
                        aba = aba,
                        aoTrocarAba = { aba = it },
                        aoAbrirNotificacoes = { navController.navigate("notificacoes") },
                        aoSair = {
                            Sessao.sair()
                            navController.navigate("login") { popUpTo(0) { inclusive = true } }
                        },
                        aoAbrirCurso = { cursoSelecionado = it },
                        aoCriarCurso = { mostrarCriacao = true },
                        aoTentarNovamente = { viewModel.carregar(manual = true) }
                    )
                }

                estado.carregando -> CarregandoParceira()

                else -> FalhaParceira(
                    mensagem = estado.erro ?: "Não foi possível carregar sua área de parceira.",
                    aoTentarNovamente = { viewModel.carregar(manual = true) }
                )
            }
        }
        BarraInferiorParceira(abaAtual = aba, aoSelecionar = { aba = it })
    }
}

@Composable
private fun ConteudoParceira(
    estado: ParceiraHomeUiState,
    aba: AbaParceira,
    aoTrocarAba: (AbaParceira) -> Unit,
    aoAbrirNotificacoes: () -> Unit,
    aoAbrirCurso: (ParceiraCursoResponse) -> Unit,
    aoCriarCurso: () -> Unit,
    aoTentarNovamente: () -> Unit,
    aoSair: () -> Unit
) {
    val perfil = estado.perfil ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeroParceira(perfil = perfil, aoAbrirNotificacoes = aoAbrirNotificacoes)
        when (aba) {
            AbaParceira.INICIO -> InicioParceira(
                perfil = perfil,
                cursos = estado.cursos,
                metricas = estado.metricas,
                aoVerCursos = { aoTrocarAba(AbaParceira.CURSOS) },
                aoVerEstatisticas = { aoTrocarAba(AbaParceira.ESTATISTICAS) },
                aoAbrirCurso = aoAbrirCurso,
                aoCriarCurso = aoCriarCurso,
                aoTentarNovamente = aoTentarNovamente
            )
            AbaParceira.CURSOS -> CursosParceira(
                perfil = perfil,
                cursos = estado.cursos,
                aoAbrirCurso = aoAbrirCurso,
                aoCriarCurso = aoCriarCurso
            )
            AbaParceira.ESTATISTICAS -> EstatisticasParceira(
                cursos = estado.cursos,
                metricas = estado.metricas
            )
            AbaParceira.CONFIGURACOES -> ConfiguracoesParceira(perfil, aoSair = aoSair)
        }
    }
}

@Composable
private fun HeroParceira(perfil: ParceiraPerfilResponse, aoAbrirNotificacoes: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF4723A7), Color(0xFF6538D1), Color(0xFF4D32BD))))
    ) {
        Box(Modifier.align(Alignment.CenterEnd).offset(x = 45.dp, y = 38.dp).size(170.dp).clip(CircleShape).background(Color.White.copy(alpha = .07f)))
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 22.dp, end = 18.dp, top = 12.dp, bottom = 25.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconeLinha(Icone.CHAPEU, Color.White, tamanho = 38.dp, espessura = 2.4f)
                Spacer(Modifier.width(12.dp))
                Text("Parceiros", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                SinoDeNotificacoes(onClick = aoAbrirNotificacoes)
            }
            Spacer(Modifier.height(19.dp))
            Text("Olá, ${perfil.nome.split(" ").firstOrNull().orEmpty()}!", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("Aqui está o resumo das suas atividades.", color = Color.White.copy(alpha = .84f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun InicioParceira(
    perfil: ParceiraPerfilResponse,
    cursos: List<ParceiraCursoResponse>,
    metricas: Map<Long, com.example.techmentor.data.MetricasParceiraCursoResponse>,
    aoVerCursos: () -> Unit,
    aoVerEstatisticas: () -> Unit,
    aoAbrirCurso: (ParceiraCursoResponse) -> Unit,
    aoCriarCurso: () -> Unit,
    aoTentarNovamente: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 16.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        CartaoParceira(perfil)
        LinhaDeResumo(cursos)
        EstatisticasCompactas(cursos, metricas, aoVerEstatisticas)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Seus cursos", color = Tinta, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("Ver todos  ›", color = Roxo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(role = Role.Button, onClick = aoVerCursos))
        }
        if (cursos.isEmpty()) {
            EmptyStateParceira("Seus cursos aparecerão aqui", "Crie o primeiro curso e organize o conteúdo para enviar à revisão.")
        } else {
            CursoCompacto(cursos.first(), aoAbrirCurso)
        }
        AvisoSituacaoConta(perfil)
        BotaoCriarCurso(permitido = perfil.situacao == "ATIVA", onClick = aoCriarCurso)
        if (perfil.situacao == "ATIVA" && cursos.any { it.situacao == "RASCUNHO" }) {
            Text("Complete os módulos e as aulas de um rascunho antes de enviá-lo para revisão.", color = Cinza, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

@Composable
private fun CartaoParceira(perfil: ParceiraPerfilResponse) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 5.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(RoxoClaro), contentAlignment = Alignment.Center) {
                IconeLinha(Icone.PREDIO, Roxo, tamanho = 27.dp, espessura = 2.1f)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(perfil.nome, color = Tinta, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Parceiro desde ${perfil.criadaEm?.take(4) ?: "—"}", color = Cinza, fontSize = 13.sp)
            }
            Text("›", color = Tinta, fontSize = 31.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun LinhaDeResumo(cursos: List<ParceiraCursoResponse>) {
    val dados = listOf(
        Triple(Icone.LIVRO, cursos.size.toString(), "Cursos\ncadastrados"),
        Triple(Icone.CHECK, cursos.count { it.situacao == "PUBLICADO" }.toString(), "Publicados"),
        Triple(Icone.RELOGIO, cursos.count { it.situacao == "EM_REVISAO" }.toString(), "Em revisão"),
        Triple(Icone.ATUALIZAR, cursos.count { it.situacao == "RASCUNHO" && !it.comentarioRevisao.isNullOrBlank() }.toString(), "Devolvidos")
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        dados.forEachIndexed { index, item ->
            val cor = when (index) { 0 -> Roxo; 1 -> Color(0xFF31B77A); 2 -> Color(0xFFED9D35); else -> Color(0xFF348DDA) }
            Surface(Modifier.weight(1f), shape = RoundedCornerShape(17.dp), color = Color.White, shadowElevation = 2.dp) {
                Column(Modifier.fillMaxWidth().height(116.dp).padding(horizontal = 6.dp, vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconeLinha(item.first, cor, tamanho = 27.dp, espessura = 2.2f)
                    Spacer(Modifier.height(2.dp))
                    Text(item.second, color = Tinta, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text(item.third, color = Tinta, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun EstatisticasCompactas(
    cursos: List<ParceiraCursoResponse>,
    metricas: Map<Long, com.example.techmentor.data.MetricasParceiraCursoResponse>,
    aoVerDetalhes: () -> Unit
) {
    val publicados = cursos.filter { it.situacao == "PUBLICADO" }.mapNotNull { metricas[it.id] }
    val matriculas = publicados.sumOf { it.matriculas }
    val concluintes = publicados.sumOf { it.concluintes }
    val taxa = if (matriculas == 0L) 0 else ((concluintes * 100) / matriculas).toInt().coerceIn(0, 100)
    Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = aoVerDetalhes)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 17.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Suas estatísticas", color = Tinta, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Ver detalhes  ›", color = Roxo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BlocoEstatistica("$matriculas", "Matrículas", Roxo, Modifier.weight(1f))
                SeparadorVertical()
                BlocoEstatistica("$concluintes", "Conclusões", Color(0xFFE5A132), Modifier.weight(1f))
                SeparadorVertical()
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawArc(Color(0xFFE9E6F1), -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                            drawArc(Roxo, -90f, 3.6f * taxa, false, style = androidx.compose.ui.graphics.drawscope.Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                        }
                        Text("$taxa%", color = Tinta, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Taxa de conclusão", color = Cinza, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun BlocoEstatistica(valor: String, titulo: String, cor: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        IconeLinha(if (titulo == "Matrículas") Icone.USUARIOS else Icone.TROFEU, cor, tamanho = 28.dp, espessura = 2.2f)
        Spacer(Modifier.height(5.dp))
        Text(valor, color = Tinta, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(titulo, color = Cinza, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun SeparadorVertical() {
    Box(Modifier.width(1.dp).height(74.dp).background(Borda))
}

@Composable
private fun CursoCompacto(curso: ParceiraCursoResponse, onClick: (ParceiraCursoResponse) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onClick(curso) }
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 104.dp, height = 88.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(Color(0xFFDED8EF), Color(0xFFCFC7E5)))), contentAlignment = Alignment.Center) {
                IconeLinha(Icone.PLAY, Roxo, tamanho = 38.dp, espessura = 2.6f)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(curso.titulo, color = Tinta, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
                StatusCurso(curso.situacao)
                Text("Por ${curso.instituicao ?: "sua instituição"}", color = Cinza, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("⋮", color = Cinza, fontSize = 21.sp, modifier = Modifier.padding(start = 8.dp, bottom = 35.dp))
        }
    }
}

@Composable
private fun StatusCurso(situacao: String) {
    val (label, cor, fundo) = when (situacao) {
        "PUBLICADO" -> Triple("Publicado", Color(0xFF23865D), Color(0xFFE2F5EC))
        "EM_REVISAO" -> Triple("Em revisão", Color(0xFF9D620C), Color(0xFFFFF2D9))
        "RASCUNHO" -> Triple("Rascunho", Roxo, RoxoClaro)
        "ARQUIVADO" -> Triple("Recusado", Color(0xFFB43A45), Color(0xFFFFE9EB))
        else -> Triple(situacao.lowercase().replaceFirstChar { it.uppercase() }, Cinza, Color(0xFFF0EFF3))
    }
    Text(label, color = cor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(fundo).padding(horizontal = 8.dp, vertical = 3.dp))
}

@Composable
private fun AvisoSituacaoConta(perfil: ParceiraPerfilResponse) {
    when (perfil.situacao) {
        "CONVIDADA" -> AvisoParceira(
            titulo = "Cadastro em verificação",
            texto = "A equipe está conferindo os dados da instituição. Você poderá criar cursos assim que a conta for ativada.",
            cor = Color(0xFFFFF5DF), icone = Icone.RELOGIO, iconeCor = Color(0xFFE19A24)
        )
        "SUSPENSA" -> AvisoParceira(
            titulo = "Conta suspensa",
            texto = perfil.motivoDaSuspensao?.takeIf { it.isNotBlank() } ?: "Sua conta está temporariamente suspensa. Entre em contato com a equipe TechMentor.",
            cor = Color(0xFFFFECEE), icone = Icone.ESCUDO, iconeCor = Color(0xFFCF4753)
        )
    }
}

@Composable
private fun AvisoParceira(titulo: String, texto: String, cor: Color, icone: Icone, iconeCor: Color) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cor).padding(13.dp), verticalAlignment = Alignment.Top) {
        IconeLinha(icone, iconeCor, tamanho = 22.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(titulo, color = Tinta, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(texto, color = Cinza, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun BotaoCriarCurso(permitido: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = permitido,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Roxo, disabledContainerColor = Color(0xFFB8A9E3)),
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) {
        Text("＋  Criar novo curso", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CursosParceira(
    perfil: ParceiraPerfilResponse,
    cursos: List<ParceiraCursoResponse>,
    aoAbrirCurso: (ParceiraCursoResponse) -> Unit,
    aoCriarCurso: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Meus cursos", color = Tinta, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${cursos.size} curso${if (cursos.size == 1) "" else "s"} no total", color = Cinza, fontSize = 13.sp)
            }
            IconeLinha(Icone.LIVRO, Roxo, tamanho = 27.dp)
        }
        AvisoSituacaoConta(perfil)
        if (cursos.isEmpty()) EmptyStateParceira("Nenhum curso cadastrado", "Quando criar seu primeiro curso, ele aparecerá nesta lista.")
        else cursos.forEach { CursoCompacto(it, aoAbrirCurso) }
        if (cursos.any { it.comentarioRevisao?.isNotBlank() == true }) {
            Text("Toque em um curso devolvido para ler o comentário da equipe.", color = Cinza, fontSize = 12.sp)
        }
        BotaoCriarCurso(permitido = perfil.situacao == "ATIVA", onClick = aoCriarCurso)
    }
}

@Composable
private fun EstatisticasParceira(
    cursos: List<ParceiraCursoResponse>,
    metricas: Map<Long, com.example.techmentor.data.MetricasParceiraCursoResponse>
) {
    val publicados = cursos.filter { it.situacao == "PUBLICADO" }.mapNotNull { metricas[it.id] }
    val matriculas = publicados.sumOf { it.matriculas }
    val concluintes = publicados.sumOf { it.concluintes }
    val taxa = if (matriculas == 0L) 0 else ((concluintes * 100) / matriculas).toInt().coerceIn(0, 100)
    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text("Desempenho dos cursos", color = Tinta, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Os dados consideram apenas cursos publicados.", color = Cinza, fontSize = 13.sp)
        EstatisticasCompactas(cursos, metricas) { }
        if (publicados.isEmpty()) {
            EmptyStateParceira("Ainda sem estatísticas", "As métricas aparecem quando um curso publicado receber matrículas.")
        } else {
            publicados.forEach { metrica ->
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(metrica.titulo, color = Tinta, fontWeight = FontWeight.Bold)
                        Text("${metrica.matriculas} matrículas · ${metrica.concluintes} conclusões · ${metrica.taxaConclusao}% de conclusão", color = Cinza, fontSize = 12.sp)
                    }
                }
            }
            Text("Total: $matriculas matrículas, $concluintes conclusões e $taxa% de conclusão.", color = Cinza, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ConfiguracoesParceira(perfil: ParceiraPerfilResponse, aoSair: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Configurações", color = Tinta, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LinhaConfiguracao("Instituição", perfil.nome)
                LinhaConfiguracao("Responsável", perfil.responsavel)
                LinhaConfiguracao("E-mail", perfil.email)
                LinhaConfiguracao("Site", perfil.site ?: "Não informado")
                LinhaConfiguracao("Tipo", perfil.tipo.lowercase().replaceFirstChar { it.uppercase() })
                LinhaConfiguracao("Situação", perfil.situacao.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
        Text("Para alterar os dados da instituição, fale com a equipe TechMentor.", color = Cinza, fontSize = 12.sp)
        OutlinedButton(onClick = aoSair, modifier = Modifier.fillMaxWidth()) { Text("Sair da conta", color = Color(0xFFB42336)) }
    }
}

@Composable
private fun LinhaConfiguracao(rotulo: String, valor: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(rotulo, color = Cinza, fontSize = 11.sp)
        Text(valor, color = Tinta, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun EmptyStateParceira(titulo: String, descricao: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 23.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IconeLinha(Icone.LIVRO, Roxo, tamanho = 31.dp)
            Spacer(Modifier.height(8.dp))
            Text(titulo, color = Tinta, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(descricao, color = Cinza, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun BarraInferiorParceira(abaAtual: AbaParceira, aoSelecionar: (AbaParceira) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Borda))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(68.dp).padding(horizontal = 5.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            AbaParceira.entries.forEach { aba ->
                val selecionada = aba == abaAtual
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Tab) { aoSelecionar(aba) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    IconeLinha(aba.icone, if (selecionada) Roxo else Color(0xFF9995A4), tamanho = 23.dp, espessura = if (selecionada) 2.2f else 1.8f)
                    Text(aba.rotulo, color = if (selecionada) Roxo else Tinta, fontSize = 9.sp, fontWeight = if (selecionada) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun DialogoNovoCurso(
    enviando: Boolean,
    erro: String?,
    aoCriar: (ParceiraCursoRequest) -> Unit,
    aoFechar: () -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("PROGRAMACAO") }
    var nivel by remember { mutableStateOf("INICIANTE") }
    var menuCategoria by remember { mutableStateOf(false) }
    var menuNivel by remember { mutableStateOf(false) }
    val categorias = listOf("PROGRAMACAO", "DADOS", "DESIGN", "CARREIRA", "SEGURANCA", "IA", "REDES", "OUTROS")
    val niveis = listOf("INICIANTE", "INTERMEDIARIO", "AVANCADO")

    AlertDialog(
        onDismissRequest = { if (!enviando) aoFechar() },
        title = { Text("Criar novo curso", color = Tinta, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(titulo, { titulo = it.take(150) }, label = { Text("Nome do curso") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(descricao, { descricao = it.take(1000) }, label = { Text("Descrição") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedButton(onClick = { menuCategoria = true }, modifier = Modifier.fillMaxWidth()) { Text("Categoria: ${categoriaLegivel(categoria)}") }
                    DropdownMenu(expanded = menuCategoria, onDismissRequest = { menuCategoria = false }) {
                        categorias.forEach { item -> DropdownMenuItem(text = { Text(categoriaLegivel(item)) }, onClick = { categoria = item; menuCategoria = false }) }
                    }
                }
                Box {
                    OutlinedButton(onClick = { menuNivel = true }, modifier = Modifier.fillMaxWidth()) { Text("Nível: ${categoriaLegivel(nivel)}") }
                    DropdownMenu(expanded = menuNivel, onDismissRequest = { menuNivel = false }) {
                        niveis.forEach { item -> DropdownMenuItem(text = { Text(categoriaLegivel(item)) }, onClick = { nivel = item; menuNivel = false }) }
                    }
                }
                if (erro != null) Text(erro, color = Color(0xFFB42336), fontSize = 12.sp)
            Text("O curso será salvo como rascunho. Você poderá adicionar módulos e aulas antes de solicitar revisão.", color = Cinza, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(enabled = !enviando && titulo.isNotBlank(), onClick = {
                aoCriar(ParceiraCursoRequest(titulo.trim(), descricao.trim(), categoria, nivel))
            }) { Text(if (enviando) "Salvando…" else "Criar curso") }
        },
        dismissButton = { TextButton(enabled = !enviando, onClick = aoFechar) { Text("Cancelar") } }
    )
}

@Composable
private fun DetalheCursoDialog(
    curso: ParceiraCursoResponse,
    editavel: Boolean,
    enviando: Boolean,
    erro: String?,
    aoAdicionarModulo: () -> Unit,
    aoAdicionarAula: (Long) -> Unit,
    aoEnviarRevisao: () -> Unit,
    aoFechar: () -> Unit
) {
    val completo = !curso.descricao.isNullOrBlank() && curso.modulos.isNotEmpty() && curso.modulos.all { it.aulas.isNotEmpty() }
    AlertDialog(
        onDismissRequest = { if (!enviando) aoFechar() },
        title = { Text(curso.titulo, color = Tinta, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                StatusCurso(curso.situacao)
                Text(curso.descricao.orEmpty().ifBlank { "Sem descrição cadastrada." }, color = Cinza, fontSize = 13.sp)
                if (curso.modulos.isEmpty()) {
                    Text("Ainda não há módulos neste curso.", color = Cinza, fontSize = 13.sp)
                } else {
                    curso.modulos.forEach { modulo ->
                        Surface(shape = RoundedCornerShape(13.dp), color = Color(0xFFF7F5FC), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(modulo.titulo, color = Tinta, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    if (editavel) TextButton(onClick = { aoAdicionarAula(modulo.id) }) { Text("＋ Aula", fontSize = 11.sp) }
                                }
                                if (modulo.aulas.isEmpty()) Text("Nenhuma aula adicionada", color = Cinza, fontSize = 11.sp)
                                modulo.aulas.forEach { aula ->
                                    Text("• ${aula.titulo} · ${aula.duracaoMinutos} min", color = Cinza, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                curso.comentarioRevisao?.takeIf { it.isNotBlank() }?.let {
                    Surface(color = Color(0xFFFFF7E8), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Comentário da equipe", color = Color(0xFF9A5B00), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(it, color = Tinta, fontSize = 12.sp)
                        }
                    }
                }
                if (curso.situacao == "EM_REVISAO") {
                    Text("O curso está com a equipe para revisão. Você poderá editar novamente quando receber uma resposta.", color = Cinza, fontSize = 12.sp)
                }
                if (erro != null) Text(erro, color = Color(0xFFB42336), fontSize = 12.sp)
            }
        },
        confirmButton = {
            if (editavel) TextButton(enabled = !enviando && completo, onClick = aoEnviarRevisao) {
                Text(if (enviando) "Enviando…" else "Enviar para revisão")
            } else TextButton(onClick = aoFechar) { Text("Fechar") }
        },
        dismissButton = {
            if (editavel) Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !enviando, onClick = aoAdicionarModulo) { Text("＋ Módulo") }
                TextButton(enabled = !enviando, onClick = aoFechar) { Text("Fechar") }
            }
        }
    )
}

@Composable
private fun DialogoNovoModulo(
    enviando: Boolean,
    erro: String?,
    aoSalvar: (String) -> Unit,
    aoFechar: () -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!enviando) aoFechar() },
        title = { Text("Adicionar módulo", color = Tinta, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(titulo, { titulo = it.take(150) }, label = { Text("Nome do módulo") }, singleLine = true)
                if (erro != null) Text(erro, color = Color(0xFFB42336), fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(enabled = !enviando && titulo.isNotBlank(), onClick = { aoSalvar(titulo.trim()) }) { Text(if (enviando) "Salvando…" else "Adicionar") } },
        dismissButton = { TextButton(enabled = !enviando, onClick = aoFechar) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogoNovaAula(
    enviando: Boolean,
    erro: String?,
    aoSalvar: (String, Int, String) -> Unit,
    aoFechar: () -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var duracao by remember { mutableStateOf("15") }
    var url by remember { mutableStateOf("") }
    val minutos = duracao.toIntOrNull()
    AlertDialog(
        onDismissRequest = { if (!enviando) aoFechar() },
        title = { Text("Adicionar aula", color = Tinta, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(titulo, { titulo = it.take(200) }, label = { Text("Nome da aula") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(duracao, { duracao = it.filter(Char::isDigit).take(3) }, label = { Text("Duração em minutos") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(url, { url = it.take(255) }, label = { Text("Link do YouTube ou vídeo MP4") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Links aceitos: YouTube ou arquivo .mp4 protegido por https.", color = Cinza, fontSize = 11.sp)
                if (erro != null) Text(erro, color = Color(0xFFB42336), fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(enabled = !enviando && titulo.isNotBlank() && (minutos ?: 0) > 0 && url.isNotBlank(), onClick = { aoSalvar(titulo.trim(), minutos ?: 15, url.trim()) }) {
                Text(if (enviando) "Salvando…" else "Adicionar aula")
            }
        },
        dismissButton = { TextButton(enabled = !enviando, onClick = aoFechar) { Text("Cancelar") } }
    )
}

private fun categoriaLegivel(valor: String): String = when (valor) {
    "PROGRAMACAO" -> "Programação"
    "DADOS" -> "Dados"
    "DESIGN" -> "Design"
    "CARREIRA" -> "Carreira"
    "SEGURANCA" -> "Segurança"
    "IA" -> "Inteligência artificial"
    "REDES" -> "Redes"
    "OUTROS" -> "Outros"
    "INICIANTE" -> "Iniciante"
    "INTERMEDIARIO" -> "Intermediário"
    "AVANCADO" -> "Avançado"
    else -> valor
}

@Composable
private fun CarregandoParceira() {
    Box(Modifier.fillMaxSize().background(Fundo), contentAlignment = Alignment.Center) {
        Text("Carregando sua área de parceira…", color = Cinza, fontSize = 14.sp)
    }
}

@Composable
private fun FalhaParceira(mensagem: String, aoTentarNovamente: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Não foi possível abrir sua área", color = Tinta, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(mensagem, color = Cinza, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        Button(onClick = aoTentarNovamente, colors = ButtonDefaults.buttonColors(containerColor = Roxo)) { Text("Tentar novamente") }
    }
}
