package com.example.techmentor.ui.cursos

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.techmentor.data.ProgressoDeVideo
import com.example.techmentor.ui.components.BotaoPrimario
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay

/**
 * O player do YouTube que a tela usa. Por padrão é o da biblioteca android-youtube-player; se ele não mostrar a imagem
 * em algum celular, o link embaixo troca para o player alternativo (WebView com filtro de formato) e o app lembra a escolha.
 */
@Composable
fun PlayerYoutube(
    videoId: String,
    aulaId: Long,
    aoProgresso: (assistidoMs: Long, duracaoMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    aoFalhar: () -> Unit = {}
) {

    var usarBiblioteca by remember { mutableStateOf(ProgressoDeVideo.playerDaBiblioteca()) }

    Column(modifier = Modifier.fillMaxWidth()) {

        if (usarBiblioteca) {
            PlayerYoutubeBiblioteca(videoId, aulaId, aoProgresso, modifier, aoFalhar)
        } else {
            PlayerYoutubeWebView(videoId, aulaId, aoProgresso, modifier, aoFalhar)
        }

        Text(
            text = if (usarBiblioteca) "Sem imagem? Usar o player alternativo" else "Voltar ao player padrão",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6D28D9),
            modifier = Modifier
                .clickable {
                    usarBiblioteca = !usarBiblioteca
                    ProgressoDeVideo.definirPlayerDaBiblioteca(usarBiblioteca)
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PlayerYoutubeBiblioteca(
    videoId: String,
    aulaId: Long,
    aoProgresso: (assistidoMs: Long, duracaoMs: Long) -> Unit,
    modifier: Modifier,
    aoFalhar: () -> Unit
) {

    val contexto = LocalContext.current
    val donoDoCiclo = LocalLifecycleOwner.current
    val abrirLink = LocalUriHandler.current
    val aoProgressoAtual by rememberUpdatedState(aoProgresso)
    val aoFalharAtual by rememberUpdatedState(aoFalhar)

    var tentativa by remember { mutableIntStateOf(0) }
    var pronto by remember(videoId, tentativa) { mutableStateOf(false) }
    var jaTocou by remember(videoId, tentativa) { mutableStateOf(false) }
    var tocando by remember(videoId, tentativa) { mutableStateOf(false) }
    // a vista que o player entrega quando a aluna toca no botão de tela cheia, e como sair dela
    var viewTelaCheia by remember(videoId, tentativa) { mutableStateOf<View?>(null) }
    var sairDaTelaCheia by remember(videoId, tentativa) { mutableStateOf<(() -> Unit)?>(null) }
    var falhou by remember(videoId, tentativa) { mutableStateOf(false) }
    var codigoDoErro by remember(videoId, tentativa) { mutableStateOf("") }
    var posicao by remember(videoId, tentativa) { mutableLongStateOf(0L) }
    var duracao by remember(videoId, tentativa) { mutableLongStateOf(0L) }
    var anterior by remember(videoId, tentativa) { mutableLongStateOf(-1L) }
    var ultimoSalvamento by remember(videoId, tentativa) { mutableLongStateOf(System.currentTimeMillis()) }
    var assistido by remember(videoId, aulaId) { mutableLongStateOf(ProgressoDeVideo.assistidoMs(aulaId)) }

    // Onde a aluna tinha parado, em segundos
    val inicioSegundos = remember(videoId, tentativa) { (ProgressoDeVideo.posicaoMs(aulaId) / 1000).toFloat() }

    val visao = remember(videoId, tentativa) {
        YouTubePlayerView(contexto).apply { enableAutomaticInitialization = false }
    }

    // O ciclo de vida do player (pausa ao ir para o segundo plano, libera o WebView) é da biblioteca
    DisposableEffect(visao) {
        donoDoCiclo.lifecycle.addObserver(visao)
        onDispose {
            val acabou = duracao > 0 && posicao >= duracao - 1000
            if (pronto) ProgressoDeVideo.salvar(aulaId, if (acabou) 0L else posicao, assistido)
            donoDoCiclo.lifecycle.removeObserver(visao)
            visao.release()
        }
    }

    // Tempo assistido: a cada meio segundo com o vídeo tocando soma meio segundo. Não depende da posição que o player
    // informa, e pular na barra não conta (a posição muda, mas o relógio é o mesmo).
    LaunchedEffect(visao) {
        while (true) {
            delay(500)
            if (tocando) {
                assistido = if (duracao > 0) (assistido + 500).coerceAtMost(duracao) else assistido + 500
                aoProgressoAtual(assistido, duracao)
            }
        }
    }

    // Nunca fica girando ou preto sem dizer nada
    LaunchedEffect(visao) {
        delay(20_000)
        if (!pronto && !falhou) {
            codigoDoErro = "demorou"
            falhou = true
            aoFalharAtual()
        }
    }
    LaunchedEffect(visao, pronto) {
        if (!pronto) return@LaunchedEffect
        delay(15_000)
        if (!jaTocou && !falhou) {
            codigoDoErro = "nao_tocou"
            falhou = true
            aoFalharAtual()
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        key(visao) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    visao.apply {
                        // O Builder usa o id do app como origem (é o que o YouTube exige para aceitar o vídeo)
                        val opcoes = IFramePlayerOptions.Builder(contexto)
                            .controls(1)
                            .rel(0)
                            .ivLoadPolicy(3)
                            .fullscreen(1)
                            .build()

                        addFullscreenListener(object : FullscreenListener {
                            override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                                sairDaTelaCheia = exitFullscreen
                                viewTelaCheia = fullscreenView
                                visibility = View.GONE
                            }

                            override fun onExitFullscreen() {
                                viewTelaCheia = null
                                sairDaTelaCheia = null
                                visibility = View.VISIBLE
                            }
                        })

                        initialize(object : AbstractYouTubePlayerListener() {

                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                pronto = true
                                youTubePlayer.loadVideo(videoId, inicioSegundos)
                            }

                            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                                duracao = (duration * 1000).toLong()
                            }

                            override fun onStateChange(
                                youTubePlayer: YouTubePlayer,
                                state: PlayerConstants.PlayerState
                            ) {
                                tocando = state == PlayerConstants.PlayerState.PLAYING
                                if (tocando) jaTocou = true
                                if (state == PlayerConstants.PlayerState.ENDED && duracao > 0) {
                                    assistido = duracao
                                    aoProgressoAtual(assistido, duracao)
                                    ProgressoDeVideo.salvar(aulaId, 0L, assistido)
                                }
                            }

                            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                                val agora = (second * 1000).toLong()
                                posicao = agora

                                if (System.currentTimeMillis() - ultimoSalvamento > 5000) {
                                    ProgressoDeVideo.salvar(aulaId, agora, assistido)
                                    ultimoSalvamento = System.currentTimeMillis()
                                }
                            }

                            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                                codigoDoErro = error.name
                                falhou = true
                                aoFalharAtual()
                            }
                        }, opcoes)
                    }
                }
            )
        }

        if (!pronto && !falhou) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
        }

        if (falhou) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF17151F))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Não foi possível exibir o vídeo aqui",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Verifique a internet ou assista pelo YouTube. ($codigoDoErro)",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                BotaoPrimario(
                    texto = "Tentar de novo",
                    modifier = Modifier.width(190.dp),
                    onClick = { tentativa++ }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BotaoPrimario(
                    texto = "Abrir no YouTube",
                    modifier = Modifier.width(190.dp),
                    onClick = { runCatching { abrirLink.openUri("https://www.youtube.com/watch?v=$videoId") } }
                )
            }
        }
    }

    viewTelaCheia?.let { vista ->
        TelaCheiaDeVideo(conteudo = vista, aoSair = { sairDaTelaCheia?.invoke() })
    }
}
