package com.example.techmentor.ui.mentora

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.AgendamentoResponse
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.ErroResponse
import com.example.techmentor.data.HorarioResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.home.parseDataHora
import com.example.techmentor.ui.mentoria.horariosDisponiveis
import com.google.gson.Gson
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Uma coisa que aparece na agenda: uma mentoria marcada ou um horário livre. */
sealed interface ItemDaAgenda {
    val inicio: Date
    val fim: Date?
}

data class ItemMentoria(
    val agendamento: AgendamentoResponse,
    override val inicio: Date,
    override val fim: Date?
) : ItemDaAgenda

data class ItemHorarioLivre(
    val horario: HorarioResponse,
    override val inicio: Date,
    override val fim: Date?
) : ItemDaAgenda

data class AgendaCalendarioUiState(
    val carregando: Boolean = true,
    val atualizando: Boolean = false,
    val erro: String? = null,
    val mentoraId: Long? = null,
    // tudo o que existe na agenda, agrupado por dia ("2026-11-12") e ordenado por hora
    val itensPorDia: Map<String, List<ItemDaAgenda>> = emptyMap(),
    // true quando a mentora ainda não preencheu o perfil
    val semPerfil: Boolean = false,
    // mentoria ou horário que está sendo confirmado, cancelado ou removido agora
    val acaoEmAndamentoId: Long? = null,
    val erroAcao: String? = null,
    val sessaoExpirada: Boolean = false
)

/** "2026-11-12": a chave de cada dia, no relógio do celular. */
internal fun chaveDoDia(data: Date): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(data)

class AgendaCalendarioViewModel : ViewModel() {

    private val _estado = MutableStateFlow(AgendaCalendarioUiState())
    val estado: StateFlow<AgendaCalendarioUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    fun carregar(manual: Boolean = false) {
        // Evita duas cargas ao mesmo tempo
        if (tarefa?.isActive == true) return

        if (Sessao.usuarioId == null) {
            _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
            return
        }

        tarefa = viewModelScope.launch {
            // O "carregando" só aparece na primeira vez; depois atualiza por baixo dos panos
            _estado.update {
                when {
                    it.mentoraId == null -> it.copy(carregando = true, erro = null)
                    manual -> it.copy(atualizando = true)
                    else -> it
                }
            }

            try {
                buscar()
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    _estado.update { it.copy(sessaoExpirada = true) }
                } else {
                    _estado.update { it.copy(erro = ERRO_CONEXAO) }
                }
            } catch (e: IOException) {
                _estado.update { it.copy(erro = ERRO_CONEXAO) }
            } finally {
                _estado.update { it.copy(carregando = false, atualizando = false) }
            }
        }
    }

    /** Busca mentorias e horários livres ao mesmo tempo e monta o mapa por dia. */
    private suspend fun buscar() {
        val api = ApiClient.api

        // 404 = a mentora ainda não preencheu o perfil
        val mentora = try {
            api.minhaMentora()
        } catch (e: HttpException) {
            if (e.code() == 404) null else throw e
        }

        if (mentora == null) {
            _estado.update { it.copy(semPerfil = true) }
            return
        }

        val (agendamentos, horarios) = coroutineScope {
            val pedidoA = async { api.listarAgendamentos() }
            val pedidoH = async { api.listarHorarios(mentora.id) }
            pedidoA.await() to pedidoH.await()
        }

        val itens = mutableListOf<ItemDaAgenda>()

        // Mentorias canceladas ou expiradas saem da agenda; o resto (inclusive as já realizadas) fica
        agendamentos
            .filter { it.status != "CANCELADA" && it.status != "EXPIRADA" }
            .forEach { a ->
                parseDataHora(a.inicio)?.let { inicio -> itens += ItemMentoria(a, inicio, parseDataHora(a.fim)) }
            }

        horariosDisponiveis(horarios).forEach { h ->
            parseDataHora(h.inicio)?.let { inicio -> itens += ItemHorarioLivre(h, inicio, parseDataHora(h.fim)) }
        }

        val porDia = itens.sortedBy { it.inicio.time }.groupBy { chaveDoDia(it.inicio) }

        _estado.update {
            it.copy(mentoraId = mentora.id, itensPorDia = porDia, semPerfil = false, erro = null)
        }
    }

    fun confirmar(agendamentoId: Long) = executar(agendamentoId) { ApiClient.api.confirmarAgendamento(agendamentoId) }

    fun cancelar(agendamentoId: Long) = executar(agendamentoId) { ApiClient.api.cancelarAgendamento(agendamentoId) }

    fun removerHorario(horarioId: Long) = executar(horarioId) { ApiClient.api.excluirHorario(horarioId) }

    fun limparErroAcao() {
        _estado.update { it.copy(erroAcao = null) }
    }

    /** Faz a chamada, recarrega a agenda e traduz os erros do servidor em mensagens. */
    private fun executar(id: Long, chamada: suspend () -> Unit) {
        if (_estado.value.acaoEmAndamentoId != null) return

        viewModelScope.launch {
            _estado.update { it.copy(acaoEmAndamentoId = id, erroAcao = null) }

            try {
                chamada()
                buscar()
                _estado.update { it.copy(acaoEmAndamentoId = null) }
            } catch (e: HttpException) {
                val daApi = runCatching {
                    Gson().fromJson(e.response()?.errorBody()?.string(), ErroResponse::class.java)?.erro
                }.getOrNull()

                val mensagem = when (e.code()) {
                    // 409 = já confirmada, já cancelada ou já passou do horário
                    400, 403, 404, 409 -> daApi ?: "Não foi possível concluir. Tente novamente."
                    else -> ERRO_CONEXAO
                }

                _estado.update { it.copy(acaoEmAndamentoId = null, erroAcao = mensagem) }
            } catch (e: IOException) {
                _estado.update { it.copy(acaoEmAndamentoId = null, erroAcao = ERRO_CONEXAO) }
            }
        }
    }
}