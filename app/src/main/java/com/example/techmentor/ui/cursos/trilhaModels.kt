package com.example.techmentor.ui.cursos

data class TrilhaModulo(
    val id: Int,
    val titulo: String,
    val descricao: String,
    val totalAulas: Int,
    val aulasConcluidas: Int
)

/** "Por Curso em Vídeo", ou nulo quando o curso não tem instituição informada. */
fun creditoDaInstituicao(instituicao: String?): String? =
    instituicao?.trim()?.takeIf { it.isNotEmpty() }?.let { "Por $it" }
