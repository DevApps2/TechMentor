package com.example.techmentor.ui.mentora

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.AreaResponse
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.ErroResponse
import com.example.techmentor.data.MentoraRequest
import com.example.techmentor.data.MentoraResponse
import com.example.techmentor.data.Sessao
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException

data class MentoraPerfilFormUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val areasDisponiveis: List<AreaResponse> = emptyList(),
    // null = a mentora ainda não tem perfil (primeiro acesso)
    val perfilAtual: MentoraResponse? = null,
    val salvando: Boolean = false,
    val erroAoSalvar: String? = null,
    val salvo: Boolean = false,
    // true quando o usuário da sessão não existe mais (ex.: o banco foi reiniciado)
    val sessaoExpirada: Boolean = false,
    // foto de perfil do usuário (a mesma que as alunas veem na lista de mentoras)
    val fotoUrl: String? = null,
    val enviandoFoto: Boolean = false,
    val erroFoto: String? = null,
)

class MentoraPerfilFormViewModel : ViewModel() {

    private val _estado = MutableStateFlow(MentoraPerfilFormUiState())
    val estado: StateFlow<MentoraPerfilFormUiState> = _estado.asStateFlow()

    fun carregar() {
        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, erro = null) }

            try {
                val api = ApiClient.api

                coroutineScope {
                    val areasPedido = async { api.listarAreas() }
                    val usuarioPedido = async { api.obterUsuario(usuarioId) }

                    // 404 não é erro aqui: só quer dizer "ainda não preencheu"
                    val perfilPedido = async {
                        try {
                            api.minhaMentora(usuarioId)
                        } catch (e: HttpException) {
                            if (e.code() == 404) null else throw e
                        }
                    }

                    val areas = areasPedido.await()
                    val perfil = perfilPedido.await()
                    val usuario = usuarioPedido.await()

                    _estado.update {
                        it.copy(
                            carregando = false,
                            areasDisponiveis = areas,
                            perfilAtual = perfil,
                            fotoUrl = usuario.fotoUrl
                        )
                    }
                }
            } catch (e: HttpException) {
                val sessaoExpirada = e.code() == 401
                _estado.update {
                    it.copy(carregando = false, sessaoExpirada = sessaoExpirada, erro = ERRO_CONEXAO)
                }
            } catch (e: IOException) {
                _estado.update { it.copy(carregando = false, erro = ERRO_CONEXAO) }
            }
        }
    }

    /** Envia a foto escolhida na galeria (já reduzida) e atualiza o avatar. */
    fun enviarFoto(jpeg: ByteArray) {
        if (_estado.value.enviandoFoto) return

        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(enviandoFoto = true, erroFoto = null) }

            try {
                val parte = MultipartBody.Part.createFormData(
                    "foto",
                    "foto.jpg",
                    jpeg.toRequestBody("image/jpeg".toMediaType())
                )

                val atualizado = ApiClient.api.enviarFoto(usuarioId, parte)

                _estado.update { it.copy(enviandoFoto = false, fotoUrl = atualizado.fotoUrl) }
            } catch (e: HttpException) {
                _estado.update { it.copy(enviandoFoto = false, erroFoto = mensagemDaApi(e) ?: ERRO_CONEXAO) }
            } catch (e: IOException) {
                _estado.update { it.copy(enviandoFoto = false, erroFoto = ERRO_CONEXAO) }
            }
        }
    }

    /** Cria o perfil (primeiro acesso) ou atualiza o que já existe. */
    fun salvar(cargo: String, empresa: String, anos: Int?, bio: String, areasIds: List<Long>) {
        val atual = _estado.value
        if (atual.salvando) return

        val usuarioId = Sessao.usuarioId
        if (usuarioId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        val pedido = MentoraRequest(
            bio = bio,
            cargo = cargo,
            empresa = empresa,
            anosExperiencia = anos,
            areasIds = areasIds
        )

        viewModelScope.launch {
            _estado.update { it.copy(salvando = true, erroAoSalvar = null, salvo = false) }

            try {
                val api = ApiClient.api
                val perfil = atual.perfilAtual

                val resposta = if (perfil == null) {
                    api.criarPerfilDeMentora(usuarioId, pedido)
                } else {
                    api.atualizarPerfilDeMentora(usuarioId, perfil.id, pedido)
                }

                _estado.update { it.copy(salvando = false, perfilAtual = resposta, salvo = true) }
            } catch (e: HttpException) {
                val daApi = mensagemDaApi(e)
                val mensagem = when (e.code()) {
                    400, 403, 404, 409 -> daApi ?: "Não foi possível salvar. Tente novamente."
                    else -> ERRO_CONEXAO
                }
                _estado.update { it.copy(salvando = false, erroAoSalvar = mensagem) }
            } catch (e: IOException) {
                _estado.update { it.copy(salvando = false, erroAoSalvar = ERRO_CONEXAO) }
            }
        }
    }

    fun limparSalvo() {
        _estado.update { it.copy(salvo = false) }
    }
}

private fun mensagemDaApi(e: HttpException): String? =
    runCatching {
        Gson().fromJson(e.response()?.errorBody()?.string(), ErroResponse::class.java)?.erro
    }.getOrNull()