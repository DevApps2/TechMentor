package com.example.techmentor.data

import android.content.Context
import android.content.SharedPreferences

/** Guarda no aparelho quem está logado, de que tipo é e o token de acesso, para o app abrir direto na tela certa. */
object Sessao {

    private const val ARQUIVO = "sessao"
    private const val CHAVE_USUARIO_ID = "usuario_id"
    private const val CHAVE_TIPO = "tipo"
    private const val CHAVE_TOKEN = "access_token"

    private var prefs: SharedPreferences? = null

    /** Chamar uma vez, na MainActivity, antes de montar as telas. */
    fun iniciar(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
    }

    val usuarioId: Long?
        get() = prefs?.getLong(CHAVE_USUARIO_ID, -1L)?.takeIf { it > 0 }

    /** O token que o servidor entregou no login. Nulo = ninguém logado (ou sessão antiga, sem token). */
    val token: String?
        get() = prefs?.getString(CHAVE_TOKEN, null)

    /** ALUNA, MENTORA ou ADMIN. Sessões antigas, sem tipo guardado, contam como ALUNA. */
    val tipo: String
        get() = prefs?.getString(CHAVE_TIPO, null) ?: "ALUNA"

    val ehMentora: Boolean get() = tipo == "MENTORA"

    /** A primeira tela de cada tipo de usuário. */
    fun rotaInicial(): String = when (tipo) {
        "ADMIN" -> "admin"
        "MENTORA" -> "mentora"
        else -> "home"
    }

    fun entrar(usuarioId: Long, tipo: String, token: String) {
        prefs?.edit()
            ?.putLong(CHAVE_USUARIO_ID, usuarioId)
            ?.putString(CHAVE_TIPO, tipo)
            ?.putString(CHAVE_TOKEN, token)
            ?.apply()
    }

    fun sair() {
        prefs?.edit()?.clear()?.apply()
    }
}