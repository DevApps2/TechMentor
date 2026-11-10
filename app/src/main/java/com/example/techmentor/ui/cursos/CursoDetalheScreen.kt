package com.example.techmentor.ui.cursos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.AulaProgressoResponse
import com.example.techmentor.data.ModuloResponse
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha

private val RoxoDetalhe = Color(0xFF7C3AED)
private val TextoDetalhe = Color(0xFF17151F)
private val CinzaDetalhe = Color(0xFF817B8A)

@Composable
fun CursoDetalheScreen(
    cursoId: Long,
    navController: NavController,
    vm: CursoDetalheViewModel = viewModel()
) {
    val estado by vm.estado.collectAsState()
    var aulaSelecionada by remember { mutableStateOf<AulaProgressoResponse?>(null) }
    val modulosAbertos = remember(estado.curso?.id) { mutableStateMapOf<Int, Boolean>() }
    LaunchedEffect(cursoId) { vm.carregar(cursoId) }

    Column(Modifier.fillMaxSize().background(Color(0xFFF8F7FB)).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(34.dp).clip(CircleShape).clickable { navController.popBackStack() }, contentAlignment = Alignment.Center) {
                IconeLinha(Icone.SETA, TextoDetalhe, modifier = Modifier.size(19.dp), tamanho = 20.dp)
            }
            Text("Sua trilha", color = TextoDetalhe, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
        }

        if (estado.carregando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RoxoDetalhe) }
        } else if (estado.curso == null) {
            Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(estado.erro ?: "Não foi possível abrir este curso.", color = CinzaDetalhe, fontSize = 13.sp)
                TextButton(onClick = { vm.carregar(cursoId) }) { Text("Tentar novamente", color = RoxoDetalhe) }
            }
        } else {
            val curso = estado.curso!!
            val total = estado.totalAulas
            val percentual = if (total == 0) 0 else (estado.aulasConcluidas * 100 / total).coerceIn(0, 100)
            val porId = estado.aulas.associateBy { it.aulaId }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFE8E9FF)), contentAlignment = Alignment.Center) {
                            IconeLinha(iconeDoCurso(curso.categoria), RoxoDetalhe, tamanho = 28.dp, espessura = 2.1f)
                        }
                        Text("${rotuloCategoriaCurso(curso.categoria)} · ${rotuloNivel(curso.nivel)}", color = RoxoDetalhe, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = .6.sp, modifier = Modifier.padding(top = 11.dp))
                        Text(curso.titulo, color = TextoDetalhe, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                        Text(curso.descricao.orEmpty(), color = CinzaDetalhe, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 8.dp)) {
                            InfoPill("$total aulas")
                            InfoPill("Até ${formatarDuracao(curso.duracaoTotalMinutos)}")
                            InfoPill("+${curso.xpTotal} XP")
                        }
                    }
                }

                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF6D28D9), Color(0xFF2563EB))))
                            .padding(15.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Seu progresso", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("$percentual%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(9.dp))
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = .25f))) {
                            Box(Modifier.fillMaxWidth(percentual / 100f).height(6.dp).clip(CircleShape).background(Color.White))
                        }
                        val proxima = estado.aulas.firstOrNull { it.status == "ATUAL" }
                        Text(
                            if (proxima != null) "${estado.aulasConcluidas} de $total aulas concluídas · Próxima: ${proxima.titulo}"
                            else if (total > 0 && estado.aulasConcluidas == total) "Trilha concluída · Parabéns pela conquista!"
                            else "${estado.aulasConcluidas} de $total aulas concluídas",
                            color = Color.White.copy(alpha = .88f), fontSize = 9.sp, modifier = Modifier.padding(top = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                item { Text("Conteúdo da trilha", color = TextoDetalhe, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp)) }

                items(curso.modulos, key = { it.id }) { modulo ->
                    val aberto = modulosAbertos[modulo.ordem] ?: (modulo.ordem == curso.modulos.minOfOrNull { m -> m.ordem })
                    ModuloCard(
                        modulo = modulo,
                        aulas = modulo.aulas.mapNotNull { aula -> porId[aula.id] },
                        aberto = aberto,
                        onAbrir = { modulosAbertos[modulo.ordem] = !aberto },
                        onSelecionarAula = { aulaSelecionada = it }
                    )
                }
                if (estado.erro != null) item { Text(estado.erro.orEmpty(), color = Color(0xFFB42318), fontSize = 11.sp) }
            }
        }
    }

    aulaSelecionada?.let { aula ->
        DialogoAula(
            aula = aula,
            conteudoUrl = estado.curso?.modulos
                ?.flatMap { modulo -> modulo.aulas }
                ?.firstOrNull { conteudo -> conteudo.id == aula.aulaId }
                ?.conteudoUrl,
            salvando = estado.salvandoAula,
            onFechar = { aulaSelecionada = null },
            onConcluir = {
                vm.concluirAula(aula)
                aulaSelecionada = null
            }
        )
    }
}

