package com.example.techmentor.ui.notificacoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.NotificacaoResponse
import com.example.techmentor.data.Sessao
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class NotificacoesUiState(
    val carregando: Boolean = true,
    // true enquanto o "puxar para atualizar" está buscando dados novos
    val atualizando: Boolean = false,
    val erro: String? = null,
    // nulo = ainda não carregou; lista vazia = carregou e não há nada
    val lista: List<NotificacaoResponse>? = null,
    // true quando o usuário da sessão não existe mais (ex.: o banco foi reiniciado)
    val sessaoExpirada: Boolean = false
)

class NotificacoesViewModel : ViewModel() {

    private val _estado = MutableStateFlow(NotificacoesUiState())
    val estado: StateFlow<NotificacoesUiState> = _estado.asStateFlow()

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
                    it.lista == null -> it.copy(carregando = true, erro = null)
                    manual -> it.copy(atualizando = true)
                    else -> it
                }
            }

            try {
                val recebidas = ApiClient.api.listarNotificacoes(usuarioId, null)

                // as datas da API são ISO, então comparar o texto já ordena por data
                val ordenadas = recebidas.sortedWith(
                    compareByDescending<NotificacaoResponse> { it.criadaEm ?: "" }
                        .thenByDescending { it.id }
                )

                _estado.update { it.copy(carregando = false, erro = null, lista = ordenadas) }

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

    fun marcarComoLida(notificacao: NotificacaoResponse) {
        if (notificacao.lida) return
        val usuarioId = Sessao.usuarioId ?: return

        // Muda na tela na hora; se a chamada falhar, recarrega a lista de verdade
        _estado.update { atual ->
            atual.copy(lista = atual.lista?.map { if (it.id == notificacao.id) it.copy(lida = true) else it })
        }

        viewModelScope.launch {
            try {
                ApiClient.api.marcarNotificacoesComoLidas(usuarioId, notificacao.id)
            } catch (e: HttpException) {
                carregar()
            } catch (e: IOException) {
                carregar()
            }
        }
    }

    fun marcarTodasComoLidas() {
        val usuarioId = Sessao.usuarioId ?: return

        _estado.update { atual ->
            atual.copy(lista = atual.lista?.map { it.copy(lida = true) })
        }

        viewModelScope.launch {
            try {
                ApiClient.api.marcarNotificacoesComoLidas(usuarioId, null)
            } catch (e: HttpException) {
                carregar()
            } catch (e: IOException) {
                carregar()
            }
        }
    }
}