package com.example.techmentor.ui.cursos

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.AulaProgressoResponse
import com.example.techmentor.ui.components.BotaoPrimario
import com.example.techmentor.ui.components.SeloDeStatus
import com.example.techmentor.ui.mentoria.CabecalhoVoltar
import com.example.techmentor.ui.theme.ManropeFamily

private val Fundo = Color(0xFFF8F7FB)
private val Roxo = Color(0xFF7C3AED)
private val RoxoEscuro = Color(0xFF6D28D9)
private val TextoEscuro = Color(0xFF17151F)
private val TextoMudo = Color(0xFF625D6C)

// Quanto do vídeo precisa ter sido assistido para a aula poder ser concluída
private const val MINIMO_ASSISTIDO = 0.9f

@Composable
fun AulaScreen(
    cursoId: Long,
    aulaId: Long,
    navController: NavController,
    vm: AulaViewModel = viewModel()
) {

    val estado by vm.estado.collectAsState()

    LaunchedEffect(cursoId, aulaId) { vm.carregar(cursoId, aulaId) }

    // rememberSaveable: girar o celular não tira a aluna da tela cheia
    var telaCheia by rememberSaveable { mutableStateOf(false) }

    // O que o player vai informando: quanto foi assistido e o tamanho do vídeo
    var assistidoMs by remember(aulaId) { mutableLongStateOf(0L) }
    var duracaoMs by remember(aulaId) { mutableLongStateOf(0L) }
    // true quando o vídeo não abriu: a aluna não fica presa, pode marcar a aula como concluída
    var videoIndisponivel by remember(aulaId) { mutableStateOf(false) }

    EfeitoDeTelaCheia(ativa = telaCheia)
    BackHandler(enabled = telaCheia) { telaCheia = false }

    val aula = estado.aula

    when {
        estado.carregando -> Box(
            modifier = Modifier.fillMaxSize().background(Fundo),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = Roxo) }

        estado.erro != null || aula == null -> TelaDeErro(
            mensagem = estado.erro ?: "Não foi possível abrir esta aula.",
            onTentarDeNovo = { vm.carregar(cursoId, aulaId) },
            onVoltar = { navController.popBackStack() }
        )

        telaCheia && !aula.conteudoUrl.isNullOrBlank() && idDoYoutube(aula.conteudoUrl) == null -> PlayerDeVideo(
            url = aula.conteudoUrl,
            aulaId = aula.aulaId,
            telaCheia = true,
            aoAlternarTelaCheia = { telaCheia = false },
            aoProgresso = { assistido, duracao ->
                assistidoMs = assistido
                duracaoMs = duracao
            },
            modifier = Modifier.fillMaxSize()
        )

        else -> {
            val semUrl = aula.conteudoUrl.isNullOrBlank()
            val idYoutube = aula.conteudoUrl?.let { idDoYoutube(it) }
            // "sem vídeo" vale também quando ele existe mas não abriu: nos dois casos não dá para cobrar que assista
            val semVideo = semUrl || videoIndisponivel
            // Se o player não informou a duração, usa a do catálogo para a barra e o botão não ficarem parados
            val baseMs = if (duracaoMs > 0) duracaoMs else aula.duracaoMinutos * 60_000L
            val fracao = if (baseMs > 0) (assistidoMs.toFloat() / baseMs).coerceIn(0f, 1f) else 0f
            val assistiuOSuficiente = semVideo || fracao >= MINIMO_ASSISTIDO

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Fundo)
                    .statusBarsPadding()
            ) {

                CabecalhoVoltar(
                    titulo = estado.cursoTitulo,
                    onVoltar = { navController.popBackStack() }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {

                    if (semUrl) {
                        SemVideo()
                    } else if (idYoutube != null) {
                        PlayerYoutube(
                            videoId = idYoutube,
                            aulaId = aula.aulaId,
                            aoProgresso = { assistido, duracao ->
                                assistidoMs = assistido
                                duracaoMs = duracao
                                videoIndisponivel = false
                            },
                            aoFalhar = { videoIndisponivel = true },
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        )
                    } else {
                        PlayerDeVideo(
                            url = aula.conteudoUrl!!,
                            aulaId = aula.aulaId,
                            telaCheia = false,
                            aoAlternarTelaCheia = { telaCheia = true },
                            aoProgresso = { assistido, duracao ->
                                assistidoMs = assistido
                                duracaoMs = duracao
                                videoIndisponivel = false
                            },
                            aoFalhar = { videoIndisponivel = true },
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        )
                    }

                    InformacoesDaAula(
                        aula = aula,
                        semVideo = semVideo,
                        fracao = fracao,
                        assistiuOSuficiente = assistiuOSuficiente
                    )
                }

                BarraDeAcao(
                    aula = aula,
                    proxima = estado.proxima,
                    liberada = assistiuOSuficiente,
                    semVideo = semVideo,
                    salvando = estado.salvando,
                    erro = estado.erroAoConcluir,
                    onConcluir = { vm.concluir() },
                    onProxima = { proxima -> irParaAula(navController, cursoId, proxima.aulaId) },
                    onVoltarParaTrilha = { navController.popBackStack() }
                )
            }
        }
    }

    if (estado.concluidaAgora && aula != null) {
        val proxima = estado.proxima

        AlertDialog(
            onDismissRequest = { vm.fecharParabens() },
            containerColor = Color.White,
            title = {
                Text(
                    text = if (estado.cursoConcluido) "Curso concluído!" else "Aula concluída!",
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextoEscuro
                )
            },
            text = {
                Column {
                    Text(
                        text = "+${aula.xpRecompensa} XP",
                        fontFamily = ManropeFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Roxo
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (estado.cursoConcluido) {
                            "Você terminou ${estado.cursoTitulo}. Parabéns pela conquista!"
                        } else {
                            "Seu progresso foi salvo."
                        },
                        fontSize = 13.sp,
                        color = TextoMudo
                    )
                }
            },
            confirmButton = {
                if (proxima != null && !estado.cursoConcluido) {
                    TextButton(onClick = {
                        vm.fecharParabens()
                        irParaAula(navController, cursoId, proxima.aulaId)
                    }) { Text("Próxima aula", color = Roxo, fontWeight = FontWeight.Bold) }
                } else {
                    TextButton(onClick = {
                        vm.fecharParabens()
                        navController.popBackStack()
                    }) { Text("Ver trilha", color = Roxo, fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = {
                if (proxima != null && !estado.cursoConcluido) {
                    TextButton(onClick = {
                        vm.fecharParabens()
                        navController.popBackStack()
                    }) { Text("Voltar à trilha", color = TextoMudo) }
                }
            }
        )
    }
}

/** Abre outra aula no lugar desta: o "voltar" continua levando à trilha. */
private fun irParaAula(navController: NavController, cursoId: Long, aulaId: Long) {
    navController.navigate("aula/$cursoId/$aulaId") {
        popUpTo("aula/{cursoId}/{aulaId}") { inclusive = true }
    }
}


// ======================================================
// PEÇAS DA TELA
// ======================================================

@Composable
private fun InformacoesDaAula(
    aula: AulaProgressoResponse,
    semVideo: Boolean,
    fracao: Float,
    assistiuOSuficiente: Boolean
) {

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {

        Text(
            text = "MÓDULO ${aula.ordemModulo} · AULA ${aula.ordemAula}",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.9.sp,
            color = Roxo
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = aula.titulo,
            fontFamily = ManropeFamily,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextoEscuro
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(text = aula.tituloModulo, fontSize = 13.sp, color = TextoMudo)

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SeloDeStatus("${aula.duracaoMinutos} min", Color(0xFFEDE9FE), RoxoEscuro)
            SeloDeStatus("+${aula.xpRecompensa} XP", Color(0xFFFEF3C7), Color(0xFFB45309))
            if (aula.status == "CONCLUIDA") {
                SeloDeStatus("Concluída", Color(0xFFDCFCE7), Color(0xFF15803D))
            }
        }

        // Só faz sentido cobrar o vídeo de quem ainda vai concluir a aula
        if (aula.status == "ATUAL") {

            Spacer(modifier = Modifier.height(18.dp))

            BlocoDeProgresso(
                semVideo = semVideo,
                fracao = fracao,
                assistiuOSuficiente = assistiuOSuficiente
            )
        }
    }
}

@Composable
private fun BlocoDeProgresso(
    semVideo: Boolean,
    fracao: Float,
    assistiuOSuficiente: Boolean
) {

    val forma = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, Color(0xFFECE9F1), forma)
            .padding(14.dp)
    ) {

        if (semVideo) {
            Text(
                text = "Não há vídeo disponível agora. Quando terminar de estudar, é só marcar como concluída.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = TextoMudo
            )
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Seu progresso no vídeo",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextoEscuro,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(fracao * 100).toInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (assistiuOSuficiente) Color(0xFF15803D) else Roxo
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFFE9E6EF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fracao)
                    .height(7.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(listOf(Roxo, Color(0xFF2563EB))))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (assistiuOSuficiente) {
                "✓ Pronto! Você já pode concluir a aula."
            } else {
                "Assista a pelo menos ${(MINIMO_ASSISTIDO * 100).toInt()}% do vídeo para concluir."
            },
            fontSize = 11.sp,
            color = if (assistiuOSuficiente) Color(0xFF15803D) else TextoMudo
        )
    }
}

