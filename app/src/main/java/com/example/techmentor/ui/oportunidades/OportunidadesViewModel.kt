package com.example.techmentor.ui.oportunidades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ERRO_CONEXAO
import kotlinx.coroutines.Job
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
    val filtro: FiltroTipo = FiltroTipo.TODOS
)

class OportunidadesViewModel : ViewModel() {

    private val _estado = MutableStateFlow(OportunidadesUiState())
    val estado: StateFlow<OportunidadesUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    // Cada carga ganha um número; só a mais recente pode mexer na tela
    private var numeroDaCarga = 0

    /** Busca as oportunidades com o filtro que estiver escolhido no momento. */
    fun carregar(manual: Boolean = false) {
        val minhaCarga = ++numeroDaCarga
        tarefa?.cancel()

        val filtro = _estado.value.filtro

        tarefa = viewModelScope.launch {
            _estado.update {
                when {
                    it.oportunidades == null -> it.copy(carregando = true, erro = null)
                    manual -> it.copy(atualizando = true, erro = null)
                    else -> it.copy(erro = null)
                }
            }

            try {
                val lista = OportunidadesApiClient.api.listar(tipo = filtro.tipoApi, area = null)

                if (minhaCarga == numeroDaCarga) {
                    _estado.update { it.copy(oportunidades = lista) }
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

    fun selecionarFiltro(filtro: FiltroTipo) {
        if (filtro == _estado.value.filtro) return

        _estado.update { it.copy(filtro = filtro) }
        carregar()
    }
}
