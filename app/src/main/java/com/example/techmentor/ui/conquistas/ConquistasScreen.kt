package com.example.techmentor.ui.conquistas

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.window.Dialog
import com.example.techmentor.data.ConquistaResponse
import com.example.techmentor.data.DesafioResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.data.UsuarioConquistaResponse
import com.example.techmentor.data.UsuarioDesafioResponse
import com.example.techmentor.data.UsuarioResponse
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha
import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.home.BottomNavigationBar
import com.example.techmentor.ui.home.fracaoDoNivel
import com.example.techmentor.ui.home.parseDataHora
import com.example.techmentor.ui.home.formatarNumero
import com.example.techmentor.ui.theme.ManropeFamily
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FundoConquistas = Color(0xFFF8F8FB)
private val RoxoConquistas = Color(0xFF7C3AED)
private val RoxoEscuroConquistas = Color(0xFF6D28D9)
private val TextoConquistas = Color(0xFF17151F)
private val TextoMudoConquistas = Color(0xFF85808C)
private val BordaConquistas = Color(0xFFEBE8EF)
private val LocaleConquistas = Locale.forLanguageTag("pt-BR")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConquistasScreen(
    navController: NavController,
    viewModel: ConquistasViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val contexto = LocalContext.current

    LaunchedEffect(estado.sessaoExpirada) {
        if (estado.sessaoExpirada) {
            Sessao.sair()
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoConquistas)
            .statusBarsPadding()
    ) {
        TopBarMarca()

        Box(modifier = Modifier.weight(1f)) {
            when {
                estado.usuario != null -> PullToRefreshBox(
                    isRefreshing = estado.atualizando,
                    onRefresh = { viewModel.carregar(manual = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    ConteudoConquistas(
                        estado = estado,
                        onParticipar = viewModel::participar,
                        onCompartilhar = { conquista, usuario ->
                            compartilharConquista(contexto, conquista, usuario)
                        }
                    )
                }

                estado.carregando || estado.sessaoExpirada -> CarregandoConquistas()

                else -> ErroConquistas(
                    mensagem = estado.erro ?: "Não foi possível carregar suas conquistas.",
                    onTentarNovamente = { viewModel.carregar() }
                )
            }
        }

        BottomNavigationBar(navController = navController, rotaAtual = "achievements")
    }

    estado.celebracoes.firstOrNull()?.let { celebracao ->
        CelebracaoDialog(
            celebracao = celebracao,
            onFechar = viewModel::dispensarCelebracao
        )
    }
}

@Composable
private fun ConteudoConquistas(
    estado: ConquistasUiState,
    onParticipar: (DesafioResponse) -> Unit,
    onCompartilhar: (ConquistaResponse, UsuarioResponse) -> Unit
) {
    val usuario = estado.usuario ?: return
    val idsDesbloqueados = estado.desbloqueadas.map { it.conquistaId }.toSet()
    val cursosConcluidos = estado.matriculas.count { it.concluidoEm != null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 25.dp)
    ) {
        Text(
            text = "SUA JORNADA",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.7.sp,
            color = RoxoConquistas
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = "Conquistas",
            fontFamily = ManropeFamily,
            fontSize = 27.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.6).sp,
            color = TextoConquistas
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "Cada passo conta. Continue brilhando!",
            fontSize = 13.sp,
            color = TextoMudoConquistas
        )

        Spacer(Modifier.height(18.dp))
        CartaoNivel(usuario)
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            EstatisticaJornada(
                modifier = Modifier.weight(1f),
                valor = usuario.sequenciaDias.toString(),
                descricao = if (usuario.sequenciaDias == 1) "dia seguido" else "dias seguidos"
            )
            EstatisticaJornada(
                modifier = Modifier.weight(1f),
                valor = idsDesbloqueados.size.toString(),
                descricao = "medalhas"
            )
            EstatisticaJornada(
                modifier = Modifier.weight(1f),
                valor = cursosConcluidos.toString(),
                descricao = if (cursosConcluidos == 1) "curso concluído" else "cursos concluídos"
            )
        }

        LinhaTituloSecao(
            titulo = "Medalhas",
            detalhe = "${idsDesbloqueados.size} de ${estado.catalogo.size}"
        )

        if (estado.catalogo.isEmpty()) {
            CartaoEstadoVazio(
                titulo = "Suas medalhas aparecem aqui",
                descricao = "Conclua aulas, desafios e mentorias para começar sua coleção."
            )
        } else {
            estado.catalogo.chunked(3).forEach { linha ->
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    linha.forEach { conquista ->
                        val desbloqueio = estado.desbloqueadas.firstOrNull {
                            it.conquistaId == conquista.id
                        }
                        CartaoMedalha(
                            modifier = Modifier.weight(1f),
                            conquista = conquista,
                            desbloqueio = desbloqueio,
                            desbloqueada = conquista.id in idsDesbloqueados,
                            onCompartilhar = { onCompartilhar(conquista, usuario) }
                        )
                    }
                    repeat(3 - linha.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(9.dp))
            }
        }

        LinhaTituloSecao(titulo = "Desafios disponíveis")
        if (estado.erroDesafio != null) {
            Text(
                text = estado.erroDesafio,
                modifier = Modifier.padding(bottom = 9.dp),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = TextoMudoConquistas
            )
        }

        if (estado.desafios.isEmpty()) {
            CartaoEstadoVazio(
                titulo = "Nenhum desafio ativo agora",
                descricao = "Novas missões aparecem aqui quando estiverem disponíveis."
            )
        } else {
            estado.desafios.forEach { desafio ->
                val participacao = estado.participacoes.firstOrNull { it.desafioId == desafio.id }
                CartaoDesafio(
                    desafio = desafio,
                    participacao = participacao,
                    entrando = desafio.id in estado.desafiosEntrando,
                    onParticipar = { onParticipar(desafio) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun CartaoNivel(usuario: UsuarioResponse) {
    val forma = RoundedCornerShape(23.dp)
    val progresso = fracaoDoNivel(usuario)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(
                Brush.linearGradient(
                    0f to Color(0xFF6D28D9),
                    0.56f to Color(0xFF4F46E5),
                    1f to Color(0xFF2563EB)
                )
            )
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(86.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.White.copy(alpha = 0.28f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color(0xFFFACC15),
                    startAngle = -90f,
                    sweepAngle = 360f * progresso,
                    useCenter = false,
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = usuario.nivel.toString(),
                    fontFamily = ManropeFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "NÍVEL",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(Modifier.width(15.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = usuario.tituloNivel?.takeIf { it.isNotBlank() } ?: "Sua evolução",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
                color = Color.White.copy(alpha = 0.76f)
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatarNumero(usuario.xpTotal),
                    fontFamily = ManropeFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = " XP",
                    modifier = Modifier.padding(bottom = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Próximo nível", fontSize = 9.sp, color = Color.White.copy(alpha = 0.9f))
                Text(
                    "${formatarNumero(usuario.xpProximoNivel)} XP",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.28f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progresso)
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE))
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Faltam ${formatarNumero(usuario.xpFaltante)} XP",
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.83f)
            )
        }
    }
}

@Composable
private fun EstatisticaJornada(
    modifier: Modifier,
    valor: String,
    descricao: String
) {
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, BordaConquistas, RoundedCornerShape(16.dp))
            .padding(horizontal = 5.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = valor,
            fontFamily = ManropeFamily,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            color = RoxoEscuroConquistas
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = descricao,
            maxLines = 2,
            textAlign = TextAlign.Center,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            color = TextoMudoConquistas
        )
    }
}

@Composable
private fun LinhaTituloSecao(titulo: String, detalhe: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = titulo,
            fontFamily = ManropeFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            color = TextoConquistas
        )
        if (detalhe != null) {
            Text(
                text = detalhe,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = RoxoConquistas
            )
        }
    }
}

