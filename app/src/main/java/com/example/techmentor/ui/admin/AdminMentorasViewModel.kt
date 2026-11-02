package com.example.techmentor.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.MentoraResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class AdminMentorasUiState(
    val carregando: Boolean = true,
    // true enquanto o "puxar para atualizar" está buscando dados novos
    val atualizando: Boolean = false,
    val erro: String? = null,
    // null = ainda não carregou
    val mentoras: List<MentoraResponse>? = null
)


class AdminMentorasViewModel : ViewModel() {

    private val _estado = MutableStateFlow(AdminMentorasUiState())
    val estado: StateFlow<AdminMentorasUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    fun carregar(manual: Boolean = false) {
        // Evita duas cargas ao mesmo tempo
        if (tarefa?.isActive == true) return

        tarefa = viewModelScope.launch {
            // O "carregando" só aparece na primeira vez; depois atualiza por baixo dos panos
            _estado.update {
                when {
                    it.mentoras == null -> it.copy(carregando = true, erro = null)
                    manual -> it.copy(atualizando = true, erro = null)
                    else -> it.copy(erro = null)
                }
            }

            when (val resultado = chamar { ApiClient.api.listarMentoras(null, null) }) {
                is Resultado.Ok -> _estado.update {
                    it.copy(carregando = false, atualizando = false, mentoras = resultado.valor)
                }

                is Resultado.Falha -> _estado.update {
                    it.copy(carregando = false, atualizando = false, erro = resultado.mensagem)
                }
            }
        }
    }
}
