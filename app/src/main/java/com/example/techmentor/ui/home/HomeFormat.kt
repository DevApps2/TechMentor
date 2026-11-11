package com.example.techmentor.ui.home

import com.example.techmentor.data.AgendamentoResponse
import com.example.techmentor.data.UsuarioResponse
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LOCALE_BR: Locale = Locale.forLanguageTag("pt-BR")

/** O fuso no fim do texto: "-03:00", "+05:30" ou "Z". */
private val TERMINA_COM_FUSO = Regex("(Z|[+-]\\d{2}:\\d{2})$")

/**
 * A API manda datas com o fuso: "2026-11-10T14:00:00-03:00". Aqui viram o instante certo,
 * que depois é mostrado no relógio do celular (quem viaja ou mora em outro fuso vê a hora local).
 * Texto sem fuso ("2026-11-10T14:00:00") vale como a hora do relógio do celular.
 */
fun parseDataHora(texto: String?): Date? =
    runCatching {
        val limpo = texto!!.trim().replace(Regex("\\.\\d+"), "") // sem frações de segundo
        if (TERMINA_COM_FUSO.containsMatchIn(limpo)) {
            val ajustado = if (limpo.endsWith("Z")) limpo.dropLast(1) + "+00:00" else limpo
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(ajustado)
        } else {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(limpo.take(19))
        }
    }.getOrNull()

/**
 * Uma mentoria AGENDADA vale até começar (depois disso a mentora não consegue mais confirmar);
 * uma CONFIRMADA vale até terminar, para o botão da sala continuar na tela enquanto ela acontece.
 */
fun mentoriaAindaVale(agendamento: AgendamentoResponse, agora: Date = Date()): Boolean {
    val limite = when (agendamento.status) {
        "CONFIRMADA" -> parseDataHora(agendamento.fim)
        "AGENDADA" -> parseDataHora(agendamento.inicio)
        else -> null
    } ?: return false
    return limite.after(agora)
}

/** A mentoria mais próxima que ainda vale (veja mentoriaAindaVale). */
fun escolherProximaMentoria(lista: List<AgendamentoResponse>): AgendamentoResponse? {
    val agora = Date()
    return lista
        .filter { mentoriaAindaVale(it, agora) }
        .mapNotNull { agendamento ->
            parseDataHora(agendamento.inicio)?.let { data -> agendamento to data }
        }
        .minByOrNull { (_, data) -> data.time }
        ?.first
}

/** Ex.: "Seg, 10 de nov · 14:00" */
fun formatarDiaEHora(inicio: String): String {
    val data = parseDataHora(inicio) ?: return inicio
    val dia = SimpleDateFormat("EEE, d 'de' MMM", LOCALE_BR).format(data)
        .replaceFirstChar { it.uppercase() }
    val hora = SimpleDateFormat("HH:mm", LOCALE_BR).format(data)
    return "$dia · $hora"
}

fun duracaoEmMinutos(inicio: String, fim: String): Long? {
    val comeco = parseDataHora(inicio) ?: return null
    val termino = parseDataHora(fim) ?: return null
    return (termino.time - comeco.time) / 60_000
}

fun rotuloCategoria(categoria: String?): String = when (categoria) {
    "PROGRAMACAO" -> "PROGRAMAÇÃO"
    "SEGURANCA" -> "SEGURANÇA"
    null -> "TRILHA"
    else -> categoria
}

/** 1240 -> "1.240" */
fun formatarNumero(valor: Int): String =
    NumberFormat.getIntegerInstance(LOCALE_BR).format(valor)

fun textoSequencia(dias: Int): String =
    if (dias == 1) "1 dia de sequência" else "$dias dias de sequência"

/** Ex.: "Seg, 10 de nov · 14:00 · 60 min" */
fun descricaoDaMentoria(agendamento: AgendamentoResponse): String {
    val diaEHora = formatarDiaEHora(agendamento.inicio)
    val minutos = duracaoEmMinutos(agendamento.inicio, agendamento.fim)
    return if (minutos != null && minutos > 0) "$diaEHora · $minutos min" else diaEHora
}

/**
 * Quanto da barra de nível está preenchida (0 a 1).
 * O tamanho de cada nível sai da própria resposta da API: o próximo nível começa em nivel * tamanho.
 */
fun fracaoDoNivel(usuario: UsuarioResponse): Float {
    if (usuario.nivel <= 0) return 0f
    val tamanho = usuario.xpProximoNivel / usuario.nivel
    if (tamanho <= 0) return 0f
    val inicioDoNivel = (usuario.nivel - 1) * tamanho
    return ((usuario.xpTotal - inicioDoNivel).toFloat() / tamanho).coerceIn(0f, 1f)
}