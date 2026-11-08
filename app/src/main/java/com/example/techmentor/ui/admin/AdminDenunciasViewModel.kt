package com.example.techmentor.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.AdminDenuncia
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.Sessao
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.techmentor.data.AdminOcultacaoRequest


data class AdminDenunciasUiState(
    val carregando: Boolean = true,
    // true enquanto o "puxar para atualizar" está buscando dados novos
    val atualizando: Boolean = false,
    val erro: String? = null,
    val sessaoExpirada: Boolean = false,
    // null = ainda não carregou
    val denuncias: List<AdminDenuncia>? = null,
    // ID -> nome, para mostrar "Maria denunciou João" em vez de números
    val nomes: Map<Long, String> = emptyMap(),
    // null = todas; ou ABERTA, EM_ANALISE, RESOLVIDA
    val filtro: String? = null
)


class AdminDenunciasViewModel : ViewModel() {

    private val _estado = MutableStateFlow(AdminDenunciasUiState())
    val estado: StateFlow<AdminDenunciasUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    fun carregar(manual: Boolean = false) {
        // Evita duas cargas ao mesmo tempo
        if (tarefa?.isActive == true) return

        val adminId = Sessao.usuarioId
        if (adminId == null) {
            _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
            return
        }

        tarefa = viewModelScope.launch {
            // O "carregando" só aparece na primeira vez; depois atualiza por baixo dos panos
            _estado.update {
                when {
                    it.denuncias == null -> it.copy(carregando = true, erro = null)
                    manual -> it.copy(atualizando = true, erro = null)
                    else -> it.copy(erro = null)
                }
            }

            coroutineScope {
                // Só as denúncias são obrigatórias: se os nomes falharem, a lista mostra "Usuária #número"
                val denunciasPedido = async { chamar { ApiClient.adminApi.listarDenuncias() } }
                val usuariosPedido = async { chamar { ApiClient.adminApi.listarUsuarios() } }

                val resultado = denunciasPedido.await()
                val usuarios = usuariosPedido.await().valorOuNull()

                when (resultado) {
                    is Resultado.Ok -> _estado.update {
                        it.copy(
                            carregando = false,
                            atualizando = false,
                            denuncias = resultado.valor,
                            nomes = usuarios?.associate { u -> u.id to u.nome } ?: it.nomes
                        )
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
    }

    fun selecionarFiltro(status: String?) {
        _estado.update { it.copy(filtro = status) }
    }
}
