package com.example.techmentor.ui.mentoria

import com.example.techmentor.data.ErroResponse
import com.example.techmentor.data.HorarioResponse
import com.example.techmentor.data.MentoraResponse
import com.example.techmentor.ui.home.parseDataHora
import com.google.gson.Gson
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LOCALE_BR: Locale = Locale.forLanguageTag("pt-BR")

/**
 * A API devolve os horários que ainda não foram reservados, mas inclui os que já passaram.
 * Aqui ficam só os que ainda dá para agendar, do mais cedo para o mais tarde.
 */
fun horariosDisponiveis(lista: List<HorarioResponse>): List<HorarioResponse> {
    val agora = Date()
    return lista
        .filter { !it.reservado && parseDataHora(it.inicio)?.after(agora) == true }
        .sortedBy { it.inicio } // as datas da API são ISO, então ordenar o texto já ordena por data
}

/** Ex.: "Qui, 12 de nov" (mesmo formato da Home) */
fun formatarDia(inicio: String): String {
    val data = parseDataHora(inicio) ?: return inicio
    return SimpleDateFormat("EEE, d 'de' MMM", LOCALE_BR).format(data)
        .replaceFirstChar { it.uppercase() }
}

/** Ex.: "10:00 – 11:00" */
fun formatarFaixaDeHorario(inicio: String, fim: String): String {
    val comeco = parseDataHora(inicio) ?: return inicio
    val formato = SimpleDateFormat("HH:mm", LOCALE_BR)
    val termino = parseDataHora(fim)
    return if (termino != null) {
        "${formato.format(comeco)} – ${formato.format(termino)}"
    } else {
        formato.format(comeco)
    }
}

/** 5.0 -> "5,0" */
fun formatarNota(nota: Double): String = String.format(LOCALE_BR, "%.1f", nota)

fun textoAnos(anos: Int): String = if (anos == 1) "1 ano" else "$anos anos"

/** Ex.: "Engenheira de Software Sênior · Acme Tecnologia" (cargo e empresa podem vir vazios) */
fun cargoEEmpresa(mentora: MentoraResponse): String {
    val partes = listOfNotNull(
        mentora.cargo?.trim()?.takeIf { it.isNotEmpty() },
        mentora.empresa?.trim()?.takeIf { it.isNotEmpty() }
    )
    return if (partes.isEmpty()) "Mentora TechMentor" else partes.joinToString(" · ")
}

/** A API responde erros como {"erro": "mensagem"}; devolve a mensagem, se houver. */
fun mensagemDaApi(e: HttpException): String? =
    runCatching {
        Gson().fromJson(e.response()?.errorBody()?.string(), ErroResponse::class.java)?.erro
    }.getOrNull()
