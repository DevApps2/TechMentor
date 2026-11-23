package com.example.techmentor.ui.admin

import com.example.techmentor.ui.avaliacao.diaMesEAno
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.AdminCandidatura
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.mentoria.CabecalhoVoltar
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily

/** As candidaturas de uma oportunidade: a admin vê quem se candidatou e muda a situação de cada uma. */
@Composable
fun AdminCandidaturasScreen(
    navController: NavController,
    oportunidadeId: Long,
    viewModel: AdminCandidaturasViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()
    val contexto = LocalContext.current

    // O PDF terminou de baixar: abre num leitor de PDF do aparelho, pelo FileProvider (o arquivo nunca sai do app)
    LaunchedEffect(estado.curriculoParaAbrir) {
        val arquivo = estado.curriculoParaAbrir ?: return@LaunchedEffect

        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", arquivo)
        val abrir = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            contexto.startActivity(abrir)
            viewModel.curriculoAberto()
        } catch (e: ActivityNotFoundException) {
            viewModel.curriculoAberto("Não há um leitor de PDF neste aparelho. Instale um (por exemplo, o Google Drive) para abrir o currículo.")
        }
    }

    EfeitoSessaoExpirada(expirada = estado.sessaoExpirada, navController = navController)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar(oportunidadeId)
    }

    // Com a tela aberta, atualiza sozinha de tempos em tempos
    AtualizarAutomaticamente { viewModel.carregar(oportunidadeId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        CabecalhoVoltar(
            titulo = "Candidaturas",
            onVoltar = { navController.popBackStack() }
        )

        Box(modifier = Modifier.weight(1f)) {

            val candidaturas = estado.candidaturas
            val erro = estado.erro

            when {
                candidaturas != null && candidaturas.isEmpty() -> SemCandidaturas()

                candidaturas != null -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    val erroAoAlterar = estado.erroAoAlterar
                    if (erroAoAlterar != null) {
                        item {
                            Text(
                                text = erroAoAlterar,
                                fontSize = 13.sp,
                                color = AuthColors.Error
                            )
                        }
                    }

                    val erroCurriculo = estado.erroCurriculo
                    if (erroCurriculo != null) {
                        item {
                            Text(
                                text = erroCurriculo,
                                fontSize = 13.sp,
                                color = AuthColors.Error
                            )
                        }
                    }

                    items(candidaturas, key = { it.id }) { candidatura ->
                        CartaoDeCandidatura(
                            candidatura = candidatura,
                            alterando = estado.alterandoId == candidatura.id,
                            baixandoCurriculo = estado.baixandoCurriculoId == candidatura.id,
                            onAlterar = { novo -> viewModel.alterarStatus(candidatura.id, novo) },
                            onAbrirCurriculo = { viewModel.baixarCurriculo(candidatura.id, contexto.cacheDir) }
                        )
                    }
                }

                erro != null -> EstadoErro(
                    mensagem = erro,
                    onTentarDeNovo = { viewModel.carregar(oportunidadeId) }
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EsqueletoBloco(altura = 110.dp)
                    EsqueletoBloco(altura = 110.dp)
                    EsqueletoBloco(altura = 110.dp)
                }
            }
        }

        AdminBottomBar(
            navController = navController,
            rotaAtual = "admin/oportunidades"
        )
    }
}


@Composable
private fun CartaoDeCandidatura(
    candidatura: AdminCandidatura,
    alterando: Boolean,
    baixandoCurriculo: Boolean,
    onAlterar: (String) -> Unit,
    onAbrirCurriculo: () -> Unit
) {

    val forma = RoundedCornerShape(17.dp)
    val estilo = estiloDaCandidatura(candidatura.status)

    // As outras situações possíveis (a atual já aparece na etiqueta)
    val acoes = listOf(
        Triple("EM_ANALISE", "Em análise", Color(0xFFB45309)),
        Triple("APROVADA", "Aprovar", Color(0xFF15803D)),
        Triple("RECUSADA", "Recusar", Color(0xFFB91C1C))
    ).filter { it.first != candidatura.status }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(16.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = candidatura.usuarioNome,
                    fontFamily = ManropeFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MentoriaCores.TextoEscuro
                )

                Text(
                    text = candidatura.usuarioEmail,
                    fontSize = 12.sp,
                    color = MentoriaCores.TextoMudo
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(estilo.fundo)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = estilo.rotulo,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = estilo.cor
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Enviada em ${dataDaCandidatura(candidatura.criadaEm)}",
            fontSize = 11.sp,
            color = Color(0xFF8A8691)
        )

        DadosDoFormulario(
            candidatura = candidatura,
            baixandoCurriculo = baixandoCurriculo,
            onAbrirCurriculo = onAbrirCurriculo
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            acoes.forEach { (status, rotulo, cor) ->
                AcaoChip(
                    texto = if (alterando) "..." else rotulo,
                    cor = cor,
                    habilitado = !alterando,
                    onClick = { onAlterar(status) }
                )
            }
        }
    }
}

