package com.example.techmentor.ui.cursos

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color as CorAndroid
import android.net.Uri
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.techmentor.data.ProgressoDeVideo
import com.example.techmentor.ui.components.BotaoPrimario
import kotlinx.coroutines.delay

// Lê o <video> da página de incorporação do YouTube: posição, duração e estado (0 terminou, 1 tocando, 2 pausado)
private const val LEITURA_DO_VIDEO =
    "(function(){var v=document.querySelector('video');if(!v){return 'x';}" +
        "return Math.round(v.currentTime*1000)+'|'+Math.round((v.duration||0)*1000)+'|'+(v.ended?0:(v.paused?2:1));})()"

private const val PAUSAR_O_VIDEO =
    "(function(){var v=document.querySelector('video');if(v){v.pause();}})()"

// Faz o navegador dizer ao YouTube que NÃO decodifica AV1/VP9. Ele passa a mandar H.264, que toda GPU decodifica.
// Em alguns celulares o WebView aceita AV1/VP9 mas pinta o vídeo de preto (só o som toca).
private const val FILTRO_DE_CODEC = """
(function () {
  if (window.__h264) { return; }
  window.__h264 = true;
  var ruim = /av01|vp9|vp09|vp8/i;
  try {
    if (window.MediaSource && MediaSource.isTypeSupported) {
      var original = MediaSource.isTypeSupported.bind(MediaSource);
      MediaSource.isTypeSupported = function (t) { return ruim.test(t) ? false : original(t); };
    }
    var canPlay = HTMLMediaElement.prototype.canPlayType;
    HTMLMediaElement.prototype.canPlayType = function (t) { return ruim.test(t) ? '' : canPlay.call(this, t); };
    if (navigator.mediaCapabilities && navigator.mediaCapabilities.decodingInfo) {
      var info = navigator.mediaCapabilities.decodingInfo.bind(navigator.mediaCapabilities);
      navigator.mediaCapabilities.decodingInfo = function (c) {
        var tipo = (c && c.video && c.video.contentType) || '';
        if (ruim.test(tipo)) { return Promise.resolve({ supported: false, smooth: false, powerEfficient: false }); }
        return info(c);
      };
    }
  } catch (e) {}
})();
"""

/** "https://www.youtube.com/watch?v=ID" ou "https://youtu.be/ID" -> ID; qualquer outro link -> null. */
internal fun idDoYoutube(url: String): String? {
    val uri = Uri.parse(url)
    val dominio = uri.host?.removePrefix("www.")?.removePrefix("m.") ?: return null
    val id = when (dominio) {
        "youtube.com" -> uri.getQueryParameter("v")
        "youtu.be" -> uri.lastPathSegment
        else -> null
    }
    return id?.takeIf { Regex("[A-Za-z0-9_-]{11}").matches(it) }
}