@Composable
private fun CartaoMedalha(
    modifier: Modifier,
    conquista: ConquistaResponse,
    desbloqueio: UsuarioConquistaResponse?,
    desbloqueada: Boolean,
    onCompartilhar: () -> Unit
) {
    val secretaBloqueada = conquista.secreta && !desbloqueada
    val corBase = if (desbloqueada) corDaMedalha(conquista.nome) else Color(0xFFAAA7B0)
    val fundoIcone = if (desbloqueada) corBase.copy(alpha = 0.11f) else Color(0xFFF0EFF2)
    val forma = RoundedCornerShape(17.dp)

    Card(
        modifier = modifier
            .heightIn(min = 151.dp)
            .clickable(enabled = desbloqueada, onClick = onCompartilhar),
        shape = forma,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BordaConquistas),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(43.dp)
                        .clip(CircleShape)
                        .background(fundoIcone)
                        .border(2.dp, corBase, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (secretaBloqueada) {
                        Text("?", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = corBase)
                    } else {
                        IconeLinha(
                            icone = iconeDaConquista(conquista.nome),
                            cor = corBase,
                            tamanho = 22.dp
                        )
                    }
                }
                if (desbloqueada) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(17.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = if (secretaBloqueada) "Conquista secreta" else conquista.nome,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (desbloqueada) TextoConquistas else Color(0xFF817E87)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (secretaBloqueada) {
                    "Dica: ${conquista.descricao.orEmpty()}"
                } else {
                    conquista.descricao.orEmpty()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                color = if (desbloqueada) TextoMudoConquistas else Color(0xFFA29FA7)
            )

            if (desbloqueada) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "↗  Compartilhar",
                    maxLines = 1,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoxoConquistas
                )
            } else if (desbloqueio != null) {
                Text(
                    text = formatarDataDesbloqueio(desbloqueio.desbloqueadaEm),
                    fontSize = 8.sp,
                    color = TextoMudoConquistas
                )
            }
        }
    }
}