/** A barra fixa de baixo: concluir (aula atual) ou seguir em frente (aula já concluída). */
@Composable
private fun BarraDeAcao(
    aula: AulaProgressoResponse,
    proxima: AulaProgressoResponse?,
    liberada: Boolean,
    semVideo: Boolean,
    salvando: Boolean,
    erro: String?,
    onConcluir: () -> Unit,
    onProxima: (AulaProgressoResponse) -> Unit,
    onVoltarParaTrilha: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {

        if (erro != null) {
            Text(text = erro, fontSize = 11.sp, color = Color(0xFFDC2626))
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (aula.status == "ATUAL") {
            BotaoPrimario(
                texto = when {
                    salvando -> "Salvando..."
                    semVideo -> "Marcar como concluída · +${aula.xpRecompensa} XP"
                    liberada -> "Concluir aula · +${aula.xpRecompensa} XP"
                    else -> "Assista ao vídeo para concluir"
                },
                habilitado = liberada && !salvando,
                modifier = Modifier.fillMaxWidth(),
                onClick = onConcluir
            )
        } else {
            val podeSeguir = proxima != null && proxima.status != "BLOQUEADA"
            BotaoPrimario(
                texto = if (podeSeguir) "Próxima aula" else "Voltar à trilha",
                modifier = Modifier.fillMaxWidth(),
                onClick = { if (podeSeguir) onProxima(proxima!!) else onVoltarParaTrilha() }
            )
        }
    }
}

@Composable
private fun SemVideo() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Brush.linearGradient(listOf(Color(0xFF4C1D95), Color(0xFF1D4ED8)))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Esta aula ainda não tem vídeo",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TelaDeErro(
    mensagem: String,
    onTentarDeNovo: () -> Unit,
    onVoltar: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Fundo).statusBarsPadding()
    ) {
        CabecalhoVoltar(titulo = "Aula", onVoltar = onVoltar)

        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = mensagem, fontSize = 14.sp, color = TextoMudo, textAlign = TextAlign.Center)
            TextButton(onClick = onTentarDeNovo) { Text("Tentar novamente", color = Roxo) }
        }
    }
}


// ======================================================
// TELA CHEIA
// ======================================================

/** Na tela cheia: gira o celular para o modo paisagem e esconde as barras do sistema. Ao sair, desfaz. */
@Composable
internal fun EfeitoDeTelaCheia(ativa: Boolean) {

    val contexto = LocalContext.current

    DisposableEffect(ativa) {
        val atividade = contexto.encontrarAtividade()
        val janela = atividade?.window
        val controle = janela?.let { WindowCompat.getInsetsController(it, it.decorView) }

        if (ativa) {
            atividade?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controle?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controle?.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            atividade?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controle?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private tailrec fun Context.encontrarAtividade(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.encontrarAtividade()
    else -> null
}
