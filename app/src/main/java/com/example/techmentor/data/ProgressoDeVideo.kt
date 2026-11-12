package com.example.techmentor.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Lembra no aparelho, por conta e por aula, onde a aluna parou no vídeo e quanto dele ela realmente assistiu.
 * Fica em arquivo próprio: sair da conta apaga a sessão, mas quem volta continua de onde parou.
 */
object ProgressoDeVideo {

    private var prefs: SharedPreferences? = null

    fun iniciar(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("video", Context.MODE_PRIVATE)
    }

    private fun chave(campo: String, aulaId: Long): String? {
        val usuarioId = Sessao.usuarioId ?: return null
        return "${campo}_${usuarioId}_$aulaId"
    }

    /** Onde a aluna parou (milissegundos). 0 = começa do início. */
    fun posicaoMs(aulaId: Long): Long =
        chave("pos", aulaId)?.let { prefs?.getLong(it, 0L) } ?: 0L

    /** Quanto do vídeo ela de fato assistiu (milissegundos), sem contar o que foi pulado. */
    fun assistidoMs(aulaId: Long): Long =
        chave("vis", aulaId)?.let { prefs?.getLong(it, 0L) } ?: 0L

    /** true = player da biblioteca android-youtube-player (padrão); false = player alternativo (WebView com filtro de formato). */
    fun playerDaBiblioteca(): Boolean = prefs?.getBoolean("youtube_biblioteca", true) ?: true

    fun definirPlayerDaBiblioteca(valor: Boolean) {
        prefs?.edit()?.putBoolean("youtube_biblioteca", valor)?.apply()
    }

    fun salvar(aulaId: Long, posicaoMs: Long, assistidoMs: Long) {
        val chavePosicao = chave("pos", aulaId) ?: return
        val chaveAssistido = chave("vis", aulaId) ?: return
        prefs?.edit()
            ?.putLong(chavePosicao, posicaoMs)
            ?.putLong(chaveAssistido, assistidoMs)
            ?.apply()
    }
}