@Composable
private fun CartaoDesafio(
    desafio: DesafioResponse,
    participacao: UsuarioDesafioResponse?,
    entrando: Boolean,
    onParticipar: () -> Unit
) {
    val progresso = (participacao?.aulasConcluidas ?: 0L).toInt().coerceAtLeast(0)
    val meta = desafio.metaAulas.coerceAtLeast(1)
    val fracao = (progresso.toFloat() / meta).coerceIn(0f, 1f)
    val concluido = participacao?.status == "CONCLUIDO"
    val acompanhado = participacao != null
    val substantivo = if (desafio.tipo == "MENTORIA") "mentorias" else "aulas"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BordaConquistas),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEDE9FE)),
                contentAlignment = Alignment.Center
            ) {
                IconeLinha(Icone.LIVRO, RoxoEscuroConquistas, tamanho = 23.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = desafio.titulo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoConquistas
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = desafio.descricao.orEmpty(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = TextoMudoConquistas
                )
                Spacer(Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE9E6EF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fracao)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(RoxoConquistas, Color(0xFF2563EB))))
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (acompanhado) "$progresso de $meta $substantivo" else "Entre no desafio para acompanhar seu progresso",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 9.sp,
                    color = RoxoConquistas
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (concluido) "Desafio concluído" else "+${desafio.xpRecompensa} XP · até ${formatarDataCurta(desafio.fim)}",
                    maxLines = 1,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (concluido) Color(0xFF16A34A) else TextoMudoConquistas
                )
            }
            if (!acompanhado) {
                Spacer(Modifier.width(7.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (entrando) Color(0xFFE7DDFB) else Color(0xFFF1EAFE))
                        .clickable(enabled = !entrando, onClick = onParticipar)
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (entrando) "Entrando" else "Participar",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoxoEscuroConquistas
                    )
                }
            }
        }
    }
}

@Composable
private fun CartaoEstadoVazio(titulo: String, descricao: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BordaConquistas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(titulo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextoConquistas)
            Spacer(Modifier.height(4.dp))
            Text(
                descricao,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = TextoMudoConquistas
            )
        }
    }
}

