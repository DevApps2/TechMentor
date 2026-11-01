package com.example.techmentor.data

import android.content.Context
import android.content.SharedPreferences

/** Guarda no aparelho quem está logado e de que tipo é, para o app abrir direto na tela certa. */
object Sessao {

    private const val ARQUIVO = "sessao"
    private const val CHAVE_USUARIO_ID = "usuario_id"
    private const val CHAVE_TIPO = "tipo"

    private var prefs: SharedPreferences? = null

    /** Chamar uma vez, na MainActivity, antes de montar as telas. */
    fun iniciar(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
    }

    val usuarioId: Long?
        get() = prefs?.getLong(CHAVE_USUARIO_ID, -1L)?.takeIf { it > 0 }

    /** ALUNA, MENTORA ou ADMIN. Sessões antigas, sem tipo guardado, contam como ALUNA. */
    val tipo: String
        get() = prefs?.getString(CHAVE_TIPO, null) ?: "ALUNA"

    val ehMentora: Boolean get() = tipo == "MENTORA"

    /** A primeira tela de cada tipo de usuário. O painel do admin ainda não existe, então ele usa a Home. */
    fun rotaInicial(): String = when (tipo) {
        "MENTORA" -> "mentora"
        else -> "home"
    }

    fun entrar(usuarioId: Long, tipo: String) {
        prefs?.edit()
            ?.putLong(CHAVE_USUARIO_ID, usuarioId)
            ?.putString(CHAVE_TIPO, tipo)
            ?.apply()
    }

    fun sair() {
        prefs?.edit()?.clear()?.apply()
    }
}