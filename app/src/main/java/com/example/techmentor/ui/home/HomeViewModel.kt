package com.example.techmentor.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.AgendamentoResponse
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.MatriculaResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.data.UsuarioResponse
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


data class HomeUiState(
    val carregando: Boolean = true,
    // true enquanto o "puxar para atualizar" está buscando dados novos
    val atualizando: Boolean = false,
    val erro: String? = null,
    val usuario: UsuarioResponse? = null,
    val cursoEmAndamento: MatriculaResponse? = null,
    val proximaMentoria: AgendamentoResponse? = null,
    // true quando o usuário da sessão não existe mais (ex.: o banco foi reiniciado)
    val sessaoExpirada: Boolean = false
)


class HomeViewModel : ViewModel() {

    private val _estado = MutableStateFlow(HomeUiState())
    val estado: StateFlow<HomeUiState> = _estado.asStateFlow()

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
                    // Só o usuário é obrigatório: se curso ou mentoria falharem, a Home abre sem eles.
                    val usuarioPedido = async { api.obterUsuario(usuarioId) }
                    val matriculasPedido = async { runCatching { api.listarMatriculas(usuarioId) }.getOrNull() }
                    val agendamentosPedido = async { runCatching { api.listarAgendamentos(usuarioId) }.getOrNull() }

                    val usuario = usuarioPedido.await()
                    val matriculas = matriculasPedido.await()
                    val agendamentos = agendamentosPedido.await()

                    _estado.update {
                        it.copy(
                            carregando = false,
                            erro = null,
                            usuario = usuario,
                            // a API já manda os cursos em andamento primeiro
                            cursoEmAndamento = matriculas?.firstOrNull { m -> m.concluidoEm == null },
                            proximaMentoria = agendamentos?.let { lista -> escolherProximaMentoria(lista) }
                        )
                    }
                }
            } catch (e: HttpException) {
                if (e.code() == 404 || e.code() == 401) {
                    _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
                } else {
                    _estado.update { it.copy(carregando = false, erro = "Erro no servidor (${e.code()}).") }
                }
            } catch (e: IOException) {
                _estado.update { it.copy(carregando = false, erro = "Sem conexão com o servidor.") }
            } finally {
                _estado.update { it.copy(atualizando = false) }
            }
        }
    }
}