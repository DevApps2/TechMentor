package com.example.techmentor.data

import android.content.Context
import android.content.SharedPreferences

/** Guarda no aparelho quem está logado, para o app abrir direto na Home e mandar o X-Usuario-Id. */
object Sessao {

    private const val ARQUIVO = "sessao"
    private const val CHAVE_USUARIO_ID = "usuario_id"

    private var prefs: SharedPreferences? = null

    /** Chamar uma vez, na MainActivity, antes de montar as telas. */
    fun iniciar(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
    }

    val usuarioId: Long?
        get() = prefs?.getLong(CHAVE_USUARIO_ID, -1L)?.takeIf { it > 0 }

    fun entrar(usuarioId: Long) {
        prefs?.edit()?.putLong(CHAVE_USUARIO_ID, usuarioId)?.apply()
    }

    fun sair() {
        prefs?.edit()?.clear()?.apply()
    }
}