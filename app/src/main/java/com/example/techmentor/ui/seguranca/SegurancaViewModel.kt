package com.example.techmentor.ui.seguranca

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ERRO_CONEXAO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class SegurancaUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    // null = ainda não carregou
    val conteudos: List<ConteudoSegurancaResponse>? = null,
    // Bloqueios SIMULADOS: ficam só na memória do app, nada é enviado ao servidor
    val bloqueados: List<String> = emptyList()
) {
    val orientacoes: List<ConteudoSegurancaResponse>
        get() = conteudos.orEmpty().filter { it.tipo.equals("ORIENTACAO", ignoreCase = true) }

    val canaisDeAjuda: List<ConteudoSegurancaResponse>
        get() = conteudos.orEmpty().filter { it.tipo.equals("CANAL_AJUDA", ignoreCase = true) }
}

class SegurancaViewModel : ViewModel() {

    private val _estado = MutableStateFlow(SegurancaUiState())
    val estado: StateFlow<SegurancaUiState> = _estado.asStateFlow()

    init {
        carregar()
    }

    fun carregar() {

        viewModelScope.launch {

            _estado.update { it.copy(carregando = true, erro = null) }

            try {
                val lista = SegurancaApiClient.api.listarConteudos()
                _estado.update { it.copy(conteudos = lista, carregando = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Sem internet, servidor fora do ar ou resposta inesperada
                _estado.update { it.copy(erro = ERRO_CONEXAO, carregando = false) }
            }
        }
    }

    fun bloquear(nome: String) {

        val limpo = nome.trim()
        if (limpo.isEmpty()) return

        _estado.update {
            if (it.bloqueados.any { existente -> existente.equals(limpo, ignoreCase = true) }) it
            else it.copy(bloqueados = it.bloqueados + limpo)
        }
    }

    fun desbloquear(nome: String) {
        _estado.update { it.copy(bloqueados = it.bloqueados - nome) }
    }
}