/** O que a candidata preencheu no formulário e o botão do currículo. Não aparece em eventos nem em candidaturas antigas. */
@Composable
private fun DadosDoFormulario(
    candidatura: AdminCandidatura,
    baixandoCurriculo: Boolean,
    onAbrirCurriculo: () -> Unit
) {

    val linhas = listOfNotNull(
        candidatura.contatoNome?.let { "Nome" to it },
        candidatura.contatoEmail?.let { "E-mail" to it },
        candidatura.telefone?.let { "Telefone" to it },
        candidatura.cidade?.let { "Cidade" to it }
    )
    val link = candidatura.linkProfissional
    val mensagem = candidatura.mensagem

    if (linhas.isEmpty() && link == null && mensagem == null && !candidatura.temCurriculo) return

    val uri = LocalUriHandler.current

    Spacer(modifier = Modifier.height(12.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F6FC))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        linhas.forEach { (rotulo, valor) ->
            Text(
                text = "$rotulo: $valor",
                fontSize = 12.sp,
                color = MentoriaCores.TextoEscuro
            )
        }

        if (link != null) {
            Text(
                text = "Link: $link",
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button) {
                        // só abre endereços web; qualquer outra coisa fica só como texto
                        if (link.startsWith("http://", ignoreCase = true) || link.startsWith("https://", ignoreCase = true)) {
                            runCatching { uri.openUri(link) }
                        }
                    },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MentoriaCores.Roxo
            )
        }

        if (mensagem != null) {
            Text(
                text = "“$mensagem”",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MentoriaCores.TextoMudo
            )
        }

        if (candidatura.temCurriculo) {
            AcaoChip(
                texto = if (baixandoCurriculo) "Baixando..." else "Abrir currículo (PDF)",
                cor = MentoriaCores.Roxo,
                habilitado = !baixandoCurriculo,
                onClick = onAbrirCurriculo
            )
        }
    }
}

@Composable
private fun AcaoChip(
    texto: String,
    cor: Color,
    habilitado: Boolean,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(50)

    Box(
        modifier = Modifier
            .clip(forma)
            .border(1.dp, cor, forma)
            .clickable(enabled = habilitado, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = cor
        )
    }
}

@Composable
private fun SemCandidaturas() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Ninguém se candidatou ainda",
            fontFamily = ManropeFamily,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MentoriaCores.TextoEscuro
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Quando uma usuária se candidatar ou se inscrever, ela aparece aqui.",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            color = MentoriaCores.TextoMudo
        )
    }
}

private data class EstiloDaCandidatura(val rotulo: String, val cor: Color, val fundo: Color)

private fun estiloDaCandidatura(status: String): EstiloDaCandidatura = when (status) {
    "ENVIADA" -> EstiloDaCandidatura("Enviada", Color(0xFF6F6A76), Color(0xFFF1F0F4))
    "EM_ANALISE" -> EstiloDaCandidatura("Em análise", Color(0xFFB45309), Color(0xFFFEF3C7))
    "APROVADA" -> EstiloDaCandidatura("Aprovada", Color(0xFF15803D), Color(0xFFDCFCE7))
    "RECUSADA" -> EstiloDaCandidatura("Recusada", Color(0xFFB91C1C), Color(0xFFFEE2E2))
    else -> EstiloDaCandidatura(status, Color(0xFF6F6A76), Color(0xFFF1F0F4))
}

/** "2026-11-07T22:30:00" -> "07/11/2026". Se vier num formato inesperado, devolve como chegou. */
private fun dataDaCandidatura(iso: String?): String {
    return diaMesEAno(iso).ifEmpty { iso ?: "-" }
}
