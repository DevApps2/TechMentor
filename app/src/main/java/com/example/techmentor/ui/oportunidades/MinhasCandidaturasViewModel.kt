package com.example.techmentor.ui.oportunidades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.InscricaoResponse
import com.example.techmentor.data.Sessao
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/** Uma candidatura (vaga ou bolsa) ou inscrição (evento) da usuária. */
data class ItemDeParticipacao(
    val inscricao: InscricaoResponse,
    val ehEvento: Boolean
)

data class MinhasCandidaturasUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    // null = ainda não carregou
    val itens: List<ItemDeParticipacao>? = null,
    // id da candidatura que está sendo retirada agora
    val retirandoId: Long? = null,
    val erroAoRetirar: String? = null,
    val sessaoExpirada: Boolean = false
)

class MinhasCandidaturasViewModel : ViewModel() {

    private val _estado = MutableStateFlow(MinhasCandidaturasUiState())
    val estado: StateFlow<MinhasCandidaturasUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    fun carregar() {
        // Evita duas cargas ao mesmo tempo
        if (tarefa?.isActive == true) return

        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
            return
        }

        tarefa = viewModelScope.launch {
            // O "carregando" só aparece na primeira vez
            _estado.update { if (it.itens == null) it.copy(carregando = true, erro = null) else it }

            try {
                val api = ApiClient.api

                coroutineScope {
                    val candidaturas = async { api.minhasCandidaturas() }
                    val inscricoes = async { api.minhasInscricoes() }

                    // Mais novas primeiro (a data chega como texto ISO, que ordena certo)
                    val itens = (candidaturas.await().map { ItemDeParticipacao(it, false) } +
                            inscricoes.await().map { ItemDeParticipacao(it, true) })
                        .sortedByDescending { it.inscricao.criadaEm ?: "" }

                    _estado.update { it.copy(carregando = false, erro = null, itens = itens) }
                }

            } catch (e: HttpException) {
                semDadosNaTela(ERRO_CONEXAO)
            } catch (e: IOException) {
                semDadosNaTela(ERRO_CONEXAO)
            }
        }
    }

    /** A própria usuária desiste, enquanto a equipe ainda não respondeu. */
    fun retirar(inscricaoId: Long) {
        if (_estado.value.retirandoId != null) return

        val usuarioId = Sessao.usuarioId ?: return

        _estado.update { it.copy(retirandoId = inscricaoId, erroAoRetirar = null) }

        viewModelScope.launch {
            try {
                ApiClient.api.desistirDaCandidatura(inscricaoId)

                _estado.update { atual ->
                    atual.copy(
                        retirandoId = null,
                        itens = atual.itens?.filterNot { it.inscricao.id == inscricaoId }
                    )
                }

            } catch (e: HttpException) {
                val respondida = e.code() == 409
                _estado.update {
                    it.copy(
                        retirandoId = null,
                        erroAoRetirar = if (respondida) {
                            "Esta candidatura já foi respondida e não pode ser retirada."
                        } else {
                            ERRO_CONEXAO
                        }
                    )
                }
                // Se já foi respondida, a situação mudou: atualiza a lista
                if (respondida) carregar()

            } catch (e: IOException) {
                _estado.update { it.copy(retirandoId = null, erroAoRetirar = ERRO_CONEXAO) }
            }
        }
    }

    // Se a atualização falhar, a lista que já estava na tela continua lá
    private fun semDadosNaTela(mensagem: String) {
        _estado.update {
            if (it.itens != null) it.copy(carregando = false) else it.copy(carregando = false, erro = mensagem)
        }
    }
}