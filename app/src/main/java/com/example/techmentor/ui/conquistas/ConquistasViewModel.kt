package com.example.techmentor.ui.conquistas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ConquistaResponse
import com.example.techmentor.data.DesafioResponse
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.MatriculaResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.data.UsuarioConquistaResponse
import com.example.techmentor.data.UsuarioDesafioResponse
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

data class CelebracaoConquista(
    val chave: String,
    val titulo: String,
    val descricao: String,
    val xp: Int = 0,
    val nivel: Int? = null
)

data class ConquistasUiState(
    val carregando: Boolean = true,
    val atualizando: Boolean = false,
    val erro: String? = null,
    val erroDesafio: String? = null,
    val usuario: UsuarioResponse? = null,
    val catalogo: List<ConquistaResponse> = emptyList(),
    val desbloqueadas: List<UsuarioConquistaResponse> = emptyList(),
    val matriculas: List<MatriculaResponse> = emptyList(),
    val desafios: List<DesafioResponse> = emptyList(),
    val participacoes: List<UsuarioDesafioResponse> = emptyList(),
    val desafiosEntrando: Set<Long> = emptySet(),
    val celebracoes: List<CelebracaoConquista> = emptyList(),
    val sessaoExpirada: Boolean = false
)

class ConquistasViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencias = application.getSharedPreferences("gamificacao", 0)
    private val _estado = MutableStateFlow(ConquistasUiState())
    val estado: StateFlow<ConquistasUiState> = _estado.asStateFlow()
    private var tarefa: Job? = null

    fun carregar(manual: Boolean = false) {
        if (tarefa?.isActive == true) return

        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
            return
        }

        tarefa = viewModelScope.launch {
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
                    val usuarioPedido = async { api.obterUsuario(usuarioId) }
                    val catalogoPedido = async { runCatching { api.listarConquistas() }.getOrNull() }
                    val desbloqueadasPedido = async { runCatching { api.listarConquistasDoUsuario(usuarioId) }.getOrNull() }
                    val matriculasPedido = async { runCatching { api.listarMatriculas(usuarioId) }.getOrNull() }
                    val desafiosPedido = async { runCatching { api.listarDesafiosAtivos() }.getOrNull() }
                    val participacoesPedido = async { runCatching { api.listarDesafiosDoUsuario(usuarioId) }.getOrNull() }

                    val usuario = usuarioPedido.await()
                    val catalogoRecebido = catalogoPedido.await()
                    val desbloqueadasRecebidas = desbloqueadasPedido.await()
                    val matriculas = matriculasPedido.await()
                    val desafios = desafiosPedido.await()
                    val participacoes = participacoesPedido.await()

                    val catalogo = catalogoRecebido ?: desbloqueadasRecebidas?.map {
                        ConquistaResponse(
                            id = it.conquistaId,
                            nome = it.nomeConquista,
                            descricao = "Conquista desbloqueada.",
                            iconeUrl = null,
                            xpBonus = 0
                        )
                    } ?: _estado.value.catalogo

                    val celebracoesNovas = detectarNovasConquistas(
                        usuarioId,
                        usuario,
                        catalogo,
                        desbloqueadasRecebidas
                    )

                    _estado.update {
                        it.copy(
                            carregando = false,
                            erro = null,
                            erroDesafio = if (desafios == null || participacoes == null) {
                                "Não foi possível atualizar os desafios. Puxe a tela para tentar novamente."
                            } else null,
                            usuario = usuario,
                            catalogo = catalogo,
                            desbloqueadas = desbloqueadasRecebidas ?: it.desbloqueadas,
                            matriculas = matriculas ?: it.matriculas,
                            desafios = desafios ?: it.desafios,
                            participacoes = participacoes ?: it.participacoes,
                            celebracoes = (it.celebracoes + celebracoesNovas).distinctBy { c -> c.chave }
                        )
                    }
                }
            } catch (e: HttpException) {
                if (e.code() == 401 || e.code() == 404) {
                    _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
                } else {
                    _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
                }
            } catch (_: IOException) {
                _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
            } finally {
                _estado.update { it.copy(atualizando = false) }
            }
        }
    }

    fun participar(desafio: DesafioResponse) {
        val usuarioId = Sessao.usuarioId ?: return
        if (_estado.value.desafiosEntrando.contains(desafio.id)) return
        if (_estado.value.participacoes.any { it.desafioId == desafio.id }) return

        viewModelScope.launch {
            _estado.update {
                it.copy(desafiosEntrando = it.desafiosEntrando + desafio.id, erroDesafio = null)
            }
            try {
                val participacao = ApiClient.api.participarDoDesafio(desafio.titulo, usuarioId)
                _estado.update {
                    it.copy(
                        participacoes = (it.participacoes.filterNot { p -> p.desafioId == desafio.id } + participacao),
                        desafiosEntrando = it.desafiosEntrando - desafio.id,
                        erroDesafio = null
                    )
                }
            } catch (e: HttpException) {
                val mensagem = when (e.code()) {
                    404 -> "Este desafio não está mais disponível. Atualize a tela."
                    409 -> "Você já está participando deste desafio."
                    else -> ERRO_CONEXAO
                }
                _estado.update {
                    it.copy(desafiosEntrando = it.desafiosEntrando - desafio.id, erroDesafio = mensagem)
                }
            } catch (_: IOException) {
                _estado.update {
                    it.copy(desafiosEntrando = it.desafiosEntrando - desafio.id, erroDesafio = ERRO_CONEXAO)
                }
            }
        }
    }

    fun dispensarCelebracao() {
        _estado.update { it.copy(celebracoes = it.celebracoes.drop(1)) }
    }

    private fun detectarNovasConquistas(
        usuarioId: Long,
        usuario: UsuarioResponse,
        catalogo: List<ConquistaResponse>,
        desbloqueadas: List<UsuarioConquistaResponse>?
    ): List<CelebracaoConquista> {
        val resultado = mutableListOf<CelebracaoConquista>()
        if (desbloqueadas != null) {
            val chaveMedalhas = "medalhas_vistas_$usuarioId"
            val medalhasAnteriores = preferencias.getStringSet(chaveMedalhas, null)
            val medalhasAtuais = desbloqueadas.map { it.conquistaId.toString() }.toSet()
            val novas = if (medalhasAnteriores == null) {
                emptyList()
            } else {
                desbloqueadas.filterNot { medalhasAnteriores.contains(it.conquistaId.toString()) }
            }
            preferencias.edit().putStringSet(chaveMedalhas, medalhasAtuais).apply()

            resultado += novas.map { desbloqueio ->
                val medalha = catalogo.firstOrNull { it.id == desbloqueio.conquistaId }
                CelebracaoConquista(
                    chave = "conquista_${desbloqueio.conquistaId}",
                    titulo = medalha?.nome ?: desbloqueio.nomeConquista,
                    descricao = "Uma nova medalha entrou para sua coleção.",
                    xp = medalha?.xpBonus ?: 0
                )
            }
        }

        val chaveXp = "xp_visto_$usuarioId"
        val xpAnterior = preferencias.getInt(chaveXp, -1)
        val xpMedalhasNovas = resultado.filter { it.chave.startsWith("conquista_") }.sumOf { it.xp }
        val chaveNivel = "nivel_visto_$usuarioId"
        val nivelAnterior = preferencias.getInt(chaveNivel, -1)
        if (nivelAnterior >= 0 && usuario.nivel > nivelAnterior) {
            val xpGanhoDesdeVisita = if (xpAnterior >= 0) (usuario.xpTotal - xpAnterior).coerceAtLeast(0) else 0
            resultado += CelebracaoConquista(
                chave = "nivel_${usuario.nivel}",
                titulo = "Você subiu de nível!",
                descricao = "Sua dedicação levou você ao nível ${usuario.nivel}.",
                xp = (xpGanhoDesdeVisita - xpMedalhasNovas).coerceAtLeast(0),
                nivel = usuario.nivel
            )
        }
        preferencias.edit()
            .putInt(chaveNivel, usuario.nivel)
            .putInt(chaveXp, usuario.xpTotal)
            .apply()
        return resultado
    }
}
