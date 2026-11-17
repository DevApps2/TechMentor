package com.example.techmentor.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.AdminCandidatura
import com.example.techmentor.data.AdminCandidaturaStatusRequest
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.Sessao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class AdminCandidaturasUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val sessaoExpirada: Boolean = false,
    // null = ainda não carregou
    val candidaturas: List<AdminCandidatura>? = null,
    // id da candidatura que está mudando de situação agora
    val alterandoId: Long? = null,
    val erroAoAlterar: String? = null,
    // id da candidatura cujo currículo está sendo baixado agora
    val baixandoCurriculoId: Long? = null,
    // o PDF já baixado, esperando a tela abri-lo (a tela chama curriculoAberto() depois)
    val curriculoParaAbrir: File? = null,
    val erroCurriculo: String? = null
)

/** Quem se candidatou a uma oportunidade e a situação de cada candidatura (a admin pode mudar). */
class AdminCandidaturasViewModel : ViewModel() {

    private val _estado = MutableStateFlow(AdminCandidaturasUiState())
    val estado: StateFlow<AdminCandidaturasUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    fun carregar(oportunidadeId: Long) {
        // Evita duas cargas ao mesmo tempo
        if (tarefa?.isActive == true) return

        val adminId = Sessao.usuarioId
        if (adminId == null) {
            _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
            return
        }

        tarefa = viewModelScope.launch {
            // O "carregando" só aparece na primeira vez
            _estado.update { if (it.candidaturas == null) it.copy(carregando = true, erro = null) else it }

            when (val resultado = chamar { ApiClient.adminApi.listarCandidaturas(oportunidadeId) }) {
                is Resultado.Ok -> _estado.update {
                    it.copy(carregando = false, erro = null, candidaturas = resultado.valor)
                }

                is Resultado.Falha -> _estado.update {
                    it.copy(
                        carregando = false,
                        erro = if (resultado.codigo == 404) "Oportunidade não encontrada." else resultado.mensagem,
                        sessaoExpirada = resultado.sessaoExpirada
                    )
                }
            }
        }
    }

    /**
     * Baixa o currículo para a pasta temporária do app (cache/shared, a mesma que o FileProvider já libera)
     * e entrega o arquivo à tela, que o abre num leitor de PDF.
     */
    fun baixarCurriculo(candidaturaId: Long, cacheDir: File) {
        if (_estado.value.baixandoCurriculoId != null) return

        viewModelScope.launch {
            _estado.update { it.copy(baixandoCurriculoId = candidaturaId, erroCurriculo = null) }

            when (val resultado = chamar { ApiClient.adminApi.baixarCurriculo(candidaturaId) }) {

                is Resultado.Ok -> {
                    val arquivo = withContext(Dispatchers.IO) {
                        runCatching {
                            val pasta = File(cacheDir, "shared").apply { mkdirs() }
                            val destino = File(pasta, "curriculo-$candidaturaId.pdf")
                            resultado.valor.use { corpo ->
                                corpo.byteStream().use { entrada ->
                                    destino.outputStream().use { saida -> entrada.copyTo(saida) }
                                }
                            }
                            destino
                        }.getOrNull()
                    }

                    _estado.update {
                        if (arquivo != null) {
                            it.copy(baixandoCurriculoId = null, curriculoParaAbrir = arquivo)
                        } else {
                            it.copy(baixandoCurriculoId = null, erroCurriculo = "Não foi possível baixar o currículo. Tente de novo.")
                        }
                    }
                }

                is Resultado.Falha -> _estado.update {
                    it.copy(
                        baixandoCurriculoId = null,
                        erroCurriculo = if (resultado.codigo == 404) "Esta candidata não anexou currículo." else resultado.mensagem,
                        sessaoExpirada = resultado.sessaoExpirada
                    )
                }
            }
        }
    }

    /** A tela já abriu (ou tentou abrir) o PDF. */
    fun curriculoAberto(erro: String? = null) {
        _estado.update { it.copy(curriculoParaAbrir = null, erroCurriculo = erro ?: it.erroCurriculo) }
    }

    /** Muda a situação (EM_ANALISE, APROVADA, RECUSADA ou ENVIADA). A candidata recebe uma notificação. */
    fun alterarStatus(candidaturaId: Long, novoStatus: String) {
        if (_estado.value.alterandoId != null) return

        val adminId = Sessao.usuarioId
        if (adminId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(alterandoId = candidaturaId, erroAoAlterar = null) }

            val resultado = chamar {
                ApiClient.adminApi.alterarStatusDaCandidatura(candidaturaId, AdminCandidaturaStatusRequest(novoStatus)
                )
            }

            when (resultado) {
                is Resultado.Ok -> _estado.update { atual ->
                    atual.copy(
                        alterandoId = null,
                        candidaturas = atual.candidaturas?.map {
                            if (it.id == candidaturaId) resultado.valor else it
                        }
                    )
                }

                is Resultado.Falha -> _estado.update {
                    it.copy(
                        alterandoId = null,
                        erroAoAlterar = resultado.mensagem,
                        sessaoExpirada = resultado.sessaoExpirada
                    )
                }
            }
        }
    }
}
