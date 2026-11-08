package com.example.techmentor.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.AdminCandidatura
import com.example.techmentor.data.AdminCandidaturaStatusRequest
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.Sessao
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminCandidaturasUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val sessaoExpirada: Boolean = false,
    // null = ainda não carregou
    val candidaturas: List<AdminCandidatura>? = null,
    // id da candidatura que está mudando de situação agora
    val alterandoId: Long? = null,
    val erroAoAlterar: String? = null
)

/** Quem se candidatou a uma oportunidade e a situação de cada candidatura (a admin pode mudar). */
class AdminCandidaturasViewModel : ViewModel() {

    private val _estado = MutableStateFlow(AdminCandidaturasUiState())
    val estado: StateFlow<AdminCandidaturasUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    fun carregar(oportunidadeId: Long) {
        // Evita duas cargas ao mesmo tempo
        if (tarefa?.isActive == true) return

        val adminId = Sessao.usuarioId
        if (adminId == null) {
            _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
            return
        }

        tarefa = viewModelScope.launch {
            // O "carregando" só aparece na primeira vez
            _estado.update { if (it.candidaturas == null) it.copy(carregando = true, erro = null) else it }

            when (val resultado = chamar { ApiClient.adminApi.listarCandidaturas(adminId, oportunidadeId) }) {
                is Resultado.Ok -> _estado.update {
                    it.copy(carregando = false, erro = null, candidaturas = resultado.valor)
                }

                is Resultado.Falha -> _estado.update {
                    it.copy(
                        carregando = false,
                        erro = if (resultado.codigo == 404) "Oportunidade não encontrada." else resultado.mensagem,
                        sessaoExpirada = resultado.sessaoExpirada
                    )
                }
            }
        }
    }

    /** Muda a situação (EM_ANALISE, APROVADA, RECUSADA ou ENVIADA). A candidata recebe uma notificação. */
    fun alterarStatus(candidaturaId: Long, novoStatus: String) {
        if (_estado.value.alterandoId != null) return

        val adminId = Sessao.usuarioId
        if (adminId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(alterandoId = candidaturaId, erroAoAlterar = null) }

            val resultado = chamar {
                ApiClient.adminApi.alterarStatusDaCandidatura(
                    adminId, candidaturaId, AdminCandidaturaStatusRequest(novoStatus)
                )
            }

            when (resultado) {
                is Resultado.Ok -> _estado.update { atual ->
                    atual.copy(
                        alterandoId = null,
                        candidaturas = atual.candidaturas?.map {
                            if (it.id == candidaturaId) resultado.valor else it
                        }
                    )
                }

                is Resultado.Falha -> _estado.update {
                    it.copy(
                        alterandoId = null,
                        erroAoAlterar = resultado.mensagem,
                        sessaoExpirada = resultado.sessaoExpirada
                    )
                }
            }
        }
    }
}
