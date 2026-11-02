package com.example.techmentor.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.Sessao
import com.example.techmentor.data.UsuarioResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class AdminPerfilUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val sessaoExpirada: Boolean = false,
    val usuario: UsuarioResponse? = null
)


class AdminPerfilViewModel : ViewModel() {

    private val _estado = MutableStateFlow(AdminPerfilUiState())
    val estado: StateFlow<AdminPerfilUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    fun carregar() {
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
                if (it.usuario == null) it.copy(carregando = true, erro = null) else it
            }

            when (val resultado = chamar { ApiClient.api.obterUsuario(adminId) }) {
                is Resultado.Ok -> _estado.update {
                    it.copy(carregando = false, erro = null, usuario = resultado.valor)
                }

                is Resultado.Falha -> _estado.update {
                    it.copy(
                        carregando = false,
                        erro = resultado.mensagem,
                        sessaoExpirada = resultado.sessaoExpirada
                    )
                }
            }
        }
    }
}
