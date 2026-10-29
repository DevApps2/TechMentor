package com.example.techmentor.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.AgendamentoResponse
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.AtualizarUsuarioRequest
import com.example.techmentor.data.ErroResponse
import com.example.techmentor.data.MatriculaResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.data.UsuarioResponse
import com.google.gson.Gson
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

data class ProfileUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val usuario: UsuarioResponse? = null,
    val totalConquistas: Int = 0,
    val ultimoCursoConcluido: MatriculaResponse? = null,
    val historicoMentorias: List<AgendamentoResponse> = emptyList(),
    // true quando o usuário da sessão não existe mais (ex.: o banco foi reiniciado)
    val sessaoExpirada: Boolean = false,
    // edição do perfil
    val salvando: Boolean = false,
    val erroEdicao: String? = null
)

class ProfileViewModel : ViewModel() {

    private val _estado = MutableStateFlow(ProfileUiState())
    val estado: StateFlow<ProfileUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    fun carregar() {
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
                if (it.usuario == null) it.copy(carregando = true, erro = null) else it
            }

            try {
                val api = ApiClient.api

                coroutineScope {
                    // Só o usuário é obrigatório; o resto, se falhar, fica de fora
                    val usuarioPedido = async { api.obterUsuario(usuarioId) }
                    val conquistasPedido = async { runCatching { api.listarConquistasDoUsuario(usuarioId) }.getOrNull() }
                    val matriculasPedido = async { runCatching { api.listarMatriculas(usuarioId) }.getOrNull() }
                    val agendamentosPedido = async { runCatching { api.listarAgendamentos(usuarioId) }.getOrNull() }

                    val usuario = usuarioPedido.await()
                    val conquistas = conquistasPedido.await()
                    val matriculas = matriculasPedido.await()
                    val agendamentos = agendamentosPedido.await()

                    _estado.update {
                        it.copy(
                            carregando = false,
                            erro = null,
                            usuario = usuario,
                            totalConquistas = conquistas?.size ?: it.totalConquistas,
                            ultimoCursoConcluido = if (matriculas != null) {
                                // as datas da API são ISO, então comparar o texto já ordena por data
                                matriculas
                                    .filter { m -> m.concluidoEm != null }
                                    .maxByOrNull { m -> m.concluidoEm.orEmpty() }
                            } else {
                                it.ultimoCursoConcluido
                            },
                            historicoMentorias = agendamentos
                                ?.sortedByDescending { a -> a.inicio }
                                ?.take(5)
                                ?: it.historicoMentorias
                        )
                    }
                }
            } catch (e: HttpException) {
                if (e.code() == 404 || e.code() == 401) {
                    _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
                } else {
                    _estado.update { it.copy(carregando = false, erro = "Erro no servidor (${e.code()}).") }
                }
            } catch (e: IOException) {
                _estado.update { it.copy(carregando = false, erro = "Sem conexão com o servidor.") }
            }
        }
    }

    /**
     * Salva a edição do perfil. O e-mail não é editável aqui, então vai o mesmo de sempre
     * (a API exige o campo). Foto vazia limpa a foto; as áreas de interesse substituem as atuais.
     * [aoTerminar] é chamado (na tela) só quando o salvamento der certo.
     */
    fun salvar(
        nome: String,
        bio: String,
        fotoUrl: String,
        areasInteresse: List<String>,
        aoTerminar: () -> Unit
    ) {
        val atual = _estado.value.usuario ?: return
        val usuarioId = Sessao.usuarioId ?: return
        if (_estado.value.salvando) return

        if (nome.isBlank()) {
            _estado.update { it.copy(erroEdicao = "Informe seu nome.") }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(salvando = true, erroEdicao = null) }

            try {
                val atualizado = ApiClient.api.atualizarUsuario(
                    usuarioId,
                    AtualizarUsuarioRequest(
                        nome = nome.trim(),
                        email = atual.email,
                        fotoUrl = fotoUrl.trim().ifEmpty { null },
                        bio = bio.trim(),
                        areasInteresse = areasInteresse
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .distinct()
                    )
                )

                _estado.update { it.copy(usuario = atualizado, salvando = false) }
                aoTerminar()
            } catch (e: HttpException) {
                val mensagem = when (e.code()) {
                    400 -> mensagemDaApi(e) ?: "Confira os dados informados."
                    404 -> "Usuário não encontrado."
                    else -> "Erro no servidor (${e.code()})."
                }
                _estado.update { it.copy(salvando = false, erroEdicao = mensagem) }
            } catch (e: IOException) {
                _estado.update { it.copy(salvando = false, erroEdicao = "Sem conexão com o servidor.") }
            }
        }
    }

    fun limparErroEdicao() {
        _estado.update { it.copy(erroEdicao = null) }
    }
}

private fun mensagemDaApi(e: HttpException): String? =
    runCatching {
        Gson().fromJson(e.response()?.errorBody()?.string(), ErroResponse::class.java)?.erro
    }.getOrNull()