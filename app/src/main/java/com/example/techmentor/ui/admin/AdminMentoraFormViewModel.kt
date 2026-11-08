package com.example.techmentor.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.AdminAreaRequest
import com.example.techmentor.data.AdminMentoraCadastroRequest
import com.example.techmentor.data.AdminMentoraPerfilRequest
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.AreaResponse
import com.example.techmentor.data.MentoraResponse
import com.example.techmentor.data.Sessao
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class AdminMentoraFormUiState(
    // falha ao carregar as áreas (e a mentora, no modo de edição)
    val erro: String? = null,
    val sessaoExpirada: Boolean = false,
    // null = ainda não carregou
    val areas: List<AreaResponse>? = null,
    // a mentora que está sendo editada; null no cadastro
    val original: MentoraResponse? = null,
    val areasSelecionadas: Set<Long> = emptySet(),
    val salvando: Boolean = false,
    val excluindo: Boolean = false,
    val erroAoSalvar: String? = null,
    // true quando cadastrou, salvou ou excluiu: a tela volta para a lista
    val concluido: Boolean = false,
    val criandoArea: Boolean = false,
    val erroDaArea: String? = null
)


class AdminMentoraFormViewModel : ViewModel() {

    private val _estado = MutableStateFlow(AdminMentoraFormUiState())
    val estado: StateFlow<AdminMentoraFormUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    /** Busca as áreas (para os botões de escolha) e, na edição, a mentora (id da mentora, não da conta). */
    fun carregar(mentoraId: Long?) {
        if (tarefa?.isActive == true) return
        // Já carregou: não refaz (senão apagaria o que a pessoa já marcou)
        if (_estado.value.areas != null && (mentoraId == null || _estado.value.original != null)) return

        tarefa = viewModelScope.launch {
            _estado.update { it.copy(erro = null) }

            coroutineScope {
                val areasPedido = async { chamar { ApiClient.api.listarAreas() } }
                val mentoraPedido = async {
                    if (mentoraId == null) null else chamar { ApiClient.api.obterMentora(mentoraId) }
                }

                val resultadoAreas = areasPedido.await()
                val resultadoMentora = mentoraPedido.await()

                when {
                    resultadoAreas is Resultado.Falha -> _estado.update { it.copy(erro = resultadoAreas.mensagem) }

                    resultadoMentora is Resultado.Falha -> _estado.update {
                        it.copy(
                            erro = if (resultadoMentora.codigo == 404) "Mentora não encontrada." else resultadoMentora.mensagem
                        )
                    }

                    resultadoAreas is Resultado.Ok -> {
                        val areas = resultadoAreas.valor
                        val mentora = resultadoMentora?.valorOuNull()

                        // A API devolve as áreas da mentora pelo nome; os botões trabalham com o ID
                        val nomes = mentora?.areas.orEmpty()
                        val marcadas = areas.filter { a -> a.nome in nomes }.map { a -> a.id }.toSet()

                        _estado.update {
                            it.copy(areas = areas, original = mentora, areasSelecionadas = marcadas)
                        }
                    }
                }
            }
        }
    }

    /** Marca ou desmarca uma área. */
    fun alternarArea(areaId: Long) {
        _estado.update {
            val novas = if (areaId in it.areasSelecionadas) it.areasSelecionadas - areaId else it.areasSelecionadas + areaId
            it.copy(areasSelecionadas = novas)
        }
    }

    /** Cria uma área de atuação nova e já deixa marcada. */
    fun criarArea(nome: String) {
        if (_estado.value.criandoArea) return

        val adminId = Sessao.usuarioId
        if (adminId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        val nomeLimpo = nome.trim()
        if (nomeLimpo.isEmpty()) {
            _estado.update { it.copy(erroDaArea = "Digite o nome da área.") }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(criandoArea = true, erroDaArea = null) }

            when (val resultado = chamar { ApiClient.adminApi.criarArea(AdminAreaRequest(nomeLimpo)) }) {
                is Resultado.Ok -> _estado.update {
                    val nova = resultado.valor
                    it.copy(
                        criandoArea = false,
                        areas = (it.areas.orEmpty() + nova).sortedBy { a -> a.nome },
                        areasSelecionadas = it.areasSelecionadas + nova.id
                    )
                }

                is Resultado.Falha -> _estado.update {
                    it.copy(
                        criandoArea = false,
                        erroDaArea = resultado.mensagem,
                        sessaoExpirada = resultado.sessaoExpirada
                    )
                }
            }
        }
    }

