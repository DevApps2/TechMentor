package com.example.techmentor.ui.oportunidades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.ErroResponse
import com.example.techmentor.data.OportunidadeResponse
import com.example.techmentor.data.Sessao
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

/** Os botões de filtro da tela. [tipoApi] é o texto que o backend espera em ?tipo= (null = sem filtro). */
enum class FiltroTipo(val rotulo: String, val tipoApi: String?) {
    TODOS("Todos", null),
    ESTAGIO("Estágio", "ESTAGIO"),
    EMPREGO("Emprego", "EMPREGO"),
    BOLSA("Bolsas", "BOLSA"),
    EVENTO("Eventos", "EVENTO")
}

data class OportunidadesUiState(
    // true só na primeira carga, quando ainda não há lista para mostrar
    val carregando: Boolean = true,
    // true enquanto o "puxar para atualizar" está buscando dados novos
    val atualizando: Boolean = false,
    val erro: String? = null,
    // null = ainda não carregou
    val oportunidades: List<OportunidadeResponse>? = null,
    val filtro: FiltroTipo = FiltroTipo.TODOS,
    // texto da caixa de busca
    val busca: String = "",
    // oportunidades em que a usuária já se candidatou/inscreveu: id da oportunidade -> status (ENVIADA, EM_ANALISE...)
    val participacoes: Map<Long, String> = emptyMap(),
    // id da oportunidade que está sendo enviada agora
    val enviandoId: Long? = null,
    // falha ao candidatar/inscrever (aparece em vermelho acima do botão)
    val erroEnvio: String? = null,
    // preenchido quando o envio deu certo: a tela mostra a confirmação
    val confirmadaId: Long? = null,
    // true quando o usuário da sessão não existe mais (ex.: o banco foi reiniciado)
    val sessaoExpirada: Boolean = false
)

class OportunidadesViewModel : ViewModel() {

    private val _estado = MutableStateFlow(OportunidadesUiState())
    val estado: StateFlow<OportunidadesUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    // Cada carga ganha um número; só a mais recente pode mexer na tela
    private var numeroDaCarga = 0

    /** Busca as oportunidades com o filtro escolhido e as participações da usuária. */
    fun carregar(manual: Boolean = false) {
        val minhaCarga = ++numeroDaCarga
        tarefa?.cancel()

        val filtro = _estado.value.filtro
        val busca = _estado.value.busca.trim().ifEmpty { null }
        val usuarioId = Sessao.usuarioId

        tarefa = viewModelScope.launch {
            _estado.update {
                when {
                    it.oportunidades == null -> it.copy(carregando = true, erro = null)
                    manual -> it.copy(atualizando = true, erro = null)
                    else -> it.copy(erro = null)
                }
            }

            try {
                val api = ApiClient.api

                coroutineScope {
                    // Só a lista é obrigatória: se "minhas candidaturas" falhar, a tela abre sem as marcações
                    val listaPedido = async { api.listarOportunidades(tipo = filtro.tipoApi, area = null, busca = busca) }
                    val minhasPedido = async {
                        if (usuarioId == null) {
                            null
                        } else {
                            runCatching {
                                api.minhasCandidaturas() + api.minhasInscricoes()
                            }.getOrNull()
                        }
                    }

                    val lista = listaPedido.await()
                    val minhas = minhasPedido.await()

                    if (minhaCarga == numeroDaCarga) {
                        _estado.update {
                            it.copy(
                                oportunidades = lista,
                                participacoes = minhas
                                    ?.associate { p -> p.oportunidadeId to p.status }
                                    ?: it.participacoes
                            )
                        }
                    }
                }
            } catch (e: HttpException) {
                if (minhaCarga == numeroDaCarga) {
                    _estado.update { it.copy(erro = ERRO_CONEXAO) }
                }
            } catch (e: IOException) {
                if (minhaCarga == numeroDaCarga) {
                    _estado.update { it.copy(erro = ERRO_CONEXAO) }
                }
            } finally {
                if (minhaCarga == numeroDaCarga) {
                    _estado.update { it.copy(carregando = false, atualizando = false) }
                }
            }
        }
    }

    private var tarefaDaBusca: Job? = null

    /** Atualiza a lista enquanto a pessoa digita, esperando um instante para não chamar a API a cada letra. */
    fun digitarBusca(texto: String) {
        if (texto.length > 60) return

        _estado.update { it.copy(busca = texto) }

        tarefaDaBusca?.cancel()
        tarefaDaBusca = viewModelScope.launch {
            delay(400)
            carregar()
        }
    }

    fun selecionarFiltro(filtro: FiltroTipo) {
        if (filtro == _estado.value.filtro) return

        _estado.update { it.copy(filtro = filtro) }
        carregar()
    }

    /** Candidatura (vagas e bolsas) ou inscrição (eventos) na oportunidade escolhida. */
    fun participar(oportunidade: OportunidadeResponse) {
        val atual = _estado.value
        if (atual.enviandoId != null || atual.participacoes.containsKey(oportunidade.id)) return

        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        val evento = oportunidade.tipo.equals("EVENTO", ignoreCase = true)

        viewModelScope.launch {
            _estado.update { it.copy(enviandoId = oportunidade.id, erroEnvio = null) }

            try {
                val api = ApiClient.api
                val criada = if (evento) {
                    api.inscreverEmEvento(oportunidade.id)
                } else {
                    api.candidatar(oportunidade.id)
                }

                _estado.update {
                    it.copy(
                        enviandoId = null,
                        participacoes = it.participacoes + (oportunidade.id to criada.status),
                        confirmadaId = oportunidade.id
                    )
                }
            } catch (e: HttpException) {
                val daApi = mensagemDaApi(e)

                // "Usuário não encontrado." = a sessão guardada no aparelho não existe mais no banco
                val sessaoExpirada = e.code() == 404 && daApi?.startsWith("Usuário") == true
                // 409 "já possui inscrição": a usuária já tinha participado em outro aparelho ou sessão
                val jaParticipava = e.code() == 409 && daApi?.contains("já possui", ignoreCase = true) == true

                val mensagem = when (e.code()) {
                    409 -> if (jaParticipava) "Você já participa desta oportunidade." else daApi ?: "Esta oportunidade está encerrada."
                    400, 403, 404 -> daApi ?: "Não foi possível concluir. Tente novamente."
                    else -> ERRO_CONEXAO
                }

                _estado.update {
                    it.copy(
                        enviandoId = null,
                        erroEnvio = mensagem,
                        sessaoExpirada = sessaoExpirada,
                        participacoes = if (jaParticipava) it.participacoes + (oportunidade.id to "ENVIADA") else it.participacoes
                    )
                }
            } catch (e: IOException) {
                _estado.update { it.copy(enviandoId = null, erroEnvio = ERRO_CONEXAO) }
            }
        }
    }

    /** Fecha a tela de confirmação (volta aos detalhes ou à lista). */
    fun fecharConfirmacao() {
        _estado.update { it.copy(confirmadaId = null) }
    }

    fun limparErroEnvio() {
        _estado.update { it.copy(erroEnvio = null) }
    }
}

private fun mensagemDaApi(e: HttpException): String? =
    runCatching {
        Gson().fromJson(e.response()?.errorBody()?.string(), ErroResponse::class.java)?.erro
    }.getOrNull()
