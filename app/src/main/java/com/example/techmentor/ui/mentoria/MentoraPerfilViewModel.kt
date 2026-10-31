package com.example.techmentor.ui.mentoria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.HorarioResponse
import com.example.techmentor.data.MentoraResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException


data class MentoraPerfilUiState(
    val carregando: Boolean = true,
    // true enquanto o "puxar para atualizar" está buscando dados novos
    val atualizando: Boolean = false,
    val erro: String? = null,
    val mentora: MentoraResponse? = null,
    // só os horários que ainda dá para agendar; null = não deu para saber (a tela não afirma nada)
    val horarios: List<HorarioResponse>? = null
)


class MentoraPerfilViewModel : ViewModel() {

    private val _estado = MutableStateFlow(MentoraPerfilUiState())
    val estado: StateFlow<MentoraPerfilUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    fun carregar(mentoraId: Long, manual: Boolean = false) {
        // Evita duas cargas ao mesmo tempo
        if (tarefa?.isActive == true) return

        tarefa = viewModelScope.launch {
            // O "carregando" só aparece na primeira vez; depois atualiza por baixo dos panos
            _estado.update {
                when {
                    it.mentora == null -> it.copy(carregando = true, erro = null)
                    manual -> it.copy(atualizando = true)
                    else -> it
                }
            }

            try {
                val api = ApiClient.api

                coroutineScope {
                    // Só a mentora é obrigatória: se os horários falharem, o perfil abre sem eles
                    val mentoraPedido = async { api.obterMentora(mentoraId) }
                    val horariosPedido = async { runCatching { api.listarHorarios(mentoraId) }.getOrNull() }

                    val mentora = mentoraPedido.await()
                    val horarios = horariosPedido.await()

                    _estado.update {
                        it.copy(
                            carregando = false,
                            erro = null,
                            mentora = mentora,
                            horarios = if (horarios != null) horariosDisponiveis(horarios) else it.horarios
                        )
                    }
                }
            } catch (e: HttpException) {
                val mensagem = if (e.code() == 404) "Mentora não encontrada." else ERRO_CONEXAO
                _estado.update { it.copy(carregando = false, erro = mensagem) }
            } catch (e: IOException) {
                _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
            } finally {
                _estado.update { it.copy(atualizando = false) }
            }
        }
    }
}
