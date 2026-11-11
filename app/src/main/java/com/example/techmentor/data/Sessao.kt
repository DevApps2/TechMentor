package com.example.techmentor.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Guarda no aparelho quem está logado, de que tipo é e o token de acesso, para o app abrir direto na tela certa. */
object Sessao {

    private const val ARQUIVO = "sessao"
    private const val CHAVE_USUARIO_ID = "usuario_id"
    private const val CHAVE_TIPO = "tipo"
    private const val CHAVE_TOKEN = "access_token"
    private const val CHAVE_EMAIL = "email"
    private const val CHAVE_TROCAR_SENHA = "precisa_trocar_senha"

    // As boas-vindas ficam em OUTRO arquivo: sair() apaga a sessão inteira, mas não pode esquecer quem já as viu
    private const val ARQUIVO_ONBOARDING = "onboarding"

    /** Tela de troca de senha do primeiro acesso (senha provisória). */
    const val ROTA_TROCA_DE_SENHA = "trocar-senha-obrigatoria"

    /** Tela de boas-vindas, mostrada uma vez por conta, entre o login e a Home da aluna. */
    const val ROTA_ONBOARDING = "onboarding"

    private var prefs: SharedPreferences? = null
    private var prefsOnboarding: SharedPreferences? = null

    /** Chamar uma vez, na MainActivity, antes de montar as telas. */
    fun iniciar(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
        prefsOnboarding = context.applicationContext.getSharedPreferences(ARQUIVO_ONBOARDING, Context.MODE_PRIVATE)
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

    /** O e-mail com que entrou; serve para o login reabrir já preenchido depois da troca de senha. */
    val email: String?
        get() = prefs?.getString(CHAVE_EMAIL, null)

    /**
     * true = entrou com a senha provisória e ainda não escolheu a sua. Fica guardado: se fechar o app antes de trocar,
     * ao reabrir ela continua sendo levada para a troca.
     */
    val precisaTrocarSenha: Boolean
        get() = prefs?.getBoolean(CHAVE_TROCAR_SENHA, false) ?: false

    /** true = esta conta já viu (ou pulou) as boas-vindas. Sem ninguém logado, não há o que mostrar. */
    val onboardingVisto: Boolean
        get() {
            val id = usuarioId ?: return true
            return prefsOnboarding?.getBoolean("visto_$id", false) ?: true
        }

    fun marcarOnboardingVisto() {
        val id = usuarioId ?: return
        prefsOnboarding?.edit()?.putBoolean("visto_$id", true)?.apply()
    }

    /**
     * A primeira tela de cada tipo de usuário (com senha provisória, antes de tudo vem a troca de senha).
     * A aluna que ainda não viu as boas-vindas passa por elas antes da Home.
     */
    fun rotaInicial(): String = when {
        precisaTrocarSenha -> ROTA_TROCA_DE_SENHA
        tipo == "ADMIN" -> "admin"
        tipo == "MENTORA" -> "mentora"
        !onboardingVisto -> ROTA_ONBOARDING
        else -> "home"
    }

    fun entrar(
        usuarioId: Long,
        tipo: String,
        token: String,
        email: String? = null,
        precisaTrocarSenha: Boolean = false
    ) {
        prefs?.edit()
            ?.putLong(CHAVE_USUARIO_ID, usuarioId)
            ?.putString(CHAVE_TIPO, tipo)
            ?.putString(CHAVE_TOKEN, token)
            ?.putString(CHAVE_EMAIL, email)
            ?.putBoolean(CHAVE_TROCAR_SENHA, precisaTrocarSenha)
            ?.apply()
    }

    fun sair() {
        prefs?.edit()?.clear()?.apply()
    }

    private val _sessaoExpirada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emite quando o servidor recusou o token (vencido ou inválido): o app deve voltar ao login. */
    val sessaoExpirada: SharedFlow<Unit> = _sessaoExpirada

    /**
     * Chamado quando uma chamada COM token volta 401. Várias telas podem carregar ao mesmo tempo e receber 401
     * juntas; por isso só o primeiro aviso vale (depois dele já não há token guardado).
     */
    fun expirar(tokenUsado: String) {
        if (token != tokenUsado) return
        sair()
        _sessaoExpirada.tryEmit(Unit)
    }
}