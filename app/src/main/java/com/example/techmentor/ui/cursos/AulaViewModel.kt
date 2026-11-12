package com.example.techmentor.ui.cursos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.AulaProgressoResponse
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.ErroResponse
import com.example.techmentor.data.Sessao
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class AulaUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val cursoTitulo: String = "",
    val aula: AulaProgressoResponse? = null,
    // a aula seguinte da trilha (pode estar bloqueada até esta ser concluída)
    val proxima: AulaProgressoResponse? = null,
    val salvando: Boolean = false,
    val erroAoConcluir: String? = null,
    // true logo depois de concluir: a tela mostra o "parabéns" com o XP e a próxima aula
    val concluidaAgora: Boolean = false,
    // true quando esta foi a última aula que faltava no curso
    val cursoConcluido: Boolean = false
)

/** Uma aula da trilha: carrega o estado dela, conclui e descobre qual é a próxima. */
class AulaViewModel : ViewModel() {

    private val _estado = MutableStateFlow(AulaUiState())
    val estado: StateFlow<AulaUiState> = _estado.asStateFlow()

    private var cursoId = 0L
    private var aulaId = 0L

    fun carregar(cursoId: Long, aulaId: Long) {
        this.cursoId = cursoId
        this.aulaId = aulaId

        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(carregando = false, erro = "Entre na sua conta para assistir às aulas.") }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, erro = null) }
            try {
                val curso = ApiClient.api.listarCursos().firstOrNull { it.id == cursoId }
                    ?: throw IllegalStateException("Não foi possível encontrar este curso.")
                val aulas = ApiClient.api.listarAulasComProgresso(curso.titulo, usuarioId)
                aplicar(curso.titulo, aulas)
            } catch (e: IllegalStateException) {
                _estado.update { it.copy(carregando = false, erro = e.message) }
            } catch (_: HttpException) {
                _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
            } catch (_: IOException) {
                _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
            }
        }
    }

    /** Localiza a aula e a seguinte na lista que veio do servidor (já na ordem de estudo). */
    private fun aplicar(cursoTitulo: String, aulas: List<AulaProgressoResponse>, concluidaAgora: Boolean = false) {
        val indice = aulas.indexOfFirst { it.aulaId == aulaId }
        val aula = aulas.getOrNull(indice)

        if (aula == null) {
            _estado.update { it.copy(carregando = false, erro = "Não foi possível encontrar esta aula.") }
            return
        }
        if (aula.status == "BLOQUEADA") {
            _estado.update { it.copy(carregando = false, erro = "Conclua a aula anterior para liberar esta.") }
            return
        }

        _estado.update {
            it.copy(
                carregando = false,
                erro = null,
                cursoTitulo = cursoTitulo,
                aula = aula,
                proxima = aulas.getOrNull(indice + 1),
                concluidaAgora = concluidaAgora,
                cursoConcluido = concluidaAgora && aulas.all { a -> a.status == "CONCLUIDA" }
            )
        }
    }

    fun concluir() {
        val usuarioId = Sessao.usuarioId ?: return
        val atual = _estado.value
        val aula = atual.aula ?: return
        if (atual.salvando || aula.status != "ATUAL") return

        viewModelScope.launch {
            _estado.update { it.copy(salvando = true, erroAoConcluir = null) }
            try {
                ApiClient.api.concluirAula(atual.cursoTitulo, aula.ordemModulo, aula.ordemAula, usuarioId)
                val aulas = ApiClient.api.listarAulasComProgresso(atual.cursoTitulo, usuarioId)
                _estado.update { it.copy(salvando = false) }
                aplicar(atual.cursoTitulo, aulas, concluidaAgora = true)
            } catch (e: HttpException) {
                val mensagem = lerMensagem(e)
                _estado.update { it.copy(salvando = false, erroAoConcluir = mensagem) }
            } catch (_: IOException) {
                _estado.update { it.copy(salvando = false, erroAoConcluir = ERRO_CONEXAO) }
            }
        }
    }

    fun fecharParabens() {
        _estado.update { it.copy(concluidaAgora = false) }
    }

    /** Lê o {"erro": "..."} que a API devolve; sem ele, a mensagem de conexão. */
    private fun lerMensagem(e: HttpException): String =
        runCatching {
            Gson().fromJson(e.response()?.errorBody()?.string(), ErroResponse::class.java)?.erro
        }.getOrNull() ?: ERRO_CONEXAO
}
