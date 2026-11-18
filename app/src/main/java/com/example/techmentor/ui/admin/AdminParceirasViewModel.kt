package com.example.techmentor.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.AdminApi
import com.example.techmentor.data.AdminConviteParceiraRequest
import com.example.techmentor.data.AdminHistoricoParceiraResponse
import com.example.techmentor.data.AdminMotivoParceiraRequest
import com.example.techmentor.data.AdminParceiraResponse
import com.example.techmentor.data.AdminVerificacaoParceiraRequest
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.ErroResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class AdminParceirasUiState(
    val carregando: Boolean = true,
    val atualizando: Boolean = false,
    val carregandoHistorico: Boolean = false,
    val enviando: Boolean = false,
    val filtro: String? = null,
    val parceiras: List<AdminParceiraResponse> = emptyList(),
    val historico: List<AdminHistoricoParceiraResponse> = emptyList(),
    val erro: String? = null,
    val erroAcao: String? = null,
    val sucesso: String? = null
)

class AdminParceirasViewModel : ViewModel() {
    private val _estado = MutableStateFlow(AdminParceirasUiState())
    val estado: StateFlow<AdminParceirasUiState> = _estado.asStateFlow()
    private val gson = Gson()

    fun carregar(filtro: String? = _estado.value.filtro) {
        viewModelScope.launch {
            _estado.update { it.copy(carregando = it.parceiras.isEmpty(), atualizando = it.parceiras.isNotEmpty(), filtro = filtro, erro = null) }
            try {
                val lista = ApiClient.adminApi.listarParceiras(filtro)
                _estado.update { it.copy(carregando = false, atualizando = false, parceiras = lista, filtro = filtro, erro = null) }
            } catch (e: HttpException) {
                _estado.update { it.copy(carregando = false, atualizando = false, erro = mensagemDaApi(e)) }
            } catch (e: IOException) {
                _estado.update { it.copy(carregando = false, atualizando = false, erro = ERRO_CONEXAO) }
            }
        }
    }

    fun convidar(dados: AdminConviteParceiraRequest) = executar("Convite registrado; o envio está sendo processado.") {
        ApiClient.adminApi.convidarParceira(dados)
    }

    fun reenviarConvite(id: Long) = executar("Reenvio do convite solicitado.") {
        ApiClient.adminApi.reenviarConviteDaParceira(id)
    }

    fun verificar(id: Long, dados: AdminVerificacaoParceiraRequest) = executar("Parceria verificada e ativada.") {
        ApiClient.adminApi.verificarParceira(id, dados)
    }

    fun suspender(id: Long, motivo: String) = executar("Conta de parceira suspensa.") {
        ApiClient.adminApi.suspenderParceira(id, AdminMotivoParceiraRequest(motivo))
    }

    fun reativar(id: Long) = executar("Conta de parceira reativada.") {
        ApiClient.adminApi.reativarParceira(id)
    }

    fun carregarHistorico(id: Long) {
        viewModelScope.launch {
            _estado.update { it.copy(carregandoHistorico = true, historico = emptyList(), erroAcao = null) }
            try {
                val registros = ApiClient.adminApi.historicoDaParceira(id)
                _estado.update { it.copy(carregandoHistorico = false, historico = registros) }
            } catch (e: HttpException) {
                _estado.update { it.copy(carregandoHistorico = false, erroAcao = mensagemDaApi(e)) }
            } catch (e: IOException) {
                _estado.update { it.copy(carregandoHistorico = false, erroAcao = ERRO_CONEXAO) }
            }
        }
    }

    fun limparMensagens() {
        _estado.update { it.copy(erroAcao = null, sucesso = null) }
    }

    private fun executar(mensagem: String, chamada: suspend AdminApi.() -> Any) {
        if (_estado.value.enviando) return
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, erroAcao = null, sucesso = null) }
            try {
                ApiClient.adminApi.chamada()
                _estado.update { it.copy(enviando = false, sucesso = mensagem) }
                carregar(_estado.value.filtro)
            } catch (e: HttpException) {
                _estado.update { it.copy(enviando = false, erroAcao = mensagemDaApi(e)) }
            } catch (e: IOException) {
                _estado.update { it.copy(enviando = false, erroAcao = ERRO_CONEXAO) }
            }
        }
    }

    private fun mensagemDaApi(e: HttpException): String {
        val corpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        return runCatching { gson.fromJson(corpo, ErroResponse::class.java)?.erro }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: ERRO_CONEXAO
    }
}
