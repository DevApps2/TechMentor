package com.example.techmentor.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.AdminOportunidade
import com.example.techmentor.data.AdminOportunidadeRequest
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.Sessao
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class AdminOportunidadeFormUiState(
    // falha ao carregar a oportunidade (só no modo de edição)
    val erro: String? = null,
    val sessaoExpirada: Boolean = false,
    // a oportunidade que está sendo editada; null no cadastro ou enquanto carrega
    val original: AdminOportunidade? = null,
    val salvando: Boolean = false,
    val excluindo: Boolean = false,
    val erroAoSalvar: String? = null,
    // true quando salvou ou excluiu: a tela volta para a lista
    val concluido: Boolean = false
)


class AdminOportunidadeFormViewModel : ViewModel() {

    private val _estado = MutableStateFlow(AdminOportunidadeFormUiState())
    val estado: StateFlow<AdminOportunidadeFormUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    /** No modo de edição, busca a oportunidade; no cadastro (id nulo) não há o que carregar. */
    fun carregar(oportunidadeId: Long?) {
        if (oportunidadeId == null) return
        if (tarefa?.isActive == true || _estado.value.original != null) return

        tarefa = viewModelScope.launch {
            _estado.update { it.copy(erro = null) }

            when (val resultado = chamar { ApiClient.adminApi.obterOportunidade(oportunidadeId) }) {
                is Resultado.Ok -> _estado.update { it.copy(original = resultado.valor) }

                is Resultado.Falha -> _estado.update {
                    it.copy(
                        erro = if (resultado.codigo == 404) "Oportunidade não encontrada." else resultado.mensagem
                    )
                }
            }
        }
    }

    /** Valida os campos e cadastra (id nulo) ou atualiza (id preenchido) a oportunidade. */
    fun salvar(
        oportunidadeId: Long?,
        tipo: String,
        titulo: String,
        organizacao: String,
        area: String,
        nivel: String,
        localidade: String,
        linkExterno: String,
        prazo: String,
        descricao: String,
        requisitos: String,
        beneficios: String
    ) {
        if (_estado.value.salvando || _estado.value.excluindo) return

        val adminId = Sessao.usuarioId
        if (adminId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        if (tipo.isBlank()) return recusar("Escolha o tipo da oportunidade.")
        if (titulo.isBlank()) return recusar("Informe o título da oportunidade.")

        val link = linkExterno.vazioParaNulo()
        if (link != null && !link.startsWith("http://") && !link.startsWith("https://")) {
            return recusar("O link deve começar com http:// ou https://")
        }

        // Prazo é opcional; se vier preenchido precisa ser uma data que existe e que ainda não passou
        var prazoIso: String? = null
        if (prazo.isNotBlank()) {
            prazoIso = dataParaIso(prazo) ?: return recusar("Data inválida. Use o formato dd/mm/aaaa.")
            if (prazoEncerrado(prazoIso)) return recusar("O prazo não pode estar no passado.")
        }

        val pedido = AdminOportunidadeRequest(
            tipo = tipo,
            titulo = titulo.trim(),
            organizacao = organizacao.vazioParaNulo(),
            descricao = descricao.vazioParaNulo(),
            requisitos = requisitos.vazioParaNulo(),
            localidade = localidade.vazioParaNulo(),
            linkExterno = link,
            prazoInscricao = prazoIso,
            beneficios = beneficios.vazioParaNulo(),
            area = area.vazioParaNulo(),
            nivel = nivel.vazioParaNulo()
        )

        viewModelScope.launch {
            _estado.update { it.copy(salvando = true, erroAoSalvar = null) }

            val resultado = chamar {
                if (oportunidadeId == null) {
                    ApiClient.adminApi.criarOportunidade(pedido)
                } else {
                    ApiClient.adminApi.atualizarOportunidade(oportunidadeId, pedido)
                }
            }

            when (resultado) {
                is Resultado.Ok -> _estado.update { it.copy(salvando = false, concluido = true) }

                is Resultado.Falha -> _estado.update {
                    it.copy(
                        salvando = false,
                        erroAoSalvar = resultado.mensagem,
                        sessaoExpirada = resultado.sessaoExpirada
                    )
                }
            }
        }
    }

    /** Desativa a oportunidade: ela some da lista dos alunos e da do admin. */
    fun excluir(oportunidadeId: Long) {
        if (_estado.value.salvando || _estado.value.excluindo) return

        val adminId = Sessao.usuarioId
        if (adminId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(excluindo = true, erroAoSalvar = null) }

            when (val resultado = chamarSemCorpo { ApiClient.adminApi.excluirOportunidade(oportunidadeId) }) {
                is Resultado.Ok -> _estado.update { it.copy(excluindo = false, concluido = true) }

                is Resultado.Falha -> _estado.update {
                    it.copy(
                        excluindo = false,
                        erroAoSalvar = resultado.mensagem,
                        sessaoExpirada = resultado.sessaoExpirada
                    )
                }
            }
        }
    }

    private fun recusar(mensagem: String) {
        _estado.update { it.copy(erroAoSalvar = mensagem) }
    }
}