@Composable
private fun CarregandoConquistas() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        repeat(5) { indice ->
            Box(
                Modifier
                    .fillMaxWidth(if (indice == 1) 0.72f else 1f)
                    .height(if (indice == 2) 154.dp else 52.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color(0xFFECEAF2))
            )
        }
    }
}

@Composable
private fun ErroConquistas(mensagem: String, onTentarNovamente: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Não foi possível abrir sua jornada", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextoConquistas)
        Spacer(Modifier.height(7.dp))
        Text(mensagem, textAlign = TextAlign.Center, fontSize = 12.sp, color = TextoMudoConquistas)
        TextButton(onClick = onTentarNovamente) { Text("Tentar novamente", color = RoxoConquistas) }
    }
}

@Composable
private fun CelebracaoDialog(
    celebracao: CelebracaoConquista,
    onFechar: () -> Unit
) {
    Dialog(onDismissRequest = onFechar) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Confetes(modifier = Modifier.fillMaxWidth().height(58.dp))
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (celebracao.nivel != null) "↑" else "★",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(13.dp))
                Text(
                    text = if (celebracao.nivel != null) "NOVO NÍVEL" else "CONQUISTA DESBLOQUEADA",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.3.sp,
                    color = RoxoConquistas
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = celebracao.nivel?.let { "Nível $it" } ?: celebracao.titulo,
                    fontFamily = ManropeFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = TextoConquistas
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = celebracao.descricao,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = TextoMudoConquistas
                )
                if (celebracao.xp > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "+${celebracao.xp} XP",
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF1EAFE))
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RoxoEscuroConquistas
                    )
                }
                Spacer(Modifier.height(9.dp))
                TextButton(onClick = onFechar) {
                    Text("Continuar", color = RoxoConquistas, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Confetes(modifier: Modifier = Modifier) {
    val transicao = rememberInfiniteTransition(label = "confetes")
    val progresso by transicao.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "queda"
    )
    val cores = listOf(Color(0xFF7C3AED), Color(0xFF2563EB), Color(0xFFF59E0B), Color(0xFF22C55E), Color(0xFFEC4899))
    Canvas(modifier) {
        repeat(22) { indice ->
            val x = size.width * ((indice * 37 % 97) / 100f)
            val yBase = (indice * 23f + progresso * size.height * 1.5f) % size.height
            val cor = cores[indice % cores.size]
            if (indice % 2 == 0) {
                drawCircle(cor, radius = 2.8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, yBase))
            } else {
                drawRoundRect(
                    color = cor,
                    topLeft = androidx.compose.ui.geometry.Offset(x, yBase),
                    size = androidx.compose.ui.geometry.Size(5.dp.toPx(), 8.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
            }
        }
    }
}

private fun corDaMedalha(nome: String): Color = when {
    nome.contains("mentoria", ignoreCase = true) -> Color(0xFF7C3AED)
    nome.contains("curso", ignoreCase = true) || nome.contains("aula", ignoreCase = true) -> Color(0xFF2563EB)
    nome.contains("desafio", ignoreCase = true) -> Color(0xFFD97706)
    nome.contains("sequência", ignoreCase = true) -> Color(0xFFDB2777)
    else -> Color(0xFF0F9F6E)
}

private fun iconeDaConquista(nome: String): Icone = when {
    nome.contains("mentoria", ignoreCase = true) -> Icone.USUARIOS
    nome.contains("curso", ignoreCase = true) || nome.contains("aula", ignoreCase = true) -> Icone.LIVRO
    nome.contains("desafio", ignoreCase = true) -> Icone.CODIGO
    nome.contains("oportunidade", ignoreCase = true) -> Icone.MALETA
    else -> Icone.TROFEU
}

private fun formatarDataCurta(valor: String): String = runCatching {
    val data = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(valor.take(10)) ?: return@runCatching valor.take(10)
    SimpleDateFormat("dd/MM", LocaleConquistas).format(data)
}.getOrDefault(valor.take(10))

private fun formatarDataDesbloqueio(valor: String?): String {
    if (valor.isNullOrBlank()) return "Desbloqueada"
    val data = parseDataHora(valor) ?: return "Desbloqueada"
    return "Em " + SimpleDateFormat("dd/MM/yy", LocaleConquistas).format(data)
}

private fun compartilharConquista(
    contexto: Context,
    conquista: ConquistaResponse,
    usuario: UsuarioResponse
) {
    val bitmap = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val tinta = Paint(Paint.ANTI_ALIAS_FLAG)
    val roxo = android.graphics.Color.rgb(109, 40, 217)
    val azul = android.graphics.Color.rgb(37, 99, 235)
    val branco = android.graphics.Color.WHITE

    canvas.drawColor(android.graphics.Color.rgb(248, 248, 251))
    tinta.shader = LinearGradient(50f, 100f, 1030f, 1110f, roxo, azul, Shader.TileMode.CLAMP)
    canvas.drawRoundRect(48f, 110f, 1032f, 1175f, 72f, 72f, tinta)
    tinta.shader = null

    fun textoCentralizado(texto: String, y: Float, tamanho: Float, cor: Int, negrito: Boolean = false) {
        tinta.color = cor
        tinta.textSize = tamanho
        tinta.typeface = Typeface.create("sans-serif", if (negrito) Typeface.BOLD else Typeface.NORMAL)
        tinta.textAlign = Paint.Align.CENTER
        canvas.drawText(texto, 540f, y, tinta)
    }

    textoCentralizado("TECHMENTOR", 235f, 34f, branco, true)
    textoCentralizado("CONQUISTA DESBLOQUEADA", 405f, 24f, android.graphics.Color.rgb(225, 214, 255), true)

    tinta.color = android.graphics.Color.argb(48, 255, 255, 255)
    canvas.drawCircle(540f, 650f, 150f, tinta)
    tinta.color = android.graphics.Color.rgb(255, 199, 76)
    canvas.drawCircle(540f, 650f, 124f, tinta)
    tinta.color = android.graphics.Color.rgb(255, 238, 184)
    canvas.drawCircle(540f, 650f, 99f, tinta)
    textoCentralizado("★", 700f, 120f, roxo, true)

    tinta.color = branco
    tinta.textSize = 55f
    tinta.typeface = Typeface.create("sans-serif", Typeface.BOLD)
    tinta.textAlign = Paint.Align.CENTER
    val linhas = quebrarTexto(conquista.nome, tinta, 900f)
    linhas.take(2).forEachIndexed { index, linha ->
        canvas.drawText(linha, 540f, 905f + index * 66f, tinta)
    }
    textoCentralizado("+${conquista.xpBonus} XP", 1085f, 38f, branco, true)
    textoCentralizado("Uma conquista a mais na jornada de ${usuario.nome}", 1260f, 24f, android.graphics.Color.rgb(98, 93, 108))

    val pasta = File(contexto.cacheDir, "shared").apply { mkdirs() }
    val imagem = File(pasta, "conquista-${conquista.id}.png")
    imagem.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()

    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", imagem)
    val compartilhar = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "Desbloqueei a conquista ${conquista.nome} no TechMentor! +${conquista.xpBonus} XP")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    contexto.startActivity(Intent.createChooser(compartilhar, "Compartilhar conquista"))
}

private fun quebrarTexto(texto: String, tinta: Paint, larguraMaxima: Float): List<String> {
    val linhas = mutableListOf<String>()
    var linha = ""
    texto.split(Regex("\\s+")).forEach { palavra ->
        val candidata = if (linha.isEmpty()) palavra else "$linha $palavra"
        if (linha.isNotEmpty() && tinta.measureText(candidata) > larguraMaxima) {
            linhas += linha
            linha = palavra
        } else {
            linha = candidata
        }
    }
    if (linha.isNotEmpty()) linhas += linha
    return linhas
}