/**
 * Player alternativo do YouTube: a página de incorporação do próprio YouTube dentro de um WebView, com o Referer que ele
 * exige e o filtro que o faz servir vídeo em H.264. Serve de reserva quando o player da biblioteca não mostra a imagem.
 * A regra é a mesma do outro player: retoma de onde parou e só soma o tempo que tocou de verdade.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun PlayerYoutubeWebView(
    videoId: String,
    aulaId: Long,
    aoProgresso: (assistidoMs: Long, duracaoMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    aoFalhar: () -> Unit = {}
) {

    val contexto = LocalContext.current
    val abrirLink = LocalUriHandler.current
    val donoDoCiclo = LocalLifecycleOwner.current
    val aoProgressoAtual by rememberUpdatedState(aoProgresso)
    val aoFalharAtual by rememberUpdatedState(aoFalhar)

    var tentativa by remember { mutableIntStateOf(0) }
    var preparado by remember(videoId, tentativa) { mutableStateOf(false) }
    var jaTocou by remember(videoId, tentativa) { mutableStateOf(false) }
    var falhou by remember(videoId, tentativa) { mutableStateOf(false) }
    var codigoDoErro by remember(videoId, tentativa) { mutableStateOf("") }
    var posicao by remember(videoId, tentativa) { mutableLongStateOf(0L) }
    var duracao by remember(videoId, tentativa) { mutableLongStateOf(0L) }
    var ultimoRelogio by remember(videoId, tentativa) { mutableLongStateOf(0L) }
    var ultimoSalvamento by remember(videoId, tentativa) { mutableLongStateOf(System.currentTimeMillis()) }
    var assistido by remember(videoId, aulaId) { mutableLongStateOf(ProgressoDeVideo.assistidoMs(aulaId)) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var viewTelaCheia by remember(videoId, tentativa) { mutableStateOf<View?>(null) }
    var saidaDaTelaCheia by remember(videoId, tentativa) { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val inicioSegundos = remember(videoId, tentativa) { ProgressoDeVideo.posicaoMs(aulaId) / 1000 }

    // O YouTube só aceita o vídeo se souber de qual site ele vem: usamos o id do app
    val origem = "https://${contexto.packageName}"

    fun registrarTique(posicaoMs: Long, duracaoMs: Long, estado: Int) {
        preparado = true
        if (estado == 1) jaTocou = true
        posicao = posicaoMs
        if (duracaoMs > 0) duracao = duracaoMs

        // Soma o tempo real em que o vídeo esteve tocando; pular na barra do YouTube não conta
        val agora = System.currentTimeMillis()
        if (estado == 1 && ultimoRelogio > 0) {
            val passou = (agora - ultimoRelogio).coerceIn(0L, 1500L)
            assistido = if (duracao > 0) (assistido + passou).coerceAtMost(duracao) else assistido + passou
        }
        ultimoRelogio = agora

        aoProgressoAtual(assistido, duracao)

        if (System.currentTimeMillis() - ultimoSalvamento > 5000) {
            ProgressoDeVideo.salvar(aulaId, posicaoMs, assistido)
            ultimoSalvamento = System.currentTimeMillis()
        }
    }

    fun registrarFim() {
        assistido = duracao
        aoProgressoAtual(assistido, duracao)
        ProgressoDeVideo.salvar(aulaId, 0L, assistido)
    }

    // A cada meio segundo, pergunta à página o que o <video> está fazendo
    LaunchedEffect(webView, tentativa) {
        val pagina = webView ?: return@LaunchedEffect
        while (true) {
            delay(500)
            pagina.evaluateJavascript(LEITURA_DO_VIDEO) { bruto ->
                val partes = bruto?.trim('"')?.split("|")
                if (partes != null && partes.size == 3) {
                    val p = partes[0].toLongOrNull()
                    val d = partes[1].toLongOrNull()
                    val e = partes[2].toIntOrNull()
                    if (p != null && d != null && e != null) {
                        registrarTique(p, d, e)
                        if (e == 0 && d > 0) registrarFim()
                    }
                }
            }
        }
    }

    // Nunca fica girando ou preto sem dizer nada
    LaunchedEffect(videoId, tentativa) {
        delay(20_000)
        if (!preparado && !falhou) {
            codigoDoErro = "demorou"
            falhou = true
            aoFalharAtual()
        }
    }
    LaunchedEffect(videoId, tentativa, preparado) {
        if (!preparado) return@LaunchedEffect
        delay(15_000)
        if (!jaTocou && !falhou) {
            codigoDoErro = "nao_tocou"
            falhou = true
            aoFalharAtual()
        }
    }

    // Ao sair da tela: guarda onde parou
    DisposableEffect(videoId, tentativa) {
        onDispose {
            val acabou = duracao > 0 && posicao >= duracao - 1000
            if (preparado) ProgressoDeVideo.salvar(aulaId, if (acabou) 0L else posicao, assistido)
        }
    }

    // Ao ir para o segundo plano, pausa o vídeo
    DisposableEffect(donoDoCiclo) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_PAUSE) webView?.evaluateJavascript(PAUSAR_O_VIDEO, null)
        }
        donoDoCiclo.lifecycle.addObserver(observador)
        onDispose { donoDoCiclo.lifecycle.removeObserver(observador) }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        key(videoId, tentativa) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        // O botão de tela cheia da página de incorporação entrega a vista do vídeo aqui
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                saidaDaTelaCheia = callback
                                viewTelaCheia = view
                            }

                            override fun onHideCustomView() {
                                viewTelaCheia = null
                                saidaDaTelaCheia = null
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                view?.evaluateJavascript(FILTRO_DE_CODEC, null)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                view?.evaluateJavascript(FILTRO_DE_CODEC, null)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true && !preparado) {
                                    codigoDoErro = "pagina"
                                    falhou = true
                                    aoFalharAtual()
                                }
                            }
                        }
                        setBackgroundColor(CorAndroid.BLACK)
                        loadUrl(
                            "https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&rel=0&modestbranding=1&start=$inicioSegundos",
                            mapOf("Referer" to origem)
                        )
                        webView = this
                    }
                },
                onRelease = {
                    it.stopLoading()
                    it.destroy()
                    if (webView === it) webView = null
                }
            )
        }

        if (!preparado && !falhou) {
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
        TelaCheiaDeVideo(
            conteudo = vista,
            aoSair = {
                saidaDaTelaCheia?.onCustomViewHidden()
                viewTelaCheia = null
                saidaDaTelaCheia = null
            }
        )
    }
}
