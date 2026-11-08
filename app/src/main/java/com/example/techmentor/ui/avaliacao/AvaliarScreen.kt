package com.example.techmentor.ui.avaliacao

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.AvaliacaoDaMentoriaResponse
import com.example.techmentor.data.AvaliacaoResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.AuthField
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.mentoria.CabecalhoVoltar
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.mentoria.SecaoCartao
import com.example.techmentor.ui.theme.ManropeFamily
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Tela de avaliar uma mentoria concluída, para a aluna (avalia a mentora) e para a mentora (avalia a aluna).
 * O backend diz se dá para avaliar; se não der, a tela mostra o motivo e o que já foi avaliado.
 */
@Composable
fun AvaliarScreen(
    navController: NavController,
    agendamentoId: Long,
    viewModel: AvaliarViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(agendamentoId) {
        viewModel.carregar(agendamentoId)
    }

    // Volta ao login se a sessão não existe mais (ex.: o banco foi reiniciado)
    LaunchedEffect(estado.sessaoExpirada) {
        if (estado.sessaoExpirada) {
            Sessao.sair()
            navController.navigate("login") {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
            .imePadding()
    ) {

        CabecalhoVoltar(
            titulo = "Avaliar mentoria",
            onVoltar = { navController.popBackStack() }
        )

        Box(modifier = Modifier.weight(1f)) {

            val situacao = estado.situacao

            when {
                estado.enviada -> Agradecimento(
                    euSouMentora = estado.euSouMentora,
                    onConcluir = { navController.popBackStack() }
                )

                situacao != null && !situacao.podeAvaliar -> SemAvaliar(
                    situacao = situacao,
                    outraPessoa = estado.outraPessoa,
                    enviandoDenuncia = estado.enviandoDenuncia,
                    denunciaEnviada = estado.denunciaEnviada,
                    erroDaDenuncia = estado.erroDaDenuncia,
                    onDenunciar = { motivo, detalhe -> viewModel.denunciarComentario(motivo, detalhe) },
                    onFecharDenuncia = { viewModel.limparErroDaDenuncia() },
                    onVoltar = { navController.popBackStack() }
                )

                situacao != null -> Formulario(
                    nota = estado.nota,
                    comentario = estado.comentario,
                    outraPessoa = estado.outraPessoa,
                    euSouMentora = estado.euSouMentora,
                    enviando = estado.enviando,
                    erro = estado.erroAoEnviar,
                    onNota = { viewModel.selecionarNota(it) },
                    onComentario = { viewModel.digitarComentario(it) },
                    onEnviar = { viewModel.enviar(agendamentoId) }
                )

                estado.carregando || estado.sessaoExpirada -> Carregando()

                else -> EstadoErro(
                    mensagem = estado.erro ?: "Não foi possível carregar esta mentoria.",
                    onTentarDeNovo = { viewModel.carregar(agendamentoId) }
                )
            }
        }
    }
}


// ======================================================
// FORMULÁRIO
// ======================================================

@Composable
private fun Formulario(
    nota: Int,
    comentario: String,
    outraPessoa: String,
    euSouMentora: Boolean,
    enviando: Boolean,
    erro: String?,
    onNota: (Int) -> Unit,
    onComentario: (String) -> Unit,
    onEnviar: () -> Unit
) {

    val foco = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Como foi sua mentoria com $outraPessoa?",
            fontFamily = ManropeFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MentoriaCores.TextoEscuro
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (euSouMentora) {
                "Seu feedback é privado: só a aluna e a equipe do TechMentor veem."
            } else {
                "Sua avaliação aparece no perfil da mentora, com o seu nome abreviado, e ajuda outras alunas a escolher."
            },
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            color = MentoriaCores.TextoMudo
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {

            for (estrela in 1..5) {

                Text(
                    text = "★",
                    modifier = Modifier
                        .clip(CircleShape)
                        .semantics {
                            contentDescription = if (estrela == 1) "1 estrela" else "$estrela estrelas"
                        }
                        .clickable(role = Role.Button) { onNota(estrela) }
                        .padding(horizontal = 4.dp),
                    fontSize = 44.sp,
                    color = if (estrela <= nota) MentoriaCores.Estrela else Color(0xFFD9D6DE)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = rotuloDaNota(nota),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (nota > 0) MentoriaCores.TextoEscuro else MentoriaCores.TextoMudo
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthField(
            label = "Comentário (opcional)",
            value = comentario,
            onValueChange = onComentario,
            placeholder = if (euSouMentora) {
                "Ex.: chegou com dúvidas claras e se dedicou"
            } else {
                "Ex.: ela me ajudou a montar um plano de estudos"
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            keyboardActions = KeyboardActions.Default,
            multilinha = true
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${comentario.length}/$LIMITE_COMENTARIO",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 10.sp,
            textAlign = TextAlign.End,
            color = Color(0xFF8A8691)
        )

        if (erro != null) {

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = erro,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = AuthColors.Error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        GradientButton(
            text = if (enviando) "Enviando..." else "Enviar avaliação",
            onClick = {
                foco.clearFocus()
                onEnviar()
            }
        )
    }
}


// ======================================================
// DEPOIS DE ENVIAR
// ======================================================

@Composable
private fun Agradecimento(
    euSouMentora: Boolean,
    onConcluir: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "★",
            fontSize = 56.sp,
            color = MentoriaCores.Estrela
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Obrigada pela avaliação!",
            fontFamily = ManropeFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MentoriaCores.TextoEscuro
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (euSouMentora) {
                "Seu feedback foi enviado para a aluna e para a equipe do TechMentor."
            } else {
                "Ela ajuda outras alunas a escolherem a mentora certa."
            },
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            color = MentoriaCores.TextoMudo
        )

        Spacer(modifier = Modifier.height(24.dp))

        GradientButton(
            text = "Concluir",
            onClick = onConcluir
        )
    }
}


// ======================================================
// QUANDO NÃO DÁ PARA AVALIAR (já avaliou, prazo vencido...)
// ======================================================

@Composable
private fun SemAvaliar(
    situacao: AvaliacaoDaMentoriaResponse,
    outraPessoa: String,
    enviandoDenuncia: Boolean,
    denunciaEnviada: Boolean,
    erroDaDenuncia: String?,
    onDenunciar: (motivo: String, detalhe: String) -> Unit,
    onFecharDenuncia: () -> Unit,
    onVoltar: () -> Unit
) {

    var denunciando by remember { mutableStateOf(false) }

    // a denúncia foi enviada: fecha a janelinha
    LaunchedEffect(denunciaEnviada) {
        if (denunciaEnviada) denunciando = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp)
    ) {

        Text(
            text = "Mentoria com $outraPessoa",
            fontFamily = ManropeFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MentoriaCores.TextoEscuro
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = situacao.motivoDeNaoPoder ?: "Esta mentoria não pode ser avaliada agora.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MentoriaCores.TextoMudo
        )

        situacao.minhaAvaliacao?.let {
            Spacer(modifier = Modifier.height(16.dp))
            CartaoDeAvaliacao(titulo = "Sua avaliação", avaliacao = it)
        }

        situacao.recebida?.let { recebida ->
            Spacer(modifier = Modifier.height(12.dp))
            CartaoDeAvaliacao(titulo = "O que $outraPessoa escreveu sobre você", avaliacao = recebida)

            // só dá para denunciar o que tem texto: a nota nunca é escondida
            if (!recebida.comentario.isNullOrBlank()) {

                Spacer(modifier = Modifier.height(4.dp))

                if (denunciaEnviada) {
                    Text(
                        text = "Denúncia enviada. Nossa equipe vai analisar este comentário.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MentoriaCores.TextoMudo
                    )
                } else {
                    Text(
                        text = "Denunciar este comentário",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AuthColors.Error,
                        modifier = Modifier
                            .clickable { denunciando = true }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        GradientButton(
            text = "Voltar",
            onClick = onVoltar
        )
    }

    if (denunciando) {
        DialogoDeDenunciaDeComentario(
            enviando = enviandoDenuncia,
            erro = erroDaDenuncia,
            onEnviar = onDenunciar,
            onCancelar = {
                denunciando = false
                onFecharDenuncia()
            }
        )
    }
}

@Composable
private fun CartaoDeAvaliacao(
    titulo: String,
    avaliacao: AvaliacaoResponse
) {

    SecaoCartao(titulo = titulo) {

        Text(
            text = estrelasEmTexto(avaliacao.nota),
            fontSize = 20.sp,
            color = MentoriaCores.Estrela
        )

        if (!avaliacao.comentario.isNullOrBlank()) {

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = avaliacao.comentario,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color(0xFF514C58)
            )
        }
    }
}

@Composable
private fun Carregando() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EsqueletoBloco(altura = 60.dp)
        EsqueletoBloco(altura = 70.dp)
        EsqueletoBloco(altura = 120.dp)
    }
}