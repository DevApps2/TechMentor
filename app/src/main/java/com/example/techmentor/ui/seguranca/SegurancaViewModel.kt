package com.example.techmentor.ui.seguranca

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.BloqueioRequest
import com.example.techmentor.data.BloqueioResponse
import com.example.techmentor.data.ConteudoSegurancaResponse
import com.example.techmentor.data.DenunciaRequest
import com.example.techmentor.data.DenunciaResponse
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.ErroResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.data.UsuarioResumoResponse
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

// Espera a pessoa parar de digitar antes de buscar usuários na API
private const val ESPERA_DA_BUSCA_MS = 400L

/** Motivos da denúncia. [api] é o texto que o backend espera; [rotulo] é o que aparece na tela. */
enum class MotivoDenuncia(val rotulo: String, val api: String) {
    ASSEDIO("Assédio", "ASSEDIO"),
    COMPORTAMENTO("Comportamento inadequado", "COMPORTAMENTO_INADEQUADO"),
    SPAM("Spam ou fraude", "SPAM"),
    ODIO("Discurso de ódio", "DISCURSO_DE_ODIO"),
    PERFIL_FALSO("Perfil falso", "PERFIL_FALSO"),
    OUTRO("Outro", "OUTRO")
}

data class SegurancaUiState(
    // true só na primeira carga, quando ainda não há conteúdo para mostrar
    val carregando: Boolean = true,
    // true enquanto o "puxar para atualizar" está buscando dados novos
    val atualizando: Boolean = false,
    val erro: String? = null,
    // null = ainda não carregou
    val conteudos: List<ConteudoSegurancaResponse>? = null,
    val bloqueados: List<BloqueioResponse> = emptyList(),
    // denúncias feitas pela própria pessoa (da mais nova para a mais antiga); null = ainda não carregou
    val minhasDenuncias: List<DenunciaResponse>? = null,
    // busca de usuários (para denunciar e para bloquear)
    val resultadosBusca: List<UsuarioResumoResponse> = emptyList(),
    val buscando: Boolean = false,
    // denúncia, bloqueio e desbloqueio em andamento
    val enviando: Boolean = false,
    // falha ao denunciar, bloquear ou desbloquear (aparece em vermelho na tela)
    val erroAcao: String? = null,
    // preenchido quando a denúncia foi registrada: a tela troca o formulário pela confirmação
    val denunciaEnviada: Boolean = false,
    // true quando o usuário da sessão não existe mais (ex.: o banco foi reiniciado)
    val sessaoExpirada: Boolean = false
) {
    val orientacoes: List<ConteudoSegurancaResponse>
        get() = conteudos.orEmpty().filter { it.tipo.equals("ORIENTACAO", ignoreCase = true) }

    val canaisDeAjuda: List<ConteudoSegurancaResponse>
        get() = conteudos.orEmpty().filter { it.tipo.equals("CANAL_AJUDA", ignoreCase = true) }
}

class SegurancaViewModel : ViewModel() {

    private val _estado = MutableStateFlow(SegurancaUiState())
    val estado: StateFlow<SegurancaUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null
    private var buscaPendente: Job? = null

    // Cada busca ganha um número; só a mais recente pode mexer na tela
    private var numeroDaBusca = 0

    /** Carrega os textos de segurança e a lista de bloqueados. */
    fun carregar(manual: Boolean = false) {
        // Evita duas cargas ao mesmo tempo
        if (tarefa?.isActive == true) return

        val usuarioId = Sessao.usuarioId

        tarefa = viewModelScope.launch {
            // O "carregando" só aparece na primeira vez; depois atualiza por baixo dos panos
            _estado.update {
                when {
                    it.conteudos == null -> it.copy(carregando = true, erro = null)
                    manual -> it.copy(atualizando = true)
                    else -> it
                }
            }

            try {
                val api = ApiClient.api

                coroutineScope {
                    // Só os textos são obrigatórios: se a lista de bloqueados falhar, a tela abre sem ela
                    val conteudosPedido = async { api.listarConteudosSeguranca() }
                    val bloqueiosPedido = async {
                        if (usuarioId == null) null else runCatching { api.listarBloqueios() }.getOrNull()
                    }
                    val denunciasPedido = async {
                        if (usuarioId == null) null else runCatching { api.minhasDenuncias() }.getOrNull()
                    }

                    val conteudos = conteudosPedido.await()
                    val bloqueios = bloqueiosPedido.await()
                    val denuncias = denunciasPedido.await()

                    _estado.update {
                        it.copy(
                            conteudos = conteudos,
                            erro = null,
                            bloqueados = bloqueios ?: it.bloqueados,
                            minhasDenuncias = denuncias ?: it.minhasDenuncias
                        )
                    }
                }
            } catch (e: HttpException) {
                _estado.update { it.copy(erro = ERRO_CONEXAO) }
            } catch (e: IOException) {
                _estado.update { it.copy(erro = ERRO_CONEXAO) }
            } finally {
                _estado.update { it.copy(carregando = false, atualizando = false) }
            }
        }
    }

    /** Chamado a cada letra digitada; só busca na API depois de uma pausa e com pelo menos 2 letras. */
    fun buscarUsuarios(texto: String) {
        buscaPendente?.cancel()
        val minhaBusca = ++numeroDaBusca

        val termo = texto.trim()
        val usuarioId = Sessao.usuarioId

        if (termo.length < 2 || usuarioId == null) {
            _estado.update { it.copy(resultadosBusca = emptyList(), buscando = false) }
            return
        }

        buscaPendente = viewModelScope.launch {
            delay(ESPERA_DA_BUSCA_MS)
            _estado.update { it.copy(buscando = true) }

            try {
                val lista = ApiClient.api.buscarUsuarios(termo)

                if (minhaBusca == numeroDaBusca) {
                    _estado.update { it.copy(resultadosBusca = lista) }
                }
            } catch (e: HttpException) {
                if (minhaBusca == numeroDaBusca) {
                    _estado.update { it.copy(resultadosBusca = emptyList()) }
                }
            } catch (e: IOException) {
                if (minhaBusca == numeroDaBusca) {
                    _estado.update { it.copy(resultadosBusca = emptyList()) }
                }
            } finally {
                if (minhaBusca == numeroDaBusca) {
                    _estado.update { it.copy(buscando = false) }
                }
            }
        }
    }

    /** Registra a denúncia. [usuarioDenunciadoId] pode ser nulo (denúncia geral). */
    fun denunciar(usuarioDenunciadoId: Long?, motivo: MotivoDenuncia, descricao: String) {
        executarAcao { api, usuarioId ->
            val criada = api.denunciar(
                DenunciaRequest(
                    usuarioDenunciadoId = usuarioDenunciadoId,
                    motivo = motivo.api,
                    descricao = descricao.trim()
                )
            )
            _estado.update {
                it.copy(
                    denunciaEnviada = true,
                    // a denúncia nova já aparece em "Minhas denúncias", no topo
                    minhasDenuncias = listOf(criada) + it.minhasDenuncias.orEmpty()
                )
            }
        }
    }

    fun bloquear(bloqueadoId: Long, aoTerminar: () -> Unit) {
        executarAcao { api, usuarioId ->
            val criado = api.bloquear(BloqueioRequest(bloqueadoId))

            _estado.update {
                it.copy(
                    bloqueados = it.bloqueados.filter { b -> b.usuarioId != criado.usuarioId } + criado,
                    resultadosBusca = emptyList()
                )
            }
            aoTerminar()
        }
    }

    fun desbloquear(bloqueadoId: Long) {
        executarAcao { api, usuarioId ->
            api.desbloquear(bloqueadoId)

            _estado.update {
                it.copy(bloqueados = it.bloqueados.filter { b -> b.usuarioId != bloqueadoId })
            }
        }
    }

    /** Limpa mensagens, confirmação e busca ao trocar de tela. */
    fun limparAcao() {
        buscaPendente?.cancel()
        numeroDaBusca++
        _estado.update {
            it.copy(
                erroAcao = null,
                denunciaEnviada = false,
                resultadosBusca = emptyList(),
                buscando = false
            )
        }
    }

    /** Roda uma ação que precisa do usuário da sessão e traduz os erros do servidor em mensagens. */
    private fun executarAcao(acao: suspend (api: com.example.techmentor.data.TechMentorApi, usuarioId: Long) -> Unit) {
        if (_estado.value.enviando) return

        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, erroAcao = null) }

            try {
                acao(ApiClient.api, usuarioId)
                _estado.update { it.copy(enviando = false) }
            } catch (e: HttpException) {
                val daApi = mensagemDaApi(e)

                // "Usuário não encontrado." = a sessão guardada no aparelho não existe mais no banco
                val sessaoExpirada = e.code() == 404 && daApi?.startsWith("Usuário") == true

                val mensagem = when (e.code()) {
                    // 409 = "já denunciou esta pessoa nas últimas 24 horas"; 429 = "muitas denúncias em pouco tempo"
                    400, 403, 404, 409, 429 -> daApi ?: "Não foi possível concluir. Tente novamente."
                    else -> ERRO_CONEXAO
                }

                _estado.update {
                    it.copy(enviando = false, erroAcao = mensagem, sessaoExpirada = sessaoExpirada)
                }
            } catch (e: IOException) {
                _estado.update { it.copy(enviando = false, erroAcao = ERRO_CONEXAO) }
            }
        }
    }
}

private fun mensagemDaApi(e: HttpException): String? =
    runCatching {
        Gson().fromJson(e.response()?.errorBody()?.string(), ErroResponse::class.java)?.erro
    }.getOrNull()
