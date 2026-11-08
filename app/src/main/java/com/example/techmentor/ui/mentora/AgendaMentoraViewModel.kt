package com.example.techmentor.ui.mentora

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.ErroResponse
import com.example.techmentor.data.HorarioRequest
import com.example.techmentor.data.HorarioResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.mentoria.horariosDisponiveis
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class AgendaUiState(
    val carregando: Boolean = true,
    val atualizando: Boolean = false,
    val erro: String? = null,
    val mentoraId: Long? = null,
    // horários livres e futuros, do mais cedo para o mais tarde
    val horarios: List<HorarioResponse> = emptyList(),
    // true quando a mentora ainda não preencheu o perfil
    val semPerfil: Boolean = false,
    val adicionando: Boolean = false,
    val removendoId: Long? = null,
    // falha ao adicionar ou remover (aparece em vermelho na tela)
    val erroAcao: String? = null,
    // true quando o usuário da sessão não existe mais (ex.: o banco foi reiniciado)
    val sessaoExpirada: Boolean = false
)

class AgendaMentoraViewModel : ViewModel() {

    private val _estado = MutableStateFlow(AgendaUiState())
    val estado: StateFlow<AgendaUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    fun carregar(manual: Boolean = false) {
        // Evita duas cargas ao mesmo tempo
        if (tarefa?.isActive == true) return

        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
            return
        }

        tarefa = viewModelScope.launch {
            // O "carregando" só aparece na primeira vez; depois atualiza por baixo dos panos
            _estado.update {
                when {
                    it.mentoraId == null -> it.copy(carregando = true, erro = null)
                    manual -> it.copy(atualizando = true)
                    else -> it
                }
            }

            try {
                val api = ApiClient.api

                // 404 = a mentora ainda não preencheu o perfil
                val mentora = try {
                    api.minhaMentora()
                } catch (e: HttpException) {
                    if (e.code() == 404) null else throw e
                }

                if (mentora == null) {
                    _estado.update { it.copy(semPerfil = true) }
                    return@launch
                }

                // A API devolve só os horários ainda não reservados
                val horarios = horariosDisponiveis(api.listarHorarios(mentora.id))

                _estado.update {
                    it.copy(mentoraId = mentora.id, horarios = horarios, semPerfil = false, erro = null)
                }
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    _estado.update { it.copy(sessaoExpirada = true) }
                } else {
                    _estado.update { it.copy(erro = ERRO_CONEXAO) }
                }
            } catch (e: IOException) {
                _estado.update { it.copy(erro = ERRO_CONEXAO) }
            } finally {
                _estado.update { it.copy(carregando = false, atualizando = false) }
            }
        }
    }

    /** Cria um horário livre. [inicio] e [fim] no formato "2026-11-10T14:00:00". */
    fun adicionar(inicio: String, fim: String, aoTerminar: () -> Unit) {
        val atual = _estado.value
        val mentoraId = atual.mentoraId ?: return
        if (atual.adicionando) return

        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(adicionando = true, erroAcao = null) }

            try {
                val criado = ApiClient.api.criarHorario(mentoraId, HorarioRequest(inicio, fim))

                _estado.update {
                    it.copy(adicionando = false, horarios = horariosDisponiveis(it.horarios + criado))
                }
                aoTerminar()
            } catch (e: HttpException) {
                _estado.update { it.copy(adicionando = false, erroAcao = mensagemDeErro(e)) }
            } catch (e: IOException) {
                _estado.update { it.copy(adicionando = false, erroAcao = ERRO_CONEXAO) }
            }
        }
    }

    fun remover(horarioId: Long) {
        if (_estado.value.removendoId != null) return

        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(removendoId = horarioId, erroAcao = null) }

            try {
                ApiClient.api.excluirHorario(horarioId)

                _estado.update {
                    it.copy(removendoId = null, horarios = it.horarios.filter { h -> h.id != horarioId })
                }
            } catch (e: HttpException) {
                _estado.update { it.copy(removendoId = null, erroAcao = mensagemDeErro(e)) }
            } catch (e: IOException) {
                _estado.update { it.copy(removendoId = null, erroAcao = ERRO_CONEXAO) }
            }
        }
    }

    fun limparErro() {
        _estado.update { it.copy(erroAcao = null) }
    }

    private fun mensagemDeErro(e: HttpException): String {
        val daApi = runCatching {
            Gson().fromJson(e.response()?.errorBody()?.string(), ErroResponse::class.java)?.erro
        }.getOrNull()

        return when (e.code()) {
            // 400 = horário no passado ou fim antes do início; 409 = sobrepõe outro horário ou já foi reservado
            400, 403, 404, 409 -> daApi ?: "Não foi possível concluir. Tente novamente."
            else -> ERRO_CONEXAO
        }
    }
}