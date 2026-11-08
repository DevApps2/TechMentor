package com.example.techmentor.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.AdminDenuncia
import com.example.techmentor.data.AdminDenunciaStatusRequest
import com.example.techmentor.data.AdminOcultacaoRequest
import com.example.techmentor.data.ApiClient
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


data class AdminDenunciaDetalheUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val sessaoExpirada: Boolean = false,
    val denuncia: AdminDenuncia? = null,
    // null = conta não encontrada (ex.: desativada) ou denúncia geral
    val denunciante: UsuarioResponse? = null,
    val denunciado: UsuarioResponse? = null,
    // true enquanto o novo status está sendo enviado
    val alterando: Boolean = false,
    val erroAoAlterar: String? = null,
    // true enquanto o comentário está sendo escondido (ou mostrado de novo)
    val ocultando: Boolean = false,
    val erroAoOcultar: String? = null,
    // mensagem verde depois de mudar o status
    val avisoDeSucesso: String? = null
)


class AdminDenunciaDetalheViewModel : ViewModel() {

    private val _estado = MutableStateFlow(AdminDenunciaDetalheUiState())
    val estado: StateFlow<AdminDenunciaDetalheUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    fun carregar(denunciaId: Long) {
        // Evita duas cargas ao mesmo tempo
        if (tarefa?.isActive == true) return

        val adminId = Sessao.usuarioId
        if (adminId == null) {
            _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
            return
        }

        tarefa = viewModelScope.launch {
            // O "carregando" só aparece na primeira vez
            _estado.update {
                if (it.denuncia == null) it.copy(carregando = true, erro = null) else it
            }

            coroutineScope {
                val denunciaPedido = async { chamar { ApiClient.adminApi.obterDenuncia(denunciaId) } }
                val usuariosPedido = async { chamar { ApiClient.adminApi.listarUsuarios() } }

                val resultado = denunciaPedido.await()
                val usuarios = usuariosPedido.await().valorOuNull()

                when (resultado) {
                    is Resultado.Ok -> {
                        val denuncia = resultado.valor
                        _estado.update {
                            it.copy(
                                carregando = false,
                                erro = null,
                                denuncia = denuncia,
                                denunciante = usuarios?.firstOrNull { u -> u.id == denuncia.denuncianteId } ?: it.denunciante,
                                denunciado = usuarios?.firstOrNull { u -> u.id == denuncia.denunciadoId } ?: it.denunciado
                            )
                        }
                    }

                    is Resultado.Falha -> _estado.update {
                        it.copy(
                            carregando = false,
                            erro = if (resultado.codigo == 404) "Denúncia não encontrada." else resultado.mensagem,
                            sessaoExpirada = resultado.sessaoExpirada
                        )
                    }
                }
            }
        }
    }

    /** Muda a situação da denúncia (ABERTA, EM_ANALISE ou RESOLVIDA). */
    fun alterarStatus(denunciaId: Long, novoStatus: String) {
        if (_estado.value.alterando) return

        val adminId = Sessao.usuarioId
        if (adminId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(alterando = true, erroAoAlterar = null, avisoDeSucesso = null) }

            val resultado = chamar {
                ApiClient.adminApi.alterarStatusDenuncia(denunciaId, AdminDenunciaStatusRequest(novoStatus))
            }

            when (resultado) {
                is Resultado.Ok -> _estado.update {
                    it.copy(
                        alterando = false,
                        denuncia = resultado.valor,
                        avisoDeSucesso = "Status atualizado para \"${nomeDoStatus(resultado.valor.status)}\"."
                    )
                }

                is Resultado.Falha -> _estado.update {
                    it.copy(
                        alterando = false,
                        erroAoAlterar = resultado.mensagem,
                        sessaoExpirada = resultado.sessaoExpirada
                    )
                }
            }
        }
    }

    /** Esconde o comentário da avaliação denunciada (ou mostra de novo). A nota continua valendo na média. */
    fun definirOcultacao(oculta: Boolean) {
        val atual = _estado.value
        val avaliacaoId = atual.denuncia?.avaliacaoId ?: return
        if (atual.ocultando) return

        val adminId = Sessao.usuarioId
        if (adminId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(ocultando = true, erroAoOcultar = null, avisoDeSucesso = null) }

            val resultado = chamar {
                ApiClient.adminApi.ocultarAvaliacao(avaliacaoId, AdminOcultacaoRequest(oculta))
            }

            when (resultado) {
                is Resultado.Ok -> _estado.update {
                    it.copy(
                        ocultando = false,
                        denuncia = it.denuncia?.copy(avaliacaoOculta = oculta),
                        avisoDeSucesso = if (oculta) "Comentário escondido." else "Comentário visível de novo."
                    )
                }

                is Resultado.Falha -> _estado.update {
                    it.copy(
                        ocultando = false,
                        erroAoOcultar = resultado.mensagem,
                        sessaoExpirada = resultado.sessaoExpirada
                    )
                }
            }
        }
    }
}