package com.example.techmentor.ui.cursos

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.techmentor.data.ProgressoDeVideo
import com.example.techmentor.ui.components.BotaoPrimario
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha
import kotlinx.coroutines.delay
import java.util.Locale

private const val SALTO_MS = 10_000L

/**
 * Player de vídeo da aula (VideoView do Android com controles próprios).
 *
 * - Retoma de onde a aluna parou e lembra quanto ela realmente assistiu.
 * - "Assistido" só soma o tempo que o vídeo tocou de verdade: pular para o fim não conta.
 * - [aoProgresso] recebe (assistidoMs, duracaoMs) a cada meio segundo; quem decide o que fazer com isso é a tela.
 */
@Composable
fun PlayerDeVideo(
    url: String,
    aulaId: Long,
    telaCheia: Boolean,
    aoAlternarTelaCheia: () -> Unit,
    aoProgresso: (assistidoMs: Long, duracaoMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    aoFalhar: () -> Unit = {}
) {

    val contexto = LocalContext.current
    val aoProgressoAtual by rememberUpdatedState(aoProgresso)
    val aoFalharAtual by rememberUpdatedState(aoFalhar)

    // "tentativa" muda quando a aluna toca em "Tentar de novo": tudo abaixo recomeça do zero
    var tentativa by remember { mutableIntStateOf(0) }

    var preparado by remember(url, tentativa) { mutableStateOf(false) }
    var falhou by remember(url, tentativa) { mutableStateOf(false) }
    var tocando by remember(url, tentativa) { mutableStateOf(false) }
    var posicao by remember(url, tentativa) { mutableLongStateOf(0L) }
    var duracao by remember(url, tentativa) { mutableLongStateOf(0L) }
    var assistido by remember(url, aulaId) { mutableLongStateOf(ProgressoDeVideo.assistidoMs(aulaId)) }

    var arrastando by remember { mutableStateOf<Float?>(null) }
    var controles by remember { mutableStateOf(true) }

    val videoView = remember(url, tentativa) { VideoView(contexto) }

    // Se em 20 segundos o vídeo não ficou pronto, mostra o erro em vez de girar para sempre
    LaunchedEffect(videoView) {
        delay(20_000)
        if (!preparado && !falhou) {
            falhou = true
            aoFalharAtual()
        }
    }

    // Ao sair da tela (ou girar o celular): guarda onde parou e solta o vídeo
    DisposableEffect(videoView) {
        onDispose {
            val agora = runCatching { videoView.currentPosition.toLong() }.getOrDefault(0L)
            val acabou = duracao > 0 && agora >= duracao - 1000
            ProgressoDeVideo.salvar(aulaId, if (acabou) 0L else agora, assistido)
            runCatching { videoView.stopPlayback() }
        }
    }

    // Ao ir para o segundo plano, pausa
    val donoDoCiclo = LocalLifecycleOwner.current
    DisposableEffect(donoDoCiclo, videoView) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_PAUSE) {
                runCatching { if (videoView.isPlaying) videoView.pause() }
            }
        }
        donoDoCiclo.lifecycle.addObserver(observador)
        onDispose { donoDoCiclo.lifecycle.removeObserver(observador) }
    }

    // A cada meio segundo: lê a posição, soma o tempo assistido e avisa a tela
    LaunchedEffect(videoView, preparado) {
        if (!preparado) return@LaunchedEffect

        var anterior = runCatching { videoView.currentPosition.toLong() }.getOrDefault(0L)
        var ultimoSalvamento = System.currentTimeMillis()

        while (true) {
            delay(500)

            val atual = runCatching { videoView.currentPosition.toLong() }.getOrDefault(anterior)
            val rodando = runCatching { videoView.isPlaying }.getOrDefault(false)

            if (arrastando == null) posicao = atual
            tocando = rodando

            if (rodando) {
                // Só conta avanço natural (até 1,5 s por leitura): um salto na barra não vale como assistido
                val avancou = atual - anterior
                if (avancou in 1..1500) assistido = (assistido + avancou).coerceAtMost(duracao)
            }
            anterior = atual

            aoProgressoAtual(assistido, duracao)

            if (System.currentTimeMillis() - ultimoSalvamento > 5000) {
                ProgressoDeVideo.salvar(aulaId, atual, assistido)
                ultimoSalvamento = System.currentTimeMillis()
            }
        }
    }

    // Com o vídeo tocando, os controles somem sozinhos depois de 3 segundos
    LaunchedEffect(controles, tocando, arrastando) {
        if (controles && tocando && arrastando == null) {
            delay(3000)
            controles = false
        }
    }

    fun alternarReproducao() {
        if (videoView.isPlaying) {
            videoView.pause()
            tocando = false
        } else {
            if (duracao > 0 && posicao >= duracao - 500) {
                videoView.seekTo(0)
                posicao = 0
            }
            videoView.start()
            tocando = true
        }
    }

    fun saltar(deltaMs: Long) {
        if (duracao <= 0) return
        val destino = (videoView.currentPosition + deltaMs).coerceIn(0L, duracao)
        videoView.seekTo(destino.toInt())
        posicao = destino
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        key(url, tentativa) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    videoView.apply {
                        setOnPreparedListener { jogador ->
                            duracao = jogador.duration.toLong()

                            // Volta para onde parou, se não for no começo nem quase no fim
                            val inicio = ProgressoDeVideo.posicaoMs(aulaId)
                            if (inicio in 3000 until (duracao - 2000)) {
                                seekTo(inicio.toInt())
                                posicao = inicio
                            }

                            preparado = true
                            start()
                            tocando = true
                        }
                        setOnCompletionListener {
                            tocando = false
                            posicao = duracao
                            assistido = duracao
                            controles = true
                            aoProgressoAtual(assistido, duracao)
                        }
                        setOnErrorListener { _, _, _ ->
                            falhou = true
                            aoFalharAtual()
                            true
                        }
                        setVideoURI(Uri.parse(url))
                    }
                }
            )
        }

        // Camada transparente por cima do vídeo: um toque mostra ou esconde os controles
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { controles = !controles }
        )

        if (!preparado && !falhou) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
        }

        if (falhou) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF17151F))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Não foi possível carregar o vídeo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Verifique sua conexão com a internet.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                BotaoPrimario(
                    texto = "Tentar de novo",
                    modifier = Modifier.width(170.dp),
                    onClick = { tentativa++ }
                )
            }
        }

        if (preparado && controles) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { controles = false }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BotaoDeSalto(texto = "−10", descricao = "Voltar 10 segundos") { saltar(-SALTO_MS) }

                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.95f))
                        .clickable { alternarReproducao() }
                        .semantics { contentDescription = if (tocando) "Pausar" else "Reproduzir" },
                    contentAlignment = Alignment.Center
                ) {
                    if (tocando) {
                        IconePausa()
                    } else {
                        IconeLinha(icone = Icone.PLAY, cor = Color(0xFF6D28D9), tamanho = 26.dp)
                    }
                }

                BotaoDeSalto(texto = "+10", descricao = "Avançar 10 segundos") { saltar(SALTO_MS) }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatarTempo(posicao)} / ${formatarTempo(duracao)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { aoAlternarTelaCheia() }
                            .semantics {
                                contentDescription = if (telaCheia) "Sair da tela cheia" else "Tela cheia"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        IconeTelaCheia(sair = telaCheia)
                    }
                }

                Slider(
                    value = arrastando ?: posicao.toFloat(),
                    onValueChange = {
                        arrastando = it
                        posicao = it.toLong()
                    },
                    onValueChangeFinished = {
                        arrastando?.let { videoView.seekTo(it.toInt()) }
                        arrastando = null
                    },
                    valueRange = 0f..duracao.coerceAtLeast(1L).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFFA78BFA),
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