    /** Cria a conta (tipo MENTORA) e o perfil de uma vez. */
    fun cadastrar(
        nome: String,
        email: String,
        senha: String,
        cargo: String,
        empresa: String,
        anos: String,
        bio: String
    ) {
        if (_estado.value.salvando) return

        val adminId = Sessao.usuarioId
        if (adminId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        if (nome.isBlank()) return recusar("Informe o nome da mentora.")
        if (!email.contains("@") || email.trim().length < 5) return recusar("Informe um e-mail válido.")
        if (senha.length < 8) return recusar("A senha provisória deve ter pelo menos 8 caracteres.")
        val anosDeExperiencia = lerAnos(anos) ?: return

        val pedido = AdminMentoraCadastroRequest(
            nome = nome.trim(),
            email = email.trim(),
            senha = senha,
            bio = bio.vazioParaNulo(),
            cargo = cargo.vazioParaNulo(),
            empresa = empresa.vazioParaNulo(),
            anosExperiencia = anosDeExperiencia.valor,
            areasIds = _estado.value.areasSelecionadas.toList()
        )

        viewModelScope.launch {
            _estado.update { it.copy(salvando = true, erroAoSalvar = null) }
            terminar(chamar { ApiClient.adminApi.cadastrarMentora(pedido) })
        }
    }

    /** Atualiza só o perfil (cargo, empresa, anos, bio e áreas) de uma mentora que já existe. */
    fun atualizar(
        mentoraId: Long,
        cargo: String,
        empresa: String,
        anos: String,
        bio: String
    ) {
        if (_estado.value.salvando || _estado.value.excluindo) return

        val adminId = Sessao.usuarioId
        if (adminId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        val anosDeExperiencia = lerAnos(anos) ?: return

        val pedido = AdminMentoraPerfilRequest(
            bio = bio.vazioParaNulo(),
            cargo = cargo.vazioParaNulo(),
            empresa = empresa.vazioParaNulo(),
            anosExperiencia = anosDeExperiencia.valor,
            areasIds = _estado.value.areasSelecionadas.toList()
        )

        viewModelScope.launch {
            _estado.update { it.copy(salvando = true, erroAoSalvar = null) }
            terminar(chamar { ApiClient.adminApi.atualizarMentora(mentoraId, pedido) })
        }
    }

    /** Exclui o perfil de mentora (a conta de acesso continua). A API recusa se ela já tem mentorias agendadas. */
    fun excluir(mentoraId: Long) {
        if (_estado.value.salvando || _estado.value.excluindo) return

        val adminId = Sessao.usuarioId
        if (adminId == null) {
            _estado.update { it.copy(sessaoExpirada = true) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(excluindo = true, erroAoSalvar = null) }

            when (val resultado = chamarSemCorpo { ApiClient.adminApi.excluirMentora(mentoraId) }) {
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

    private fun terminar(resultado: Resultado<MentoraResponse>) {
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

    /** Anos de experiência são opcionais; se vierem preenchidos têm que ser um número. */
    private fun lerAnos(texto: String): Anos? {
        if (texto.isBlank()) return Anos(null)

        val numero = texto.trim().toIntOrNull()
        if (numero == null || numero < 0) {
            recusar("Anos de experiência deve ser um número.")
            return null
        }
        return Anos(numero)
    }

    // Caixinha para diferenciar "campo vazio" (valor nulo, tudo bem) de "campo inválido" (lerAnos devolve null)
    private class Anos(val valor: Int?)

    private fun recusar(mensagem: String) {
        _estado.update { it.copy(erroAoSalvar = mensagem) }
    }
}
