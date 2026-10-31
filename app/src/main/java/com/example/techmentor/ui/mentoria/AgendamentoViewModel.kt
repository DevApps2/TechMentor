package com.example.techmentor.ui.mentoria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.AgendamentoRequest
import com.example.techmentor.data.AgendamentoResponse
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.HorarioResponse
import com.example.techmentor.data.MentoraResponse
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


data class AgendamentoUiState(
    val carregando: Boolean = true,
    // falha ao carregar a tela (mentora e horários)
    val erro: String? = null,
    val mentora: MentoraResponse? = null,
    // só os horários que ainda dá para agendar
    val horarios: List<HorarioResponse> = emptyList(),
    val horarioSelecionadoId: Long? = null,
    val agendando: Boolean = false,
    // falha ao confirmar o agendamento (aparece em vermelho acima do botão)
    val erroAgendamento: String? = null,
    // preenchido quando o agendamento deu certo: a tela troca o formulário pela confirmação
    val agendamentoCriado: AgendamentoResponse? = null,
    // true quando o usuário da sessão não existe mais (ex.: o banco foi reiniciado)
    val sessaoExpirada: Boolean = false
)


class AgendamentoViewModel : ViewModel() {

    private val _estado = MutableStateFlow(AgendamentoUiState())
    val estado: StateFlow<AgendamentoUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null
    private var mentoraIdAtual = 0L

    fun carregar(mentoraId: Long) {
        mentoraIdAtual = mentoraId

        // Evita duas cargas ao mesmo tempo e não mexe na tela depois que o agendamento deu certo
        if (tarefa?.isActive == true) return
        if (_estado.value.agendamentoCriado != null) return

        tarefa = viewModelScope.launch {
            // O "carregando" só aparece na primeira vez; depois atualiza por baixo dos panos
            _estado.update {
                if (it.mentora == null) it.copy(carregando = true, erro = null) else it
            }

            try {
                val api = ApiClient.api

                coroutineScope {
                    val mentoraPedido = async { api.obterMentora(mentoraId) }
                    val horariosPedido = async { api.listarHorarios(mentoraId) }

                    val mentora = mentoraPedido.await()
                    val horarios = horariosDisponiveis(horariosPedido.await())

                    _estado.update {
                        it.copy(
                            carregando = false,
                            erro = null,
                            mentora = mentora,
                            horarios = horarios,
                            // se o horário escolhido sumiu da lista (alguém reservou), a escolha é desfeita
                            horarioSelecionadoId = it.horarioSelecionadoId
                                ?.takeIf { id -> horarios.any { h -> h.id == id } }
                        )
                    }
                }
            } catch (e: HttpException) {
                val mensagem = if (e.code() == 404) "Mentora não encontrada." else ERRO_CONEXAO
                _estado.update { it.copy(carregando = false, erro = mensagem) }
            } catch (e: IOException) {
                _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
            }
        }
    }

    fun selecionarHorario(horarioId: Long) {
        _estado.update { it.copy(horarioSelecionadoId = horarioId, erroAgendamento = null) }
    }

    /** Confirma o agendamento do horário escolhido. O assunto é opcional. */
    fun agendar(assunto: String) {
        val atual = _estado.value
        if (atual.agendando || atual.agendamentoCriado != null) return

        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        val horarioId = atual.horarioSelecionadoId
        if (horarioId == null) {
            _estado.update { it.copy(erroAgendamento = "Escolha um horário para continuar.") }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(agendando = true, erroAgendamento = null) }

            try {
                val criado = ApiClient.api.criarAgendamento(
                    usuarioId,
                    AgendamentoRequest(
                        horarioId = horarioId,
                        assunto = assunto.trim().ifEmpty { null }
                    )
                )

                _estado.update { it.copy(agendando = false, agendamentoCriado = criado) }
            } catch (e: HttpException) {
                val daApi = mensagemDaApi(e)

                // "Usuário não encontrado." = a sessão guardada no aparelho não existe mais no banco
                val sessaoExpirada = e.code() == 404 && daApi?.startsWith("Usuário") == true

                val mensagem = when (e.code()) {
                    400 -> daApi ?: "Confira os dados informados."
                    403 -> "Apenas alunas podem agendar mentorias."
                    404, 409 -> daApi ?: "Esse horário não está mais disponível."
                    else -> ERRO_CONEXAO
                }

                _estado.update {
                    it.copy(
                        agendando = false,
                        erroAgendamento = mensagem,
                        sessaoExpirada = sessaoExpirada
                    )
                }

                // Horário reservado por outra pessoa ou removido: atualiza a lista para não insistir nele
                if (e.code() == 404 || e.code() == 409) {
                    carregar(mentoraIdAtual)
                }
            } catch (e: IOException) {
                _estado.update { it.copy(agendando = false, erroAgendamento = ERRO_CONEXAO) }
            }
        }
    }
}
