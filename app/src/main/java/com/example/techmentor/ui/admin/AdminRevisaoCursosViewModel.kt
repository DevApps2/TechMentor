package com.example.techmentor.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.AdminApi
import com.example.techmentor.data.AdminCursoComentarioRequest
import com.example.techmentor.data.AdminCursoSituacaoRequest
import com.example.techmentor.data.AdminCursoResponse
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.Sessao
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminRevisaoCursosUiState(
    val carregando: Boolean = true,
    val atualizando: Boolean = false,
    val enviando: Boolean = false,
    val cursos: List<AdminCursoResponse>? = null,
    val erro: String? = null,
    val erroAcao: String? = null,
    val mensagem: String? = null,
    val sessaoExpirada: Boolean = false
)

class AdminRevisaoCursosViewModel : ViewModel() {
    private val _estado = MutableStateFlow(AdminRevisaoCursosUiState())
    val estado: StateFlow<AdminRevisaoCursosUiState> = _estado.asStateFlow()
    private var tarefa: Job? = null

    fun carregar(manual: Boolean = false) {
        if (tarefa?.isActive == true) return
        if (Sessao.usuarioId == null) {
            _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
            return
        }

        tarefa = viewModelScope.launch {
            _estado.update {
                when {
                    it.cursos == null -> it.copy(carregando = true, erro = null)
                    manual -> it.copy(atualizando = true, erro = null)
                    else -> it.copy(erro = null)
                }
            }
            when (val resultado = chamar { ApiClient.adminApi.listarCursosAdmin("EM_REVISAO") }) {
                is Resultado.Ok -> _estado.update {
                    it.copy(carregando = false, atualizando = false, cursos = resultado.valor, erro = null)
                }
                is Resultado.Falha -> _estado.update {
                    it.copy(
                        carregando = false,
                        atualizando = false,
                        erro = resultado.mensagem,
                        sessaoExpirada = resultado.sessaoExpirada
                    )
                }
            }
        }
    }

    fun aprovar(cursoId: Long) = executar("Curso aprovado e publicado.") {
        alterarSituacaoCurso(cursoId, AdminCursoSituacaoRequest("PUBLICADO"))
    }

    fun devolver(cursoId: Long, comentario: String) = executar("Curso devolvido para ajustes.") {
        devolverCursoParaAjustes(cursoId, AdminCursoComentarioRequest(comentario))
    }

    fun recusar(cursoId: Long, comentario: String) = executar("Curso recusado.") {
        recusarCurso(cursoId, AdminCursoComentarioRequest(comentario))
    }

    fun limparMensagens() {
        _estado.update { it.copy(erroAcao = null, mensagem = null) }
    }

    private fun executar(mensagem: String, chamada: suspend AdminApi.() -> Any) {
        if (_estado.value.enviando) return
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, erroAcao = null, mensagem = null) }
            when (val resultado = chamar { ApiClient.adminApi.chamada() }) {
                is Resultado.Ok -> {
                    _estado.update { it.copy(enviando = false, mensagem = mensagem) }
                    when (val fila = chamar { ApiClient.adminApi.listarCursosAdmin("EM_REVISAO") }) {
                        is Resultado.Ok -> _estado.update { it.copy(cursos = fila.valor, erro = null) }
                        is Resultado.Falha -> _estado.update {
                            it.copy(erro = "A decisão foi registrada, mas não foi possível atualizar a fila.")
                        }
                    }
                }
                is Resultado.Falha -> _estado.update {
                    it.copy(
                        enviando = false,
                        erroAcao = resultado.mensagem,
                        sessaoExpirada = resultado.sessaoExpirada
                    )
                }
            }
        }
    }
}
