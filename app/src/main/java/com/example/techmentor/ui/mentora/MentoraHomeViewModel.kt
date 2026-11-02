package com.example.techmentor.ui.mentora

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.AgendamentoResponse
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.ErroResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.data.UsuarioResponse
import com.example.techmentor.ui.home.mentoriaAindaVale
import com.example.techmentor.ui.home.parseDataHora
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
import java.util.Date

data class MentoraHomeUiState(
    val carregando: Boolean = true,
    val atualizando: Boolean = false,
    val erro: String? = null,
    val usuario: UsuarioResponse? = null,
    // mentorias que ainda vão acontecer, da mais próxima para a mais distante
    val proximas: List<AgendamentoResponse> = emptyList(),
    val realizadas: Int = 0,
    // true quando a mentora ainda não preencheu o perfil: o app abre a tela "Complete seu perfil"
    val semPerfil: Boolean = false,
    // mentoria que está sendo confirmada ou cancelada agora
    val acaoEmAndamentoId: Long? = null,
    // falha ao confirmar ou cancelar (aparece em vermelho na tela)
    val erroAcao: String? = null,
    // true quando o usuário da sessão não existe mais (ex.: o banco foi reiniciado)
    val sessaoExpirada: Boolean = false
)

class MentoraHomeViewModel : ViewModel() {

    private val _estado = MutableStateFlow(MentoraHomeUiState())
    val estado: StateFlow<MentoraHomeUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    fun carregar(manual: Boolean = false) {
        // Evita duas cargas ao mesmo tempo
        if (tarefa?.isActive == true) return

        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
            return
        }

        tarefa = viewModelScope.launch {
            // O "carregando" só aparece na primeira vez; depois atualiza por baixo dos panos
            _estado.update {
                when {
                    it.usuario == null -> it.copy(carregando = true, erro = null)
                    manual -> it.copy(atualizando = true)
                    else -> it
                }
            }

            try {
                val api = ApiClient.api

                coroutineScope {
                    // As três chamadas saem ao mesmo tempo.
                    // Para uma mentora, a API devolve as mentorias em que ela é a mentora.
                    val usuarioPedido = async { api.obterUsuario(usuarioId) }
                    val agendamentosPedido = async { api.listarAgendamentos(usuarioId) }

                    // 404 = a mentora ainda não preencheu o perfil
                    val semPerfilPedido = async {
                        try {
                            api.minhaMentora(usuarioId)
                            false
                        } catch (e: HttpException) {
                            if (e.code() == 404) true else throw e
                        }
                    }

                    val usuario = usuarioPedido.await()
                    val agendamentos = agendamentosPedido.await()
                    val semPerfil = semPerfilPedido.await()

                    val agora = Date()

                    val proximas = agendamentos
                        .filter { mentoriaAindaVale(it, agora) }
                        .mapNotNull { a -> parseDataHora(a.inicio)?.let { data -> a to data } }
                        .sortedBy { (_, data) -> data.time }
                        .map { (a, _) -> a }

                    _estado.update {
                        it.copy(
                            carregando = false,
                            erro = null,
                            usuario = usuario,
                            proximas = proximas,
                            realizadas = agendamentos.count { a -> a.status == "CONCLUIDA" },
                            semPerfil = semPerfil
                        )
                    }
                }
            } catch (e: HttpException) {
                if (e.code() == 404 || e.code() == 401) {
                    _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
                } else {
                    _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
                }
            } catch (e: IOException) {
                _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
            } finally {
                _estado.update { it.copy(atualizando = false) }
            }
        }
    }

    /** A mentora confirma: a mentoria passa a CONFIRMADA e a aluna é avisada pelo backend. */
    fun confirmar(agendamentoId: Long) {
        executar(agendamentoId) { usuarioId ->
            ApiClient.api.confirmarAgendamento(usuarioId, agendamentoId)
        }
    }

    /** A mentora cancela: a aluna é avisada e o horário volta a ficar livre. */
    fun cancelar(agendamentoId: Long) {
        executar(agendamentoId) { usuarioId ->
            ApiClient.api.cancelarAgendamento(usuarioId, agendamentoId)
        }
    }

    fun limparErroAcao() {
        _estado.update { it.copy(erroAcao = null) }
    }

    /** Faz a chamada, atualiza a lista com a resposta e traduz os erros do servidor em mensagens. */
    private fun executar(agendamentoId: Long, chamada: suspend (usuarioId: Long) -> AgendamentoResponse) {
        if (_estado.value.acaoEmAndamentoId != null) return

        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(acaoEmAndamentoId = agendamentoId, erroAcao = null) }

            try {
                val resposta = chamada(usuarioId)

                _estado.update { atual ->
                    atual.copy(
                        acaoEmAndamentoId = null,
                        // cancelada sai da lista; as outras só mudam de situação
                        proximas = if (resposta.status == "CANCELADA") {
                            atual.proximas.filter { it.id != resposta.id }
                        } else {
                            atual.proximas.map { if (it.id == resposta.id) resposta else it }
                        }
                    )
                }
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