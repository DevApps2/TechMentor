package com.example.techmentor.ui.mentora

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.AgendamentoResponse
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.Sessao
import com.example.techmentor.data.UsuarioResponse
import com.example.techmentor.ui.home.parseDataHora
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
                    // Para uma mentora, a API devolve as mentorias em que ela é a mentora
                    val usuarioPedido = async { api.obterUsuario(usuarioId) }
                    val agendamentosPedido = async { api.listarAgendamentos(usuarioId) }

                    val usuario = usuarioPedido.await()
                    val agendamentos = agendamentosPedido.await()

                    val agora = Date()

                    val proximas = agendamentos
                        .filter { it.status == "AGENDADA" || it.status == "CONFIRMADA" }
                        .mapNotNull { a -> parseDataHora(a.inicio)?.let { data -> a to data } }
                        .filter { (_, data) -> data.after(agora) }
                        .sortedBy { (_, data) -> data.time }
                        .map { (a, _) -> a }

                    _estado.update {
                        it.copy(
                            carregando = false,
                            erro = null,
                            usuario = usuario,
                            proximas = proximas,
                            realizadas = agendamentos.count { a -> a.status == "CONCLUIDA" }
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
}