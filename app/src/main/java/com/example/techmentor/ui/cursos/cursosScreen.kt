package com.example.techmentor.ui.cursos

import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.theme.ManropeFamily
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.techmentor.data.CursoResponse
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha
import com.example.techmentor.ui.home.BottomNavigationBar

private val RoxoCursos = Color(0xFF7C3AED)
private val RoxoEscuroCursos = Color(0xFF312E81)
private val TextoCursos = Color(0xFF17151F)
private val CinzaCursos = Color(0xFF817B8A)
private val CategoriasCursos = listOf("Todas", "Programação", "Dados", "Segurança", "Design", "IA", "Redes", "Carreira")

@Composable
fun CursosScreen(navController: NavController, vm: CursosViewModel = viewModel()) {
    val estado by vm.estado.collectAsState()
    var categoriaSelecionada by remember { mutableStateOf("Todas") }
    val rotaAtual = navController.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(rotaAtual) {
        if (rotaAtual == "learning") vm.carregar(forcar = true)
    }

    val cursos = estado.cursos.filter { categoriaSelecionada == "Todas" || rotuloCategoriaCurso(it.curso.categoria).equals(categoriaSelecionada, true) }

    Column(Modifier.fillMaxSize().background(Color(0xFFF8F7FB)).statusBarsPadding()) {
        TopBarMarca(navController = navController)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(Modifier.padding(top = 13.dp, bottom = 3.dp)) {
                    Text("EVOLUA NO SEU RITMO", color = RoxoCursos, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                    Text("Aprenda tecnologia", fontFamily = ManropeFamily, color = TextoCursos, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    Text("Trilhas práticas para transformar seu futuro.", color = CinzaCursos, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
            if (estado.carregando) {
                item { Box(Modifier.fillMaxWidth().padding(35.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RoxoCursos) } }
            } else if (estado.erro != null) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(estado.erro.orEmpty(), color = CinzaCursos, fontSize = 14.sp)
                        TextButton(onClick = vm::carregar) { Text("Tentar novamente", color = RoxoCursos) }
                    }
                }
            } else {
                val destaque = estado.cursos.firstOrNull()
                if (destaque != null) item { CursoDestaque(destaque.curso) { navController.navigate("learning/${destaque.curso.id}") } }
                item {
                    Text("Explore as trilhas", color = TextoCursos, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 1.dp))
                }
                item {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        CategoriasCursos.forEach { categoria ->
                            val selecionada = categoria == categoriaSelecionada
                            Box(
                                Modifier.clip(CircleShape)
                                    .background(if (selecionada) RoxoCursos else Color.White)
                                    .border(1.dp, if (selecionada) RoxoCursos else Color(0xFFEAE7EF), CircleShape)
                                    .clickable { categoriaSelecionada = categoria }
                                    .padding(horizontal = 13.dp, vertical = 7.dp)
                            ) { Text(categoria, color = if (selecionada) Color.White else TextoCursos, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                        }
                    }
                }
                if (cursos.isEmpty()) item { Text("Nenhum curso nesta categoria ainda.", color = CinzaCursos, fontSize = 13.sp, modifier = Modifier.padding(vertical = 18.dp)) }
                items(cursos, key = { it.curso.id }) { item ->
                    CursoCard(item) { navController.navigate("learning/${item.curso.id}") }
                }
            }
        }
        BottomNavigationBar(navController = navController, rotaAtual = "learning")
    }
}

