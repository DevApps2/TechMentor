package com.example.techmentor.ui.mentoria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.ErroResponse
import com.example.techmentor.data.HorarioResponse
import com.example.techmentor.data.ReagendamentoRequest
import com.example.techmentor.data.Sessao
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class ReagendarUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    // horários livres da mentora, do mais cedo para o mais tarde (o horário atual da mentoria já está reservado, então não aparece)
    val horarios: List<HorarioResponse> = emptyList(),
    val selecionadoId: Long? = null,
    val enviando: Boolean = false,
    val erroEnvio: String? = null,
    // preenchido quando o reagendamento deu certo: a tela volta para quem abriu
    val concluido: Boolean = false,
    // true quando o usuário da sessão não existe mais (ex.: o banco foi reiniciado)
    val sessaoExpirada: Boolean = false
)

class ReagendarViewModel : ViewModel() {

    private val _estado = MutableStateFlow(ReagendarUiState())
    val estado: StateFlow<ReagendarUiState> = _estado.asStateFlow()

    fun carregar(mentoraId: Long) {
        viewModelScope.launch {
            // O "carregando" só aparece na primeira vez
            _estado.update { if (it.horarios.isEmpty()) it.copy(carregando = true, erro = null) else it }

            try {
                val horarios = horariosDisponiveis(ApiClient.api.listarHorarios(mentoraId))

                _estado.update {
                    it.copy(
                        carregando = false,
                        erro = null,
                        horarios = horarios,
                        // se o horário escolhido sumiu da lista (alguém reservou), a escolha é desfeita
                        selecionadoId = it.selecionadoId?.takeIf { id -> horarios.any { h -> h.id == id } }
                    )
                }
            } catch (e: HttpException) {
                _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
            } catch (e: IOException) {
                _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
            }
        }
    }

    fun selecionar(horarioId: Long) {
        _estado.update { it.copy(selecionadoId = horarioId, erroEnvio = null) }
    }

    /** Troca a mentoria pelo horário escolhido. O backend avisa a outra pessoa e define a nova situação. */
    fun confirmar(agendamentoId: Long, mentoraId: Long) {
        val atual = _estado.value
        if (atual.enviando || atual.concluido) return

        val horarioId = atual.selecionadoId
        if (horarioId == null) {
            _estado.update { it.copy(erroEnvio = "Escolha um horário para continuar.") }
            return
        }

        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, erroEnvio = null) }

            try {
                ApiClient.api.reagendarAgendamento(usuarioId, agendamentoId, ReagendamentoRequest(horarioId))

                _estado.update { it.copy(enviando = false, concluido = true) }
            } catch (e: HttpException) {
                val daApi = runCatching {
                    Gson().fromJson(e.response()?.errorBody()?.string(), ErroResponse::class.java)?.erro
                }.getOrNull()

                val mensagem = when (e.code()) {
                    // 409 = horário reservado por outra pessoa, ou a mentoria já começou, ou já foi cancelada
                    400, 403, 404, 409 -> daApi ?: "Não foi possível reagendar. Tente novamente."
                    else -> ERRO_CONEXAO
                }

                _estado.update { it.copy(enviando = false, erroEnvio = mensagem) }

                // Horário tomado por outra pessoa: atualiza a lista para não insistir nele
                if (e.code() == 409) carregar(mentoraId)
            } catch (e: IOException) {
                _estado.update { it.copy(enviando = false, erroEnvio = ERRO_CONEXAO) }
            }
        }
    }
}