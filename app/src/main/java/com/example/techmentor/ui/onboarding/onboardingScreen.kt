package com.example.techmentor.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha
import com.example.techmentor.ui.theme.ManropeFamily
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// ======================================================
// CONTEÚDO
// ======================================================

private val Roxo = Color(0xFF7C3AED)
private val RoxoEscuro = Color(0xFF6D28D9)
private val Azul = Color(0xFF2563EB)
private val TextoMudo = Color(0xFF625D6C)

/** Uma das telas que apresentam uma área do app: uma prévia do que a pessoa vai ver e o que ela consegue fazer ali. */
private class Etapa(
    val rotulo: String,
    val titulo: String,
    val subtitulo: String,
    val topicos: List<String>,
    val corA: Color,
    val corB: Color,
    val previa: @Composable () -> Unit
)

private val etapas = listOf(
    Etapa(
        rotulo = "BEM-VINDA",
        titulo = "Sua jornada em tecnologia começa aqui",
        subtitulo = "O TechMentor junta o que você precisa para entrar na área: quem te guie, o que estudar e onde se candidatar.",
        topicos = listOf(
            "Mentoras que já trilharam esse caminho",
            "Cursos curtos para aprender no seu ritmo",
            "Vagas, bolsas e eventos num só lugar"
        ),
        corA = Color(0xFF4C1D95),
        corB = Color(0xFF1D4ED8),
        previa = { PreviaBoasVindas() }
    ),
    Etapa(
        rotulo = "MENTORIA",
        titulo = "Converse com quem já chegou lá",
        subtitulo = "Escolha uma mentora da sua área e marque uma conversa por videochamada.",
        topicos = listOf(
            "Veja as mentoras por área e a nota de cada uma",
            "Agende, reagende ou cancele pelo app",
            "A sala do Google Meet abre 30 minutos antes",
            "Depois da conversa, vocês se avaliam de 1 a 5 estrelas"
        ),
        corA = Color(0xFF7C3AED),
        corB = Color(0xFF4F46E5),
        previa = { PreviaMentoria() }
    ),
    Etapa(
        rotulo = "APRENDIZADO",
        titulo = "Aprenda no seu ritmo e evolua a cada aula",
        subtitulo = "Cursos divididos em módulos e aulas. Cada passo conta para o seu progresso.",
        topicos = listOf(
            "Concluir uma aula libera a próxima",
            "Ganhe XP, suba de nível e acompanhe sua sequência de dias",
            "Desbloqueie conquistas e cumpra desafios"
        ),
        corA = Color(0xFF2563EB),
        corB = Color(0xFF0EA5E9),
        previa = { PreviaAprendizado() }
    ),
    Etapa(
        rotulo = "OPORTUNIDADES",
        titulo = "Encontre o seu próximo passo",
        subtitulo = "Estágios, empregos, bolsas e eventos para quem está começando.",
        topicos = listOf(
            "Busque e filtre por tipo e por área",
            "Candidate-se ou inscreva-se em poucos toques",
            "Acompanhe cada etapa: enviada, em análise, aprovada ou recusada"
        ),
        corA = Color(0xFF059669),
        corB = Color(0xFF2563EB),
        previa = { PreviaOportunidade() }
    ),
    Etapa(
        rotulo = "SEGURANÇA",
        titulo = "Um espaço feito para você se sentir segura",
        subtitulo = "Se algo não estiver certo, você tem como agir, e a equipe analisa cada denúncia.",
        topicos = listOf(
            "Denuncie usuárias e também comentários",
            "Bloqueie quem te incomodar",
            "Consulte orientações e canais de ajuda quando precisar"
        ),
        corA = Color(0xFFBE185D),
        corB = Color(0xFF7C3AED),
        previa = { PreviaSeguranca() }
    )
)

/** A última tela não é uma Etapa: é a pergunta "por onde começar?", com atalhos. */
private val totalDePaginas = etapas.size + 1

private class Atalho(val icone: Icone, val titulo: String, val detalhe: String, val destino: String)

private val atalhos = listOf(
    Atalho(Icone.USUARIOS, "Conhecer mentoras", "Veja quem pode te ajudar", "mentorship"),
    Atalho(Icone.LIVRO, "Começar um curso", "Escolha a sua primeira trilha", "learning"),
    Atalho(Icone.MALETA, "Ver oportunidades", "Vagas, bolsas e eventos", "opportunities")
)


// ======================================================
// TELA
// ======================================================

/**
 * Boas-vindas da aluna, mostradas uma vez por conta, entre o login e a Home.
 * [onConcluir] recebe para onde ir: "home" (pular ou "Ir para o início") ou a rota do atalho escolhido.
 */
@Composable
fun OnboardingScreen(
    onConcluir: (destino: String) -> Unit
) {

    val estadoDasPaginas = rememberPagerState(pageCount = { totalDePaginas })
    val escopo = rememberCoroutineScope()

    val paginaAtual = estadoDasPaginas.currentPage
    val ehUltima = paginaAtual == totalDePaginas - 1

    fun irPara(pagina: Int) {
        escopo.launch { estadoDasPaginas.animateScrollToPage(pagina) }
    }

    // O "voltar" do celular volta uma tela das boas-vindas (na primeira, sai do app como de costume)
    BackHandler(enabled = paginaAtual > 0) { irPara(paginaAtual - 1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        HorizontalPager(
            state = estadoDasPaginas,
            modifier = Modifier.fillMaxSize()
        ) { indice ->

            // Positivo = esta tela está à esquerda do centro; negativo = à direita; 0 = parada no meio
            val deslocamento = (paginaAtual - indice) + estadoDasPaginas.currentPageOffsetFraction

            if (indice < etapas.size) {
                PaginaDaEtapa(etapa = etapas[indice], deslocamento = deslocamento)
            } else {
                PaginaFinal(deslocamento = deslocamento, onEscolher = { destino -> onConcluir(destino) })
            }
        }

        // TOPO: barrinhas de progresso e "Pular"
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(totalDePaginas) { indice ->

                    val cor by animateColorAsState(
                        targetValue = if (indice <= paginaAtual) Color.White else Color.White.copy(alpha = 0.35f),
                        label = "barra"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(cor)
                    )
                }
            }

            // Na última tela o "Pular" some: ali já se escolhe o destino
            if (!ehUltima) {
                Text(
                    text = "Pular",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) { onConcluir("home") }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // RODAPÉ: voltar e avançar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (paginaAtual > 0) {
                Text(
                    text = "Voltar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuthColors.TextMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { irPara(paginaAtual - 1) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(modifier = Modifier.weight(1f)) {
                GradientButton(
                    text = if (ehUltima) "Ir para o início" else "Próximo",
                    onClick = {
                        if (ehUltima) onConcluir("home") else irPara(paginaAtual + 1)
                    }
                )
            }
        }
    }
}


// ======================================================
// PÁGINAS
// ======================================================

/** Faixa colorida em cima (com a prévia do app) e o texto embaixo. */
@Composable
private fun PaginaDaEtapa(
    etapa: Etapa,
    deslocamento: Float
) {

    Column(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.43f)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.linearGradient(listOf(etapa.corA, etapa.corB)))
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {

            CirculosDecorativos()

            // A prévia anda mais devagar que a tela ao arrastar (efeito de profundidade)
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 28.dp, end = 28.dp, top = 34.dp)
                    .graphicsLayer {
                        translationX = deslocamento * size.width * 0.35f
                        alpha = 1f - deslocamento.absoluteValue.coerceIn(0f, 1f) * 0.9f
                    }
            ) {
                etapa.previa()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.57f)
                .graphicsLayer {
                    alpha = 1f - deslocamento.absoluteValue.coerceIn(0f, 1f) * 0.9f
                }
                .verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 104.dp)
        ) {

            Text(
                text = etapa.rotulo,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = etapa.corA
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = etapa.titulo,
                fontFamily = ManropeFamily,
                fontSize = 23.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AuthColors.TextDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = etapa.subtitulo,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = TextoMudo
            )

            Spacer(modifier = Modifier.height(16.dp))

            etapa.topicos.forEach { topico ->
                Topico(texto = topico, cor = etapa.corA)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun PaginaFinal(
    deslocamento: Float,
    onEscolher: (destino: String) -> Unit
) {

    Column(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.30f)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF4C1D95), Color(0xFF1D4ED8))))
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {

            CirculosDecorativos()

            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 30.dp)
                    .size(84.dp)
                    .graphicsLayer { translationX = deslocamento * size.width * 0.35f }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconeLinha(icone = Icone.TROFEU, cor = Color.White, tamanho = 40.dp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.70f)
                .graphicsLayer {
                    alpha = 1f - deslocamento.absoluteValue.coerceIn(0f, 1f) * 0.9f
                }
                .verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 104.dp)
        ) {

            Text(
                text = "TUDO PRONTO",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = Roxo
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Por onde você quer começar?",
                fontFamily = ManropeFamily,
                fontSize = 23.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AuthColors.TextDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Escolha um atalho. Tudo isso continua no menu de baixo, é só trocar quando quiser.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = TextoMudo
            )

            Spacer(modifier = Modifier.height(18.dp))

            atalhos.forEach { atalho ->
                CartaoDeAtalho(atalho = atalho, onClick = { onEscolher(atalho.destino) })
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun CartaoDeAtalho(
    atalho: Atalho,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, Color(0xFFECE9F1), forma)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFEDE9FE), Color(0xFFDBEAFE)))),
            contentAlignment = Alignment.Center
        ) {
            IconeLinha(icone = atalho.icone, cor = RoxoEscuro, tamanho = 24.dp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = atalho.titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AuthColors.TextDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = atalho.detalhe, fontSize = 11.sp, color = TextoMudo)
        }

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Roxo),
            contentAlignment = Alignment.Center
        ) {
            IconeLinha(icone = Icone.SETA, cor = Color.White, tamanho = 15.dp)
        }
    }
}


// ======================================================
// PEÇAS
// ======================================================

@Composable
private fun CirculosDecorativos() {

    Box(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = (-60).dp)
                .size(190.dp)
                .border(34.dp, Color.White.copy(alpha = 0.06f), CircleShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-70).dp, y = 60.dp)
                .size(170.dp)
                .border(30.dp, Color.White.copy(alpha = 0.06f), CircleShape)
        )
    }
}

@Composable
private fun Topico(
    texto: String,
    cor: Color
) {

    Row(verticalAlignment = Alignment.Top) {

        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(cor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✓", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = cor)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = texto,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = AuthColors.TextBrand
        )
    }
}

/** O cartão branco em que cada prévia aparece, parecido com os cartões reais do app. */
@Composable
private fun CartaoDePrevia(content: @Composable ColumnScope.() -> Unit) {

    val forma = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = forma,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(forma)
            .background(Color.White)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun Selo(
    texto: String,
    fundo: Color,
    cor: Color
) {
    Text(
        text = texto,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = cor,
        modifier = Modifier
            .clip(CircleShape)
            .background(fundo)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

@Composable
private fun QuadroDeIcone(
    icone: Icone,
    fundo: Color = Color(0xFFEDE9FE),
    cor: Color = RoxoEscuro,
    tamanho: Int = 44
) {
    Box(
        modifier = Modifier
            .size(tamanho.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(fundo),
        contentAlignment = Alignment.Center
    ) {
        IconeLinha(icone = icone, cor = cor, tamanho = (tamanho / 2).dp)
    }
}


// ======================================================
// PRÉVIAS (desenhadas com os mesmos elementos do app)
// ======================================================

@Composable
private fun PreviaBoasVindas() {

    val transicao = rememberInfiniteTransition(label = "flutuar")

    val sobe by transicao.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sobe"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Text(
            text = buildAnnotatedString {
                append("Tech")
                withStyle(SpanStyle(color = Color(0xFFC4D8FF))) { append("Mentor") }
            },
            fontFamily = ManropeFamily,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Aprenda. Conecte-se. Cresça.",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(26.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            listOf(
                Triple(Icone.USUARIOS, "Mentoria", 1f),
                Triple(Icone.LIVRO, "Cursos", -1f),
                Triple(Icone.MALETA, "Vagas", 1f)
            ).forEach { (icone, nome, sentido) ->

                Column(
                    modifier = Modifier
                        .offset(y = ((sobe * 10f) - 5f).dp * sentido)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconeLinha(icone = icone, cor = Color.White, tamanho = 26.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = nome, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PreviaMentoria() {

    CartaoDePrevia {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Azul))),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "MS", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Mentoria com Maria Silva",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuthColors.TextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Qui, 12 de nov · 14:00", fontSize = 11.sp, color = TextoMudo)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Selo("Confirmada", Color(0xFFDCFCE7), Color(0xFF15803D))
            Selo("Google Meet", Color(0xFFEDE9FE), RoxoEscuro)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Roxo, Azul))),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Entrar na sala", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun PreviaAprendizado() {

    CartaoDePrevia {

        Row(verticalAlignment = Alignment.CenterVertically) {

            QuadroDeIcone(icone = Icone.CODIGO, tamanho = 50)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = "PROGRAMAÇÃO",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.7.sp,
                    color = Roxo
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Lógica de programação",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuthColors.TextDark
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(text = "3 de 8 aulas", fontSize = 11.sp, color = TextoMudo)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape)
                .background(Color(0xFFE9E6EF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.375f)
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Roxo, Azul)))
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Selo("+50 XP", Color(0xFFFEF3C7), Color(0xFFB45309))
            Selo("Nível 2", Color(0xFFE6EFFF), Azul)
            Selo("7 dias seguidos", Color(0xFFDCFCE7), Color(0xFF15803D))
        }
    }
}

@Composable
private fun PreviaOportunidade() {

    CartaoDePrevia {

        Row(verticalAlignment = Alignment.CenterVertically) {

            QuadroDeIcone(icone = Icone.MALETA, fundo = Color(0xFFD1FAE5), cor = Color(0xFF047857))

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Estágio em Desenvolvimento",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuthColors.TextDark,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Acme Tecnologia", fontSize = 11.sp, color = TextoMudo)
            }

            Selo("Estágio", Color(0xFFD1FAE5), Color(0xFF047857))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Linha do tempo da candidatura: enviada -> em análise -> aprovada
        Row(modifier = Modifier.fillMaxWidth()) {
            Passo(texto = "Enviada", estado = 2)
            Linha(ativa = true)
            Passo(texto = "Em análise", estado = 1)
            Linha(ativa = false)
            Passo(texto = "Aprovada", estado = 0)
        }
    }
}

/** estado: 2 = concluído, 1 = atual, 0 = ainda não chegou. */
@Composable
private fun Passo(
    texto: String,
    estado: Int
) {

    Column(
        modifier = Modifier.width(58.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (estado == 2) Roxo else Color.White)
                .border(
                    border = BorderStroke(2.dp, if (estado == 0) Color(0xFFD9D5E0) else Roxo),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when (estado) {
                2 -> Text(text = "✓", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                1 -> Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Roxo)
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = texto,
            fontSize = 9.sp,
            fontWeight = if (estado == 1) FontWeight.Bold else FontWeight.Medium,
            color = if (estado == 0) AuthColors.TextHint else AuthColors.TextBrand,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Linha(ativa: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(top = 9.dp)
            .height(2.dp)
            .background(if (ativa) Roxo else Color(0xFFD9D5E0))
    )
}

@Composable
private fun PreviaSeguranca() {

    CartaoDePrevia {

        listOf(
            Triple(Icone.ESCUDO, "Denunciar", "Relate usuárias ou comentários"),
            Triple(Icone.USUARIOS, "Bloquear", "Gerencie a sua lista de bloqueios"),
            Triple(Icone.SINO, "Pedir ajuda", "Orientações e canais de apoio")
        ).forEachIndexed { indice, (icone, titulo, detalhe) ->

            if (indice > 0) Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {

                QuadroDeIcone(
                    icone = icone,
                    fundo = Color(0xFFFCE7F3),
                    cor = Color(0xFFBE185D),
                    tamanho = 40
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AuthColors.TextDark)
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(text = detalhe, fontSize = 11.sp, color = TextoMudo)
                }
            }
        }
    }
}
