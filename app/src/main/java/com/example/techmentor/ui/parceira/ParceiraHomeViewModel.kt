package com.example.techmentor.ui.parceira

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.ErroResponse
import com.example.techmentor.data.MetricasParceiraCursoResponse
import com.example.techmentor.data.ParceiraCursoRequest
import com.example.techmentor.data.ParceiraCursoResponse
import com.example.techmentor.data.ParceiraModuloRequest
import com.example.techmentor.data.ParceiraAulaRequest
import com.example.techmentor.data.ParceiraPerfilResponse
import com.example.techmentor.data.Sessao
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class ParceiraHomeUiState(
    val carregando: Boolean = true,
    val atualizando: Boolean = false,
    val perfil: ParceiraPerfilResponse? = null,
    val cursos: List<ParceiraCursoResponse> = emptyList(),
    val metricas: Map<Long, MetricasParceiraCursoResponse> = emptyMap(),
    val erro: String? = null,
    val sessaoExpirada: Boolean = false,
    val criandoCurso: Boolean = false,
    val erroCriacao: String? = null,
    val cursoCriado: Boolean = false,
    val acaoCursoEmAndamento: Boolean = false,
    val erroAcaoCurso: String? = null
)

class ParceiraHomeViewModel : ViewModel() {

    private val _estado = MutableStateFlow(ParceiraHomeUiState())
    val estado: StateFlow<ParceiraHomeUiState> = _estado.asStateFlow()
    private val gson = Gson()

    fun carregar(manual: Boolean = false) {
        val atual = _estado.value
        if (atual.carregando || atual.atualizando) return
        val usuarioId = Sessao.usuarioId
        if (usuarioId == null || Sessao.tipo != "PARCEIRA") {
            _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
            return
        }

        viewModelScope.launch {
            _estado.update {
                if (it.perfil == null) it.copy(carregando = true, erro = null)
                else it.copy(atualizando = manual, erro = null)
            }
            try {
                val (perfil, cursos) = coroutineScope {
                    val perfilPedido = async { ApiClient.api.meuPerfilDeParceira() }
                    val cursosPedido = async { ApiClient.api.meusCursosDeParceira() }
                    perfilPedido.await() to cursosPedido.await()
                }
                val metricas = coroutineScope {
                    cursos.filter { it.situacao == "PUBLICADO" }.map { curso ->
                        async {
                            runCatching { ApiClient.api.metricasDoCursoDaParceira(curso.id) }
                                .getOrNull()?.let { curso.id to it }
                        }
                    }.awaitAll().filterNotNull().toMap()
                }
                _estado.update {
                    it.copy(
                        carregando = false,
                        atualizando = false,
                        perfil = perfil,
                        cursos = cursos.sortedWith(compareBy({ ordemSituacao(it.situacao) }, { it.titulo.lowercase() })),
                        metricas = metricas,
                        erro = null,
                        sessaoExpirada = false
                    )
                }
            } catch (e: HttpException) {
                if (e.code() == 401 || e.code() == 403) {
                    _estado.update { it.copy(carregando = false, atualizando = false, sessaoExpirada = true) }
                } else {
                    _estado.update { it.copy(carregando = false, atualizando = false, erro = mensagemDaApi(e)) }
                }
            } catch (e: IOException) {
                _estado.update { it.copy(carregando = false, atualizando = false, erro = ERRO_CONEXAO) }
            }
        }
    }

    fun criarCurso(dados: ParceiraCursoRequest) {
        if (_estado.value.criandoCurso) return
        viewModelScope.launch {
            _estado.update { it.copy(criandoCurso = true, erroCriacao = null, cursoCriado = false) }
            try {
                ApiClient.api.criarCursoDaParceira(dados)
                _estado.update { it.copy(criandoCurso = false, cursoCriado = true) }
                carregar()
            } catch (e: HttpException) {
                _estado.update { it.copy(criandoCurso = false, erroCriacao = mensagemDaApi(e)) }
            } catch (e: IOException) {
                _estado.update { it.copy(criandoCurso = false, erroCriacao = ERRO_CONEXAO) }
            }
        }
    }

    fun limparResultadoCriacao() {
        _estado.update { it.copy(erroCriacao = null, cursoCriado = false) }
    }

    fun criarModulo(cursoId: Long, titulo: String) = executarAcaoCurso {
        ApiClient.api.criarModuloDaParceira(cursoId, ParceiraModuloRequest(titulo.trim()))
    }

    fun criarAula(moduloId: Long, titulo: String, duracao: Int, url: String) = executarAcaoCurso {
        ApiClient.api.criarAulaDaParceira(moduloId, ParceiraAulaRequest(titulo.trim(), duracao, conteudoUrl = url.trim()))
    }

    fun enviarParaRevisao(cursoId: Long) = executarAcaoCurso {
        ApiClient.api.enviarCursoParaRevisaoDaParceira(cursoId)
    }

    fun limparErroAcaoCurso() {
        _estado.update { it.copy(erroAcaoCurso = null) }
    }

    private fun executarAcaoCurso(acao: suspend () -> Any) {
        if (_estado.value.acaoCursoEmAndamento) return
        viewModelScope.launch {
            _estado.update { it.copy(acaoCursoEmAndamento = true, erroAcaoCurso = null) }
            try {
                acao()
                _estado.update { it.copy(acaoCursoEmAndamento = false) }
                carregar()
            } catch (e: HttpException) {
                _estado.update { it.copy(acaoCursoEmAndamento = false, erroAcaoCurso = mensagemDaApi(e)) }
            } catch (e: IOException) {
                _estado.update { it.copy(acaoCursoEmAndamento = false, erroAcaoCurso = ERRO_CONEXAO) }
            }
        }
    }

    private fun mensagemDaApi(e: HttpException): String {
        val corpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        return runCatching { gson.fromJson(corpo, ErroResponse::class.java)?.erro }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: ERRO_CONEXAO
    }

    private fun ordemSituacao(situacao: String): Int = when (situacao) {
        "EM_REVISAO" -> 0
        "RASCUNHO" -> 1
        "PUBLICADO" -> 2
        "ARQUIVADO" -> 3
        else -> 4
    }
}