@Composable
private fun CursoDestaque(curso: CursoResponse, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF27276E), Color(0xFF6128D9))))
            .clickable(onClick = onClick).padding(17.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text("COMECE OS SEUS PLANOS", color = Color.White.copy(alpha = .72f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(curso.titulo, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
            Text("${rotuloCategoriaCurso(curso.categoria)} · ${rotuloNivel(curso.nivel)}", color = Color.White.copy(alpha = .78f), fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            Row(Modifier.padding(top = 10.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Conheça a trilha", color = RoxoEscuroCursos, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                IconeLinha(Icone.SETA, RoxoEscuroCursos, tamanho = 13.dp, modifier = Modifier.padding(start = 4.dp))
            }
        }
        IconeLinha(iconeDoCurso(curso.categoria), Color.White.copy(alpha = .15f), modifier = Modifier.align(Alignment.BottomEnd), tamanho = 46.dp, espessura = 2.4f)
    }
}

@Composable
private fun CursoCard(item: CursoCatalogoItem, onClick: () -> Unit) {
    val curso = item.curso
    val total = curso.totalAulas.toInt().coerceAtLeast(0)
    val fracao = if (total == 0) 0f else (item.aulasConcluidas.toFloat() / total).coerceIn(0f, 1f)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Color.White)
            .border(1.dp, Color(0xFFECE9F1), RoundedCornerShape(17.dp)).clickable(onClick = onClick).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(47.dp).clip(RoundedCornerShape(13.dp)).background(fundoDaCategoria(curso.categoria)), contentAlignment = Alignment.Center) {
            IconeLinha(iconeDoCurso(curso.categoria), corDaCategoria(curso.categoria), tamanho = 22.dp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("${rotuloCategoriaCurso(curso.categoria)} · ${rotuloNivel(curso.nivel)}", color = RoxoCursos, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = .35.sp)
            Text(curso.titulo, color = TextoCursos, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            Text("$total aulas · até ${formatarDuracao(curso.duracaoTotalMinutos)} · ${curso.xpTotal} XP", color = CinzaCursos, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(Color(0xFFE9E6EF))) {
                Box(Modifier.fillMaxWidth(fracao).height(5.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(RoxoCursos, Color(0xFF2563EB)))))
            }
            Text("${item.aulasConcluidas} de $total aulas · ${if (item.iniciado) "Continuar" else "Iniciar"}", color = RoxoCursos, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.width(6.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (item.iniciado) "Continuar" else "Iniciar", color = RoxoCursos, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            IconeLinha(Icone.SETA, RoxoCursos, tamanho = 15.dp)
        }
    }
}

internal fun rotuloNivel(nivel: String?): String = when (nivel) {
    "INICIANTE" -> "Iniciante"
    "INTERMEDIARIO" -> "Intermediário"
    "AVANCADO" -> "Avançado"
    else -> "Todos os níveis"
}

internal fun rotuloCategoriaCurso(categoria: String): String = when (categoria) {
    "PROGRAMACAO" -> "Programação"
    "DADOS" -> "Dados"
    "SEGURANCA" -> "Segurança"
    "DESIGN" -> "Design"
    "IA" -> "IA"
    "REDES" -> "Redes"
    "CARREIRA" -> "Carreira"
    else -> "Outros"
}

internal fun formatarDuracao(minutos: Int): String {
    val horas = minutos / 60
    val resto = minutos % 60
    return when {
        horas > 0 && resto > 0 -> "${horas}h${resto.toString().padStart(2, '0')}min"
        horas > 0 -> "${horas}h"
        else -> "${resto}min"
    }
}

internal fun iconeDoCurso(categoria: String): Icone = when (categoria) {
    "DADOS" -> Icone.LIVRO
    "SEGURANCA" -> Icone.ESCUDO
    "CARREIRA" -> Icone.MALETA
    "DESIGN" -> Icone.PERFIL
    else -> Icone.CODIGO
}

private fun fundoDaCategoria(categoria: String): Color = when (categoria) {
    "DADOS" -> Color(0xFFE6EEFF)
    "SEGURANCA" -> Color(0xFFFFE8F2)
    "DESIGN" -> Color(0xFFFFF1DF)
    else -> Color(0xFFF0E9FF)
}

private fun corDaCategoria(categoria: String): Color = when (categoria) {
    "DADOS" -> Color(0xFF2563EB)
    "SEGURANCA" -> Color(0xFFEC4899)
    "DESIGN" -> Color(0xFFEA8A17)
    else -> RoxoCursos
}
