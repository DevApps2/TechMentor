package com.example.techmentor.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.Sessao
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class AdminPainelUiState(
    val carregando: Boolean = true,
    // true enquanto o "puxar para atualizar" está buscando dados novos
    val atualizando: Boolean = false,
    val erro: String? = null,
    val sessaoExpirada: Boolean = false,
    val nome: String? = null,
    // null = não deu para carregar esse número (a tela mostra "—")
    val denunciasAbertas: Int? = null,
    val denunciasEmAnalise: Int? = null,
    val oportunidades: Int? = null,
    val mentoras: Int? = null,
    val alunas: Int? = null
)


class AdminPainelViewModel : ViewModel() {

    private val _estado = MutableStateFlow(AdminPainelUiState())
    val estado: StateFlow<AdminPainelUiState> = _estado.asStateFlow()

    private var tarefa: Job? = null

    fun carregar(manual: Boolean = false) {
        // Evita duas cargas ao mesmo tempo
        if (tarefa?.isActive == true) return

        val adminId = Sessao.usuarioId
        if (adminId == null) {
            _estado.update { it.copy(carregando = false, sessaoExpirada = true) }
            return
        }

        tarefa = viewModelScope.launch {
            // O "carregando" só aparece na primeira vez; depois atualiza por baixo dos panos
            _estado.update {
                when {
                    !it.carregou() -> it.copy(carregando = true, erro = null)
                    manual -> it.copy(atualizando = true)
                    else -> it
                }
            }

            coroutineScope {
                val perfil = async { chamar { ApiClient.api.obterUsuario(adminId) } }
                val denuncias = async { chamar { ApiClient.adminApi.listarDenuncias() } }
                val oportunidades = async { chamar { ApiClient.adminApi.listarOportunidades() } }
                val mentoras = async { chamar { ApiClient.api.listarMentoras(null, null) } }
                val usuarios = async { chamar { ApiClient.adminApi.listarUsuarios() } }

                val resultadoPerfil = perfil.await()
                val resultadoDenuncias = denuncias.await()
                val resultadoOportunidades = oportunidades.await()
                val resultadoMentoras = mentoras.await()
                val resultadoUsuarios = usuarios.await()

                // Se nada carregou, o problema é a conexão (ou a sessão): mostra o erro com "Tentar novamente"
                val falhas = listOf(
                    resultadoDenuncias, resultadoOportunidades, resultadoMentoras, resultadoUsuarios
                ).filterIsInstance<Resultado.Falha>()

                if (falhas.size == 4) {
                    val primeira = falhas.first()
                    _estado.update {
                        it.copy(
                            carregando = false,
                            atualizando = false,
                            erro = primeira.mensagem,
                            sessaoExpirada = primeira.sessaoExpirada
                        )
                    }
                } else {
                    val listaDeDenuncias = resultadoDenuncias.valorOuNull()
                    val listaDeUsuarios = resultadoUsuarios.valorOuNull()

                    _estado.update {
                        it.copy(
                            carregando = false,
                            atualizando = false,
                            erro = null,
                            nome = resultadoPerfil.valorOuNull()?.nome ?: it.nome,
                            denunciasAbertas = listaDeDenuncias?.count { d -> d.status == "ABERTA" },
                            denunciasEmAnalise = listaDeDenuncias?.count { d -> d.status == "EM_ANALISE" },
                            oportunidades = resultadoOportunidades.valorOuNull()?.size,
                            mentoras = resultadoMentoras.valorOuNull()?.size,
                            alunas = listaDeUsuarios?.count { u -> u.tipo == "ALUNA" }
                        )
                    }
                }
            }
        }
    }

    // Já carregou alguma vez? (sem isso, a tela mostra o esqueleto cinza)
    private fun AdminPainelUiState.carregou(): Boolean = !carregando && erro == null
}
