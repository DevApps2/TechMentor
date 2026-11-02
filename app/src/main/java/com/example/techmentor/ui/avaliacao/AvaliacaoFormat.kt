package com.example.techmentor.ui.avaliacao

import com.example.techmentor.ui.home.parseDataHora
import java.text.SimpleDateFormat
import java.util.Locale

private val LocaleBr: Locale = Locale.forLanguageTag("pt-BR")

/** Tamanho máximo do comentário; o mesmo limite do backend. */
const val LIMITE_COMENTARIO = 1000

/** 4 vira "★★★★☆". */
fun estrelasEmTexto(nota: Int): String {
    val cheias = nota.coerceIn(0, 5)
    return "★".repeat(cheias) + "☆".repeat(5 - cheias)
}

fun rotuloDaNota(nota: Int): String = when (nota) {
    1 -> "Foi ruim"
    2 -> "Poderia ser melhor"
    3 -> "Foi boa"
    4 -> "Foi muito boa"
    5 -> "Foi excelente!"
    else -> "Toque nas estrelas para avaliar"
}

/** "2026-11-17T15:00:00" vira "17/11". */
fun diaEMes(texto: String?): String {
    val data = parseDataHora(texto) ?: return ""
    return SimpleDateFormat("dd/MM", LocaleBr).format(data)
}

/** "2026-11-17T15:00:00" vira "17/11/2026". */
fun diaMesEAno(texto: String?): String {
    val data = parseDataHora(texto) ?: return ""
    return SimpleDateFormat("dd/MM/yyyy", LocaleBr).format(data)
}

/** 4.5 vira "4,5". */
fun formatarNotaMedia(nota: Double): String = String.format(LocaleBr, "%.1f", nota)