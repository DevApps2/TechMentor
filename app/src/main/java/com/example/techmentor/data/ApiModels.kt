package com.example.techmentor.data

data class LoginRequest(val email: String, val senha: String)

data class UsuarioResponse(
    val id: Long,
    val nome: String,
    val email: String,
    val fotoUrl: String?,
    val tipo: String,
    val xpTotal: Int,
    val nivel: Int,
    val sequenciaDias: Int
)

data class CadastroRequest(val nome: String, val email: String, val senha: String)

data class ErroResponse(val erro: String?)