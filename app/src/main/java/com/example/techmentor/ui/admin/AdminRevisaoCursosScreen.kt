package com.example.techmentor.ui.admin

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.AdminCursoResponse
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.mentoria.CabecalhoVoltar
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro

private val RevisaoFundo = Color(0xFFF8F8FB)
private val RevisaoTexto = Color(0xFF20202A)
private val RevisaoCinza = Color(0xFF77717E)

private enum class DecisaoCurso { APROVAR, DEVOLVER, RECUSAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRevisaoCursosScreen(
    navController: NavController,
    cursoId: Long? = null,
    viewModel: AdminRevisaoCursosViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    var selecionado by remember { mutableStateOf<AdminCursoResponse?>(null) }
    var decisao by remember { mutableStateOf<DecisaoCurso?>(null) }

    EfeitoSessaoExpirada(expirada = estado.sessaoExpirada, navController = navController)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.carregar() }
    AtualizarAutomaticamente { viewModel.carregar() }
    LaunchedEffect(estado.cursos, cursoId) {
        val alvo = cursoId?.let { id -> estado.cursos?.firstOrNull { it.id == id } }
        if (alvo != null) selecionado = alvo
    }

    Column(Modifier.fillMaxSize().background(RevisaoFundo)) {
        CabecalhoVoltar(titulo = "Revisar cursos", onVoltar = { navController.popBackStack() })
        Box(Modifier.weight(1f)) {
            when {
                estado.carregando && estado.cursos == null -> RevisaoCarregando()
                estado.erro != null && estado.cursos == null -> EstadoErro(
                    mensagem = estado.erro!!,
                    onTentarDeNovo = { viewModel.carregar() }
                )
                else -> PullToRefreshBox(
                    isRefreshing = estado.atualizando,
                    onRefresh = { viewModel.carregar(manual = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    val cursos = estado.cursos.orEmpty()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        item {
                            Text(
                                "Analise o conteúdo enviado pelas instituições antes de publicá-lo para as alunas.",
                                color = RevisaoCinza,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "${cursos.size} ${if (cursos.size == 1) "curso aguardando" else "cursos aguardando"}",
                                color = RevisaoTexto,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            estado.mensagem?.let {
                                Spacer(Modifier.height(9.dp))
                                MensagemRevisao(it, sucesso = true)
                            }
                            estado.erro?.let {
                                Spacer(Modifier.height(9.dp))
                                MensagemRevisao(it, sucesso = false)
                            }
                            estado.erroAcao?.let {
                                Spacer(Modifier.height(9.dp))
                                MensagemRevisao(it, sucesso = false)
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        if (cursos.isEmpty()) {
                            item {
                                Column(
                                    Modifier.fillMaxWidth().padding(vertical = 46.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Nenhum curso aguardando revisão.", color = RevisaoTexto, fontWeight = FontWeight.SemiBold)
                                    Text("Quando uma parceira enviar um curso, ele aparecerá aqui.", color = RevisaoCinza, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(cursos, key = { it.id }) { curso ->
                                CartaoCursoEmRevisao(curso) { selecionado = curso }
                            }
                        }
                    }
                }
            }
        }
        AdminBottomBar(navController = navController, rotaAtual = "admin/cursos-revisao")
    }

    if (decisao == null) {
        selecionado?.let { curso ->
            DialogoDetalheCurso(curso = curso, enviando = estado.enviando,
                aoAprovar = { decisao = DecisaoCurso.APROVAR },
                aoDevolver = { decisao = DecisaoCurso.DEVOLVER },
                aoRecusar = { decisao = DecisaoCurso.RECUSAR },
                aoFechar = { selecionado = null; viewModel.limparMensagens() })
        }
    }

    decisao?.let { acao ->
        selecionado?.let { curso ->
            DialogoDecisaoCurso(
                decisao = acao,
                curso = curso,
                enviando = estado.enviando,
                erro = estado.erroAcao,
                aoConfirmar = { comentario ->
                    when (acao) {
                        DecisaoCurso.APROVAR -> viewModel.aprovar(curso.id)
                        DecisaoCurso.DEVOLVER -> viewModel.devolver(curso.id, comentario)
                        DecisaoCurso.RECUSAR -> viewModel.recusar(curso.id, comentario)
                    }
                    selecionado = null
                    decisao = null
                },
                aoCancelar = { if (!estado.enviando) decisao = null }
            )
        }
    }
}

@Composable
private fun CartaoCursoEmRevisao(curso: AdminCursoResponse, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Color.White)
            .border(1.dp, Color(0xFFE9E7EE), RoundedCornerShape(17.dp))
            .clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF0EAFF)),
            contentAlignment = Alignment.Center
        ) {
            Text("${curso.titulo.take(1).uppercase()}", color = AuthColors.Purple, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(curso.titulo, color = RevisaoTexto, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(curso.instituicao ?: "Parceira", color = RevisaoCinza, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${curso.totalAulas} aulas · ${curso.modulos.size} módulos", color = RevisaoCinza, fontSize = 10.sp)
        }
        Text("Analisar", color = AuthColors.Purple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DialogoDetalheCurso(
    curso: AdminCursoResponse,
    enviando: Boolean,
    aoAprovar: () -> Unit,
    aoDevolver: () -> Unit,
    aoRecusar: () -> Unit,
    aoFechar: () -> Unit
) {
    val contexto = LocalContext.current
    AlertDialog(
        onDismissRequest = { if (!enviando) aoFechar() },
        title = { Text(curso.titulo, color = RevisaoTexto, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${curso.instituicao ?: "Parceira"} · ${textoLegivel(curso.categoria)} · ${textoLegivel(curso.nivel)}", color = RevisaoCinza, fontSize = 11.sp)
                Text(curso.descricao.orEmpty().ifBlank { "Sem descrição informada." }, color = RevisaoTexto, fontSize = 13.sp)
                Text("${curso.modulos.size} módulos · ${curso.totalAulas} aulas · ${curso.duracaoTotalMinutos} min · ${curso.xpTotal} XP", color = RevisaoCinza, fontSize = 11.sp)
                curso.modulos.sortedBy { it.ordem }.forEachIndexed { indice, modulo ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF7F5FA)).padding(10.dp)) {
                        Text("${indice + 1}. ${modulo.titulo}", color = RevisaoTexto, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        if (modulo.aulas.isEmpty()) Text("Módulo sem aulas", color = Color(0xFFB42336), fontSize = 10.sp)
                        modulo.aulas.sortedBy { it.ordem }.forEach { aula ->
                            Text("• ${aula.titulo} · ${aula.duracaoMinutos} min", color = RevisaoTexto, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                            aula.conteudoUrl?.takeIf { it.isNotBlank() }?.let { url ->
                                TextButton(onClick = {
                                    runCatching { contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                }) { Text("Abrir vídeo da aula", fontSize = 10.sp) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = aoAprovar, enabled = !enviando, modifier = Modifier.fillMaxWidth()) { Text("Aprovar e publicar") }
                TextButton(onClick = aoDevolver, enabled = !enviando, modifier = Modifier.fillMaxWidth()) { Text("Devolver para ajustes") }
                TextButton(onClick = aoRecusar, enabled = !enviando, modifier = Modifier.fillMaxWidth()) { Text("Recusar curso", color = Color(0xFFB42336)) }
            }
        },
        confirmButton = { TextButton(enabled = !enviando, onClick = aoFechar) { Text("Fechar") } }
    )
}

@Composable
private fun DialogoDecisaoCurso(
    decisao: DecisaoCurso,
    curso: AdminCursoResponse,
    enviando: Boolean,
    erro: String?,
    aoConfirmar: (String) -> Unit,
    aoCancelar: () -> Unit
) {
    var comentario by remember(curso.id, decisao) { mutableStateOf("") }
    var erroLocal by remember(curso.id, decisao) { mutableStateOf<String?>(null) }
    val exigeComentario = decisao != DecisaoCurso.APROVAR
    val titulo = when (decisao) {
        DecisaoCurso.APROVAR -> "Publicar curso?"
        DecisaoCurso.DEVOLVER -> "Devolver para ajustes"
        DecisaoCurso.RECUSAR -> "Recusar curso"
    }
    val explicacao = when (decisao) {
        DecisaoCurso.APROVAR -> "${curso.titulo} ficará visível no catálogo para as alunas."
        DecisaoCurso.DEVOLVER -> "Escreva o que a parceira precisa corrigir. O comentário ficará visível para ela."
        DecisaoCurso.RECUSAR -> "Informe o motivo da recusa. O curso será arquivado e não poderá ser reenviado."
    }
    AlertDialog(
        onDismissRequest = { if (!enviando) aoCancelar() },
        title = { Text(titulo, color = RevisaoTexto, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(explicacao, color = RevisaoCinza, fontSize = 12.sp)
                if (exigeComentario) {
                    OutlinedTextField(
                        value = comentario,
                        onValueChange = { comentario = it.take(500); erroLocal = null },
                        label = { Text(if (decisao == DecisaoCurso.RECUSAR) "Motivo da recusa" else "O que deve ser ajustado?") },
                        supportingText = { Text("${comentario.length}/500") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                (erroLocal ?: erro)?.let { Text(it, color = Color(0xFFB42336), fontSize = 11.sp) }
            }
        },
        confirmButton = {
            TextButton(enabled = !enviando, onClick = {
                if (exigeComentario && comentario.isBlank()) {
                    erroLocal = if (decisao == DecisaoCurso.RECUSAR) "Informe o motivo da recusa." else "Informe o que deve ser ajustado."
                } else {
                    aoConfirmar(comentario.trim())
                }
            }) { Text(if (enviando) "Salvando…" else if (decisao == DecisaoCurso.APROVAR) "Publicar" else "Confirmar") }
        },
        dismissButton = { TextButton(enabled = !enviando, onClick = aoCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun MensagemRevisao(texto: String, sucesso: Boolean) {
    Text(
        texto,
        Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
            .background(if (sucesso) Color(0xFFE8F7EF) else Color(0xFFFFE9EB)).padding(11.dp),
        color = if (sucesso) Color(0xFF23865D) else Color(0xFFB42336),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun RevisaoCarregando() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EsqueletoBloco(altura = 54.dp)
        EsqueletoBloco(altura = 88.dp)
        EsqueletoBloco(altura = 88.dp)
        EsqueletoBloco(altura = 88.dp)
    }
}

private fun textoLegivel(valor: String?): String = valor.orEmpty().replace('_', ' ').lowercase().replaceFirstChar { it.uppercaseChar() }
