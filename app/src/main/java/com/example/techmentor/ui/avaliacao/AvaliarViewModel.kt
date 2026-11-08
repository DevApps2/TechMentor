package com.example.techmentor.ui.avaliacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.AvaliacaoDaMentoriaResponse
import com.example.techmentor.data.AvaliacaoRequest
import com.example.techmentor.data.DenunciaRequest
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.mentoria.mensagemDaApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class AvaliarUiState(
    val carregando: Boolean = true,
    // erro ao carregar a tela (não ao enviar)
    val erro: String? = null,
    // quem será avaliada: a mentora (quando quem avalia é a aluna) ou a aluna (quando é a mentora)
    val outraPessoa: String = "",
    val euSouMentora: Boolean = false,
    // o que o backend diz sobre esta mentoria: se pode avaliar, o motivo, o que já foi avaliado
    val situacao: AvaliacaoDaMentoriaResponse? = null,
    val nota: Int = 0,
    val comentario: String = "",
    val enviando: Boolean = false,
    val erroAoEnviar: String? = null,
    val enviada: Boolean = false,
    // true quando o usuário da sessão não existe mais (ex.: o banco foi reiniciado)
    val sessaoExpirada: Boolean = false,
    // denunciar o comentário que a outra pessoa escreveu sobre mim
    val enviandoDenuncia: Boolean = false,
    val denunciaEnviada: Boolean = false,
    val erroDaDenuncia: String? = null,
)

class AvaliarViewModel : ViewModel() {

    private val _estado = MutableStateFlow(AvaliarUiState())
    val estado: StateFlow<AvaliarUiState> = _estado.asStateFlow()

    fun carregar(agendamentoId: Long) {
        // Só carrega uma vez: ao voltar para a tela, não apaga o que a pessoa já escolheu ou digitou
        if (_estado.value.situacao != null) return

        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, erro = null) }

            try {
                val api = ApiClient.api

                coroutineScope {
                    val agendamentoPedido = async { api.obterAgendamento(usuarioId, agendamentoId) }
                    val situacaoPedido = async { api.situacaoDaAvaliacao(usuarioId, agendamentoId) }

                    val agendamento = agendamentoPedido.await()
                    val situacao = situacaoPedido.await()

                    val euSouAluna = agendamento.alunaId == usuarioId

                    _estado.update {
                        it.copy(
                            carregando = false,
                            situacao = situacao,
                            euSouMentora = !euSouAluna,
                            outraPessoa = if (euSouAluna) agendamento.mentoraNome else agendamento.alunaNome
                        )
                    }
                }

            } catch (e: HttpException) {
                val daApi = mensagemDaApi(e)

                // "Usuário não encontrado." = a sessão guardada no aparelho não existe mais no banco
                if (e.code() == 404 && daApi?.startsWith("Usuário") == true) {
                    _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
                } else {
                    val mensagem = when (e.code()) {
                        403 -> "Você não participou desta mentoria."
                        404 -> "Mentoria não encontrada."
                        else -> ERRO_CONEXAO
                    }
                    _estado.update { it.copy(carregando = false, erro = mensagem) }
                }

            } catch (e: IOException) {
                _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
            }
        }
    }

    fun selecionarNota(nota: Int) {
        _estado.update { it.copy(nota = nota, erroAoEnviar = null) }
    }

    fun digitarComentario(texto: String) {
        if (texto.length <= LIMITE_COMENTARIO) {
            _estado.update { it.copy(comentario = texto) }
        }
    }

    /**
     * Denuncia o comentário que a outra pessoa escreveu sobre mim. O servidor descobre quem escreveu
     * pelo id da avaliação, então o app só manda qual avaliação é e o motivo.
     */
    fun denunciarComentario(motivo: String, detalhe: String) {
        val atual = _estado.value
        val avaliacaoId = atual.situacao?.recebida?.id ?: return
        if (atual.enviandoDenuncia || atual.denunciaEnviada) return

        val usuarioId = Sessao.usuarioId ?: return

        // O servidor exige pelo menos 10 letras na descrição; sem detalhe, vai um texto padrão
        val texto = if (detalhe.trim().length >= 10) detalhe.trim()
        else "Comentário de uma avaliação considerado inadequado."

        _estado.update { it.copy(enviandoDenuncia = true, erroDaDenuncia = null) }

        viewModelScope.launch {
            try {
                ApiClient.api.denunciar(usuarioId, DenunciaRequest(null, motivo, texto, avaliacaoId))
                _estado.update { it.copy(enviandoDenuncia = false, denunciaEnviada = true) }

            } catch (e: HttpException) {
                val daApi = mensagemDaApi(e)

                val mensagem = when (e.code()) {
                    400, 409, 429 -> daApi ?: "Não foi possível enviar a denúncia."
                    403 -> "Só quem foi avaliada pode denunciar este comentário."
                    else -> ERRO_CONEXAO
                }
                _estado.update { it.copy(enviandoDenuncia = false, erroDaDenuncia = mensagem) }

            } catch (e: IOException) {
                _estado.update { it.copy(enviandoDenuncia = false, erroDaDenuncia = ERRO_CONEXAO) }
            }
        }
    }

    fun limparErroDaDenuncia() {
        _estado.update { it.copy(erroDaDenuncia = null) }
    }

    fun enviar(agendamentoId: Long) {
        val atual = _estado.value
        if (atual.enviando || atual.enviada) return

        if (atual.nota !in 1..5) {
            _estado.update { it.copy(erroAoEnviar = "Escolha de 1 a 5 estrelas.") }
            return
        }

        val usuarioId = Sessao.usuarioId ?: return

        _estado.update { it.copy(enviando = true, erroAoEnviar = null) }

        viewModelScope.launch {
            try {
                ApiClient.api.avaliar(
                    usuarioId,
                    agendamentoId,
                    AvaliacaoRequest(nota = atual.nota, comentario = atual.comentario.trim().ifEmpty { null })
                )

                _estado.update { it.copy(enviando = false, enviada = true) }

            } catch (e: HttpException) {
                val daApi = mensagemDaApi(e)

                val mensagem = when (e.code()) {
                    400, 409 -> daApi ?: "Não foi possível enviar a avaliação."
                    403 -> "Você não participou desta mentoria."
                    else -> ERRO_CONEXAO
                }

                _estado.update { it.copy(enviando = false, erroAoEnviar = mensagem) }

                // 409 costuma ser "já avaliou" ou "prazo vencido": busca a situação de novo para a tela mostrar o estado certo
                if (e.code() == 409) {
                    atualizarSituacao(usuarioId, agendamentoId)
                }

            } catch (e: IOException) {
                _estado.update { it.copy(enviando = false, erroAoEnviar = ERRO_CONEXAO) }
            }
        }
    }

    private suspend fun atualizarSituacao(usuarioId: Long, agendamentoId: Long) {
        try {
            val situacao = ApiClient.api.situacaoDaAvaliacao(usuarioId, agendamentoId)
            _estado.update { it.copy(situacao = situacao) }
        } catch (e: HttpException) {
            // a mensagem de erro já está na tela; não precisa de outra
        } catch (e: IOException) {
            // idem
        }
    }
}