package com.example.techmentor.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.AdminDenuncia
import com.example.techmentor.data.UsuarioResponse
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.auth.OutlinedAuthButton
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.mentoria.CabecalhoVoltar
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.mentoria.SecaoCartao


@Composable
fun AdminDenunciaDetalheScreen(
    navController: NavController,
    denunciaId: Long,
    viewModel: AdminDenunciaDetalheViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    EfeitoSessaoExpirada(expirada = estado.sessaoExpirada, navController = navController)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar(denunciaId)
    }

    // Com a tela aberta, atualiza sozinha de tempos em tempos
    AtualizarAutomaticamente { viewModel.carregar(denunciaId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        CabecalhoVoltar(
            titulo = "Denúncia #$denunciaId",
            onVoltar = { navController.popBackStack() }
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {

            val denuncia = estado.denuncia

            when {
                denuncia != null -> DetalheConteudo(
                    denuncia = denuncia,
                    denunciante = estado.denunciante,
                    denunciado = estado.denunciado,
                    alterando = estado.alterando,
                    erroAoAlterar = estado.erroAoAlterar,
                    avisoDeSucesso = estado.avisoDeSucesso,
                    ocultando = estado.ocultando,
                    erroAoOcultar = estado.erroAoOcultar,
                    onDefinirOcultacao = { oculta -> viewModel.definirOcultacao(oculta) },
                    onAlterarStatus = { novo -> viewModel.alterarStatus(denunciaId, novo) }
                )

                estado.carregando -> DetalheCarregando()

                else -> EstadoErro(
                    mensagem = estado.erro ?: "Não foi possível carregar a denúncia.",
                    onTentarDeNovo = { viewModel.carregar(denunciaId) }
                )
            }
        }

        AdminBottomBar(
            navController = navController,
            rotaAtual = "admin/denuncias"
        )
    }
}


@Composable
private fun DetalheConteudo(
    denuncia: AdminDenuncia,
    denunciante: UsuarioResponse?,
    denunciado: UsuarioResponse?,
    alterando: Boolean,
    erroAoAlterar: String?,
    avisoDeSucesso: String?,
    ocultando: Boolean,
    erroAoOcultar: String?,
    onDefinirOcultacao: (Boolean) -> Unit,
    onAlterarStatus: (String) -> Unit
) {

    var confirmandoOcultar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            PilulaStatus(status = denuncia.status)

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = formatarDataHora(denuncia.criadaEm),
                fontSize = 12.sp,
                color = Color(0xFF8A8691)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SecaoCartao(titulo = nomeDoMotivo(denuncia.motivo)) {

            Text(
                text = denuncia.descricao?.takeIf { it.isNotBlank() } ?: "Sem descrição.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color(0xFF514C58)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SecaoCartao(titulo = "Envolvidas") {

            LinhaDeInformacao(
                rotulo = "Quem denunciou",
                valor = descreverUsuaria(denunciante, denuncia.denuncianteId)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LinhaDeInformacao(
                rotulo = "Quem foi denunciada",
                valor = if (denuncia.denunciadoId == null) {
                    "Denúncia geral (ninguém em específico)"
                } else {
                    descreverUsuaria(denunciado, denuncia.denunciadoId)
                }
            )
        }

        // Só aparece quando a denúncia é sobre o comentário de uma avaliação
        if (denuncia.avaliacaoId != null) {

            Spacer(modifier = Modifier.height(12.dp))

            val oculta = denuncia.avaliacaoOculta == true

            SecaoCartao(titulo = "Comentário denunciado") {

                Text(
                    text = "Nota: ${denuncia.avaliacaoNota ?: "-"} de 5",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MentoriaCores.TextoEscuro
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = denuncia.avaliacaoComentario?.takeIf { it.isNotBlank() } ?: "Sem comentário.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFF514C58)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (oculta) {
                        "Este comentário está escondido do público."
                    } else {
                        "Este comentário está visível para todo mundo."
                    },
                    fontSize = 12.sp,
                    color = Color(0xFF8A8691)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (oculta) {
                    OutlinedAuthButton(
                        text = if (ocultando) "Atualizando..." else "Mostrar comentário de novo",
                        onClick = { onDefinirOcultacao(false) }
                    )
                } else {
                    GradientButton(
                        text = if (ocultando) "Atualizando..." else "Esconder comentário",
                        onClick = { confirmandoOcultar = true }
                    )
                }

                if (erroAoOcultar != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    MensagemDeErro(texto = erroAoOcultar)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SecaoCartao(titulo = "Atualizar situação") {

            // Mostra só os botões das outras situações (a atual já aparece na etiqueta lá em cima)
            if (denuncia.status != "EM_ANALISE") {
                OutlinedAuthButton(
                    text = if (alterando) "Atualizando..." else "Colocar em análise",
                    onClick = { onAlterarStatus("EM_ANALISE") }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (denuncia.status != "RESOLVIDA") {
                GradientButton(
                    text = if (alterando) "Atualizando..." else "Marcar como resolvida",
                    onClick = { onAlterarStatus("RESOLVIDA") }
                )
            }

            if (denuncia.status == "RESOLVIDA") {
                OutlinedAuthButton(
                    text = if (alterando) "Atualizando..." else "Reabrir denúncia",
                    onClick = { onAlterarStatus("ABERTA") }
                )
            }

            if (avisoDeSucesso != null) {
                Spacer(modifier = Modifier.height(10.dp))
                MensagemDeSucesso(texto = avisoDeSucesso)
            }

            if (erroAoAlterar != null) {
                Spacer(modifier = Modifier.height(10.dp))
                MensagemDeErro(texto = erroAoAlterar)
            }
        }
    }

    if (confirmandoOcultar) {
        DialogoDeConfirmacao(
            titulo = "Esconder comentário?",
            texto = "O comentário deixa de aparecer para todo mundo. A nota continua valendo na média. "
                    + "Você pode mostrar de novo depois.",
            textoDoBotao = "Esconder",
            onConfirmar = {
                confirmandoOcultar = false
                onDefinirOcultacao(true)
            },
            onCancelar = { confirmandoOcultar = false }
        )
    }
}

/** "Maria Silva (maria@email.com)"; se a conta não foi encontrada, só o número. */
private fun descreverUsuaria(usuaria: UsuarioResponse?, id: Long): String =
    if (usuaria == null) "Usuária #$id (conta não encontrada)" else "${usuaria.nome} (${usuaria.email})"

@Composable
private fun DetalheCarregando() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        EsqueletoBloco(altura = 28.dp)
        EsqueletoBloco(altura = 110.dp)
        EsqueletoBloco(altura = 120.dp)
        EsqueletoBloco(altura = 140.dp)
    }
}