package com.example.techmentor.ui.admin

import com.example.techmentor.data.AdminDenuncia
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.ui.home.parseDataHora
import com.example.techmentor.ui.mentoria.mensagemDaApi
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LOCALE_BR: Locale = Locale.forLanguageTag("pt-BR")


// ======================================================
// RESULTADO DE UMA CHAMADA À API
// ======================================================

/** Deu certo (com o valor) ou falhou (com a mensagem que já pode ir para a tela). */
sealed interface Resultado<out T> {

    data class Ok<T>(val valor: T) : Resultado<T>

    data class Falha(val mensagem: String, val codigo: Int? = null) : Resultado<Nothing> {

        /** "Usuário não encontrado." = a sessão guardada no aparelho não existe mais no banco. */
        val sessaoExpirada: Boolean
            get() = codigo == 404 && mensagem.startsWith("Usuário")
    }
}

fun <T> Resultado<T>.valorOuNull(): T? = when (this) {
    is Resultado.Ok -> valor
    is Resultado.Falha -> null
}

/**
 * Faz a chamada e transforma qualquer falha de API ou de rede em [Resultado.Falha].
 * Erros 4xx mostram a mensagem da própria API (ex.: "E-mail já cadastrado."); o resto vira "falha de conexão".
 * O cancelamento da coroutine passa direto (não é capturado).
 */
suspend fun <T> chamar(bloco: suspend () -> T): Resultado<T> =
    try {
        Resultado.Ok(bloco())
    } catch (e: HttpException) {
        val daApi = if (e.code() in 400..499) mensagemDaApi(e) else null
        Resultado.Falha(daApi ?: ERRO_CONEXAO, e.code())
    } catch (e: IOException) {
        Resultado.Falha(ERRO_CONEXAO)
    }

/** Para as chamadas que respondem 204 (sem corpo), como excluir. */
suspend fun chamarSemCorpo(bloco: suspend () -> Response<Unit>): Resultado<Unit> =
    chamar {
        val resposta = bloco()
        if (!resposta.isSuccessful) throw HttpException(resposta)
    }


// ======================================================
// TEXTOS EM PORTUGUÊS (a API manda códigos como ASSEDIO)
// ======================================================

fun nomeDoMotivo(motivo: String): String = when (motivo) {
    "ASSEDIO" -> "Assédio"
    "DISCURSO_DE_ODIO" -> "Discurso de ódio"
    "SPAM" -> "Spam ou fraude"
    "PERFIL_FALSO" -> "Perfil falso"
    "COMPORTAMENTO_INADEQUADO" -> "Comportamento inadequado"
    "OUTRO" -> "Outro"
    else -> motivo
}

fun nomeDoStatus(status: String): String = when (status) {
    "ABERTA" -> "Aberta"
    "EM_ANALISE" -> "Em análise"
    "RESOLVIDA" -> "Resolvida"
    else -> status
}

fun nomeDoTipoDeOportunidade(tipo: String): String = when (tipo) {
    "ESTAGIO" -> "Estágio"
    "EMPREGO" -> "Emprego"
    "BOLSA" -> "Bolsa"
    "EVENTO" -> "Evento"
    else -> tipo
}

val STATUS_DE_DENUNCIA = listOf("ABERTA", "EM_ANALISE", "RESOLVIDA")

val TIPOS_DE_OPORTUNIDADE = listOf("ESTAGIO", "EMPREGO", "BOLSA", "EVENTO")


// ======================================================
// DENÚNCIAS
// ======================================================

private fun ordemDoStatus(status: String): Int = STATUS_DE_DENUNCIA.indexOf(status).let { if (it < 0) 99 else it }

/** O que precisa de atenção primeiro: abertas, depois em análise, depois resolvidas; dentro de cada uma, as mais novas. */
fun ordenarDenuncias(lista: List<AdminDenuncia>): List<AdminDenuncia> =
    lista.sortedWith(
        compareBy<AdminDenuncia> { ordemDoStatus(it.status) }
            .thenByDescending { it.criadaEm.orEmpty() }
    )


// ======================================================
// DATAS
// ======================================================

/** "2026-10-26T14:30:00" -> "26/10/2026 às 14:30" */
fun formatarDataHora(iso: String?): String {
    val data = parseDataHora(iso) ?: return ""
    return SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", LOCALE_BR).format(data)
}

/** "2026-12-01" -> "01/12/2026" */
fun formatarData(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val data = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso.take(10)) }.getOrNull()
        ?: return iso
    return SimpleDateFormat("dd/MM/yyyy", LOCALE_BR).format(data)
}

/** O prazo já passou? (comparar texto "aaaa-mm-dd" funciona porque a ordem do texto é a ordem das datas) */
fun prazoEncerrado(iso: String?): Boolean {
    if (iso.isNullOrBlank()) return false
    val hoje = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    return iso.take(10) < hoje
}

/** "01/12/2026" -> "2026-12-01"; devolve null se a data não existir (ex.: 31/02/2026). */
fun dataParaIso(texto: String): String? {
    val entrada = SimpleDateFormat("dd/MM/yyyy", LOCALE_BR).apply { isLenient = false }
    val data = runCatching { entrada.parse(texto.trim()) }.getOrNull() ?: return null
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(data)
}

/** Vai pondo as barras enquanto a pessoa digita: "01122026" -> "01/12/2026". */
fun mascaraDeData(digitado: String): String {
    val digitos = digitado.filter { it.isDigit() }.take(8)
    return buildString {
        digitos.forEachIndexed { posicao, digito ->
            if (posicao == 2 || posicao == 4) append('/')
            append(digito)
        }
    }
}

/** Texto vazio vira null: o backend guarda "sem valor" em vez de texto em branco. */
fun String.vazioParaNulo(): String? = trim().ifEmpty { null }