@Composable
private fun BotaoDeSalto(
    texto: String,
    descricao: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = descricao },
        contentAlignment = Alignment.Center
    ) {
        Text(text = texto, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}

/** Duas barras verticais (o ícone de pausa). */
@Composable
private fun IconePausa() {
    Canvas(modifier = Modifier.size(22.dp)) {
        val largura = size.width * 0.28f
        val raio = CornerRadius(largura / 2)
        drawRoundRect(
            color = Color(0xFF6D28D9),
            topLeft = Offset(size.width * 0.12f, 0f),
            size = Size(largura, size.height),
            cornerRadius = raio
        )
        drawRoundRect(
            color = Color(0xFF6D28D9),
            topLeft = Offset(size.width * 0.60f, 0f),
            size = Size(largura, size.height),
            cornerRadius = raio
        )
    }
}

/** Quatro cantos: apontam para fora (entrar na tela cheia) ou para dentro (sair). */
@Composable
private fun IconeTelaCheia(sair: Boolean) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val l = size.width
        val canto = l * 0.34f
        val traco = Stroke(width = 2.2f * density, cap = StrokeCap.Round)
        val cor = Color.White

        fun angulo(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(cor, Offset(x, y), Offset(x + dx, y), traco.width, StrokeCap.Round)
            drawLine(cor, Offset(x, y), Offset(x, y + dy), traco.width, StrokeCap.Round)
        }

        if (!sair) {
            angulo(0f, 0f, canto, canto)
            angulo(l, 0f, -canto, canto)
            angulo(0f, l, canto, -canto)
            angulo(l, l, -canto, -canto)
        } else {
            val m = l * 0.34f
            angulo(m, m, -canto, -canto)
            angulo(l - m, m, canto, -canto)
            angulo(m, l - m, -canto, canto)
            angulo(l - m, l - m, canto, canto)
        }
    }
}

/** 75000 -> "1:15" */
internal fun formatarTempo(ms: Long): String {
    val segundos = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", segundos / 60, segundos % 60)
}
