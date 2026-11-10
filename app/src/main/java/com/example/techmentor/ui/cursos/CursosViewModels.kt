package com.example.techmentor.ui.cursos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.AulaProgressoResponse
import com.example.techmentor.data.CursoResponse
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.Sessao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class CursoCatalogoItem(
    val curso: CursoResponse,
    val iniciado: Boolean,
    val aulasConcluidas: Long
)

data class CursosUiState(
    val carregando: Boolean = true,
    val cursos: List<CursoCatalogoItem> = emptyList(),
    val erro: String? = null
)

class CursosViewModel : ViewModel() {
    private val _estado = MutableStateFlow(CursosUiState())
    val estado: StateFlow<CursosUiState> = _estado.asStateFlow()

    fun carregar(forcar: Boolean = false) {
        if (!forcar && !_estado.value.carregando && _estado.value.cursos.isNotEmpty()) return
        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, erro = null) }
            try {
                val cursos = ApiClient.api.listarCursos().filter { it.ativo }
                val matriculas = Sessao.usuarioId?.let { id -> ApiClient.api.listarMatriculas(id) }.orEmpty()
                val porTitulo = matriculas.associateBy { it.tituloCurso }
                _estado.update {
                    it.copy(
                        carregando = false,
                        cursos = cursos.map { curso ->
                            val matricula = porTitulo[curso.titulo]
                            CursoCatalogoItem(
                                curso = curso,
                                iniciado = matricula != null,
                                aulasConcluidas = matricula?.aulasConcluidas ?: 0L
                            )
                        },
                        erro = null
                    )
                }
            } catch (_: HttpException) {
                _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
            } catch (_: IOException) {
                _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
            }
        }
    }
}

data class CursoDetalheUiState(
    val carregando: Boolean = true,
    val curso: CursoResponse? = null,
    val aulas: List<AulaProgressoResponse> = emptyList(),
    val salvandoAula: Boolean = false,
    val erro: String? = null
) {
    val aulasConcluidas: Int get() = aulas.count { it.status == "CONCLUIDA" }
    val totalAulas: Int get() = curso?.totalAulas?.toInt() ?: 0
}

class CursoDetalheViewModel : ViewModel() {
    private val _estado = MutableStateFlow(CursoDetalheUiState())
    val estado: StateFlow<CursoDetalheUiState> = _estado.asStateFlow()

    fun carregar(cursoId: Long) {
        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(carregando = false, erro = "Entre na sua conta para acompanhar seu progresso.") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, erro = null) }
            try {
                val curso = ApiClient.api.listarCursos().firstOrNull { it.id == cursoId }
                    ?: throw IllegalStateException("Curso não encontrado")
                val aulas = ApiClient.api.listarAulasComProgresso(curso.titulo, usuarioId)
                _estado.update { it.copy(carregando = false, curso = curso, aulas = aulas, erro = null) }
            } catch (_: HttpException) {
                _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
            } catch (_: IOException) {
                _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
            } catch (_: IllegalStateException) {
                _estado.update { it.copy(carregando = false, erro = "Não foi possível encontrar este curso.") }
            }
        }
    }

    fun concluirAula(aula: AulaProgressoResponse) {
        val usuarioId = Sessao.usuarioId ?: return
        val curso = _estado.value.curso ?: return
        if (_estado.value.salvandoAula || aula.status != "ATUAL") return
        viewModelScope.launch {
            _estado.update { it.copy(salvandoAula = true, erro = null) }
            try {
                ApiClient.api.concluirAula(curso.titulo, aula.ordemModulo, aula.ordemAula, usuarioId)
                val aulas = ApiClient.api.listarAulasComProgresso(curso.titulo, usuarioId)
                _estado.update { it.copy(salvandoAula = false, aulas = aulas, erro = null) }
            } catch (_: HttpException) {
                _estado.update { it.copy(salvandoAula = false, erro = ERRO_CONEXAO) }
            } catch (_: IOException) {
                _estado.update { it.copy(salvandoAula = false, erro = ERRO_CONEXAO) }
            }
        }
    }
}