@Composable
private fun InfoPill(texto: String) {
    Box(Modifier.clip(CircleShape).background(Color(0xFFEDE9FE)).padding(horizontal = 9.dp, vertical = 4.dp)) {
        Text(texto, color = RoxoDetalhe, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModuloCard(
    modulo: ModuloResponse,
    aulas: List<AulaProgressoResponse>,
    aberto: Boolean,
    onAbrir: () -> Unit,
    onSelecionarAula: (AulaProgressoResponse) -> Unit
) {
    val concluidas = aulas.count { it.status == "CONCLUIDA" }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, Color(0xFFECE9F1), RoundedCornerShape(16.dp))) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onAbrir).padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(27.dp).clip(CircleShape).background(Color(0xFFF1EDFB)), contentAlignment = Alignment.Center) {
                Text(modulo.ordem.toString(), color = RoxoDetalhe, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(modulo.titulo, color = TextoDetalhe, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${modulo.aulas.size} aulas · $concluidas concluídas", color = CinzaDetalhe, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Text(if (aberto) "−" else "+", color = RoxoDetalhe, fontSize = 19.sp, fontWeight = FontWeight.Light)
        }
        if (aberto) {
            HorizontalDivider(color = Color(0xFFF1EFF4))
            aulas.forEach { aula -> AulaLinha(aula, onClick = { onSelecionarAula(aula) }) }
        }
    }
}

@Composable
private fun AulaLinha(aula: AulaProgressoResponse, onClick: () -> Unit) {
    val concluida = aula.status == "CONCLUIDA"
    val atual = aula.status == "ATUAL"
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape)
                .background(if (concluida || atual) RoxoDetalhe else Color(0xFFECEAF0)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (concluida) "✓" else if (atual) "›" else "·", color = if (concluida || atual) Color.White else CinzaDetalhe, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Text(aula.titulo, color = if (aula.status == "BLOQUEADA") CinzaDetalhe else TextoDetalhe, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${aula.duracaoMinutos} min · +${aula.xpRecompensa} XP", color = CinzaDetalhe, fontSize = 8.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            when (aula.status) { "CONCLUIDA" -> "Concluída"; "ATUAL" -> "Estudar"; else -> "Bloqueada" },
            color = if (aula.status == "BLOQUEADA") CinzaDetalhe else RoxoDetalhe,
            fontSize = 8.sp,
            fontWeight = if (atual) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DialogoAula(
    aula: AulaProgressoResponse,
    conteudoUrl: String?,
    salvando: Boolean,
    onFechar: () -> Unit,
    onConcluir: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val urlDoConteudo = conteudoUrl?.takeIf { it.isNotBlank() }
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(aula.titulo, color = TextoDetalhe, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column {
                Text("${aula.tituloModulo} · ${aula.duracaoMinutos} min · ${aula.xpRecompensa} XP", color = CinzaDetalhe, fontSize = 12.sp)
                when (aula.status) {
                    "CONCLUIDA" -> Text("Aula concluída. Você já recebeu os pontos de XP desta etapa.", color = CinzaDetalhe, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                    "BLOQUEADA" -> Text("Conclua a aula anterior para liberar esta etapa.", color = CinzaDetalhe, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                    else -> Text("Esta é sua próxima aula. Ao concluir, seu progresso será salvo.", color = CinzaDetalhe, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                }
                if (aula.status == "ATUAL") {
                    TextButton(onClick = onConcluir, enabled = !salvando, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        if (salvando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = RoxoDetalhe)
                        else Text("Concluir aula e ganhar ${aula.xpRecompensa} XP", color = RoxoDetalhe, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            if (aula.status == "ATUAL" && urlDoConteudo != null) {
                TextButton(onClick = { runCatching { uriHandler.openUri(urlDoConteudo) } }) { Text("Abrir conteúdo", color = RoxoDetalhe) }
            } else TextButton(onClick = onFechar) { Text("Fechar", color = RoxoDetalhe) }
        },
        containerColor = Color.White
    )
}
