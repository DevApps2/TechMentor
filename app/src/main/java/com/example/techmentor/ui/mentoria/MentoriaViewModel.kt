package com.example.techmentor.ui.mentoria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.MentoraResponse
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

// Espera o usuário parar de digitar antes de buscar na API
private const val ESPERA_DA_BUSCA_MS = 400L

data class MentoriaUiState(
    // true só na primeira carga, quando ainda não há lista para mostrar
    val carregando: Boolean = true,
    // true enquanto o "puxar para atualizar" está buscando dados novos
    val atualizando: Boolean = false,
    // true quando uma busca ou filtro está recarregando (a lista antiga continua na tela)
    val buscando: Boolean = false,
    val erro: String? = null,
    // null = ainda não carregou
    val mentoras: List<MentoraResponse>? = null,
    val areas: List<String> = emptyList(),
    val busca: String = "",
    val areaSelecionada: String? = null
) {
    val temFiltro: Boolean
        get() = busca.isNotBlank() || areaSelecionada != null
}


class MentoriaViewModel : ViewModel() {

    private val _estado = MutableStateFlow(MentoriaUiState())
    val estado: StateFlow<MentoriaUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null
    private var buscaPendente: Job? = null

    // Cada carga ganha um número; só a mais recente pode mexer na tela (evita resposta velha sobrescrever a nova)
    private var numeroDaCarga = 0

    /** Busca as mentoras com a busca e a área que estiverem escolhidas no momento. */
    fun carregar(manual: Boolean = false) {
        val minhaCarga = ++numeroDaCarga
        tarefa?.cancel()

        val busca = _estado.value.busca.trim()
        val area = _estado.value.areaSelecionada
        // as áreas quase não mudam: só buscam de novo na primeira vez ou ao puxar para atualizar
        val precisaDasAreas = manual || _estado.value.areas.isEmpty()

        tarefa = viewModelScope.launch {
            _estado.update {
                when {
                    it.mentoras == null -> it.copy(carregando = true, erro = null)
                    manual -> it.copy(atualizando = true, erro = null)
                    else -> it.copy(buscando = true, erro = null)
                }
            }

            try {
                val api = ApiClient.api

                coroutineScope {
                    // Só a lista é obrigatória: se as áreas falharem, a tela abre sem os filtros
                    val mentorasPedido = async { api.listarMentoras(busca.ifEmpty { null }, area) }
                    val areasPedido = async {
                        if (precisaDasAreas) runCatching { api.listarAreas() }.getOrNull() else null
                    }

                    val mentoras = mentorasPedido.await()
                    val areas = areasPedido.await()

                    if (minhaCarga == numeroDaCarga) {
                        _estado.update {
                            it.copy(
                                mentoras = mentoras,
                                areas = areas?.map { a -> a.nome } ?: it.areas
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
                    _estado.update {
                        it.copy(carregando = false, atualizando = false, buscando = false)
                    }
                }
            }
        }
    }

    /** Chamado a cada letra digitada; só busca na API depois de uma pequena pausa. */
    fun aoDigitarBusca(texto: String) {
        if (texto == _estado.value.busca) return

        _estado.update { it.copy(busca = texto) }

        buscaPendente?.cancel()
        buscaPendente = viewModelScope.launch {
            delay(ESPERA_DA_BUSCA_MS)
            carregar()
        }
    }

    /** Botão de buscar do teclado: não precisa esperar a pausa. */
    fun buscarAgora() {
        buscaPendente?.cancel()
        carregar()
    }

    /** Escolhe a área; tocar na área que já está escolhida (ou em "Todas") tira o filtro. */
    fun selecionarArea(area: String?) {
        val nova = if (area == _estado.value.areaSelecionada) null else area

        _estado.update { it.copy(areaSelecionada = nova) }

        buscaPendente?.cancel()
        carregar()
    }

    fun limparFiltros() {
        _estado.update { it.copy(busca = "", areaSelecionada = null) }

        buscaPendente?.cancel()
        carregar()
    }
}
