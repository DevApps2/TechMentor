package com.example.techmentor.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.AdminApi
import com.example.techmentor.data.AdminConviteParceiraRequest
import com.example.techmentor.data.AdminHistoricoParceiraResponse
import com.example.techmentor.data.AdminMotivoParceiraRequest
import com.example.techmentor.data.AdminParceiraResponse
import com.example.techmentor.data.AdminPedidoParceriaResponse
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
import retrofit2.Response
import java.io.IOException

data class AdminParceirasUiState(
    val carregando: Boolean = true,
    val atualizando: Boolean = false,
    val carregandoHistorico: Boolean = false,
    val enviando: Boolean = false,
    val filtro: String? = null,
    val parceiras: List<AdminParceiraResponse> = emptyList(),
    // pedidos que chegaram pelo formulário do site e esperam a administradora
    val pedidos: List<AdminPedidoParceriaResponse> = emptyList(),
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
            val primeira = _estado.value.parceiras.isEmpty() && _estado.value.pedidos.isEmpty()
            _estado.update { it.copy(carregando = primeira, atualizando = !primeira, filtro = filtro, erro = null) }
            try {
                // "PEDIDOS" não é uma situação da parceira: só mostra a fila de pedidos
                val parceiras = if (filtro == FILTRO_PEDIDOS) emptyList() else ApiClient.adminApi.listarParceiras(filtro)
                val pedidos = ApiClient.adminApi.listarPedidosDeParceria("PENDENTE")
                _estado.update { it.copy(carregando = false, atualizando = false, parceiras = parceiras, pedidos = pedidos, filtro = filtro, erro = null) }
            } catch (e: HttpException) {
                _estado.update { it.copy(carregando = false, atualizando = false, erro = mensagemDaApi(e)) }
            } catch (e: IOException) {
                _estado.update { it.copy(carregando = false, atualizando = false, erro = ERRO_CONEXAO) }
            }
        }
    }

    // ---- Pedidos vindos do site: a administradora só revisa e decide ----

    fun aprovarPedido(id: Long) = executar("Pedido aprovado; o convite foi enviado por e-mail.") {
        ApiClient.adminApi.aprovarPedidoDeParceria(id)
    }

    fun recusarPedido(id: Long, motivo: String) = executar("Pedido recusado; a instituição foi avisada por e-mail.") {
        ApiClient.adminApi.recusarPedidoDeParceria(id, AdminMotivoParceiraRequest(motivo))
    }

    fun apagarPedido(id: Long) = executar("Pedido apagado.") {
        ApiClient.adminApi.apagarPedidoDeParceria(id)
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
                val resposta = ApiClient.adminApi.chamada()
                if (resposta is Response<*> && !resposta.isSuccessful) {
                    _estado.update { it.copy(enviando = false, erroAcao = mensagemDoCorpo(resposta.errorBody()?.string())) }
                    return@launch
                }
                _estado.update { it.copy(enviando = false, sucesso = mensagem) }
                carregar(_estado.value.filtro)
            } catch (e: HttpException) {
                _estado.update { it.copy(enviando = false, erroAcao = mensagemDaApi(e)) }
            } catch (e: IOException) {
                _estado.update { it.copy(enviando = false, erroAcao = ERRO_CONEXAO) }
            }
        }
    }

    private fun mensagemDaApi(e: HttpException): String =
        mensagemDoCorpo(runCatching { e.response()?.errorBody()?.string() }.getOrNull())

    private fun mensagemDoCorpo(corpo: String?): String =
        runCatching { gson.fromJson(corpo, ErroResponse::class.java)?.erro }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: ERRO_CONEXAO
}

/** Filtro da tela de parceiras que mostra só os pedidos feitos pelo site. */
const val FILTRO_PEDIDOS = "PEDIDOS"
