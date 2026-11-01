package com.example.techmentor.ui.seguranca

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily

internal val DegradeBotao = Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB)))

internal val TextoAviso = "Dados fictícios para demonstração do protótipo. Nenhuma conexão ou envio real é realizado. Não existe equipe de moderação conectada."


// ======================================================
// CABEÇALHO: [<]  Título
// ======================================================

@Composable
internal fun CabecalhoSeguranca(titulo: String, onVoltar: () -> Unit) {

    val forma = RoundedCornerShape(18.dp)

    Column(modifier = Modifier.fillMaxWidth()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .align(Alignment.CenterStart)
                    .clip(forma)
                    .background(Color.White)
                    .border(1.dp, MentoriaCores.Borda, forma)
                    .clickable(role = Role.Button, onClick = onVoltar),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "‹",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium,
                    color = MentoriaCores.TextoEscuro
                )
            }

            Text(
                text = titulo,
                modifier = Modifier.align(Alignment.Center),
                fontFamily = ManropeFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MentoriaCores.TextoEscuro
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MentoriaCores.Borda)
        )
    }
}


/** Base das telas internas: cabeçalho fixo + aviso de demonstração + conteúdo que rola. */
@Composable
internal fun TelaInterna(
    titulo: String,
    onVoltar: () -> Unit,
    conteudo: @Composable ColumnScope.() -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        CabecalhoSeguranca(titulo = titulo, onVoltar = onVoltar)

        Column(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {

            AvisoLateral(texto = TextoAviso)

            Spacer(modifier = Modifier.height(24.dp))

            conteudo()
        }
    }
}


// ======================================================
// AVISO COM BARRA ROXA, BOTÕES E CARTÕES
// ======================================================

@Composable
internal fun AvisoLateral(texto: String, modifier: Modifier = Modifier) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF0F0F6))
    ) {

        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(MentoriaCores.Roxo)
        )

        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MentoriaCores.TextoMudo
        )
    }
}

@Composable
internal fun BotaoDegrade(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {

    val forma = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(forma)
            .background(DegradeBotao)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = texto,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/** Botão só com contorno roxo. Com [ocuparLargura] = false ele fica do tamanho do texto. */
@Composable
internal fun BotaoContorno(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ocuparLargura: Boolean = true
) {

    val forma = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .then(if (ocuparLargura) Modifier.fillMaxWidth() else Modifier)
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Roxo, forma)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = texto,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MentoriaCores.Roxo
        )
    }
}

/** Card branco com título e texto; [destaque] (ex.: "Ligue 188") aparece em roxo embaixo. */
@Composable
internal fun CartaoConteudo(titulo: String, texto: String?, destaque: String? = null) {

    val forma = RoundedCornerShape(24.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {

        Text(
            text = titulo,
            fontFamily = ManropeFamily,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Medium,
            color = MentoriaCores.TextoEscuro
        )

        if (!texto.isNullOrBlank()) {

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = texto,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = MentoriaCores.TextoMudo
            )
        }

        if (!destaque.isNullOrBlank()) {

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = destaque,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MentoriaCores.Roxo
            )
        }
    }
}

@Composable
internal fun TituloDeSecao(texto: String) {

    Text(
        text = texto,
        fontFamily = ManropeFamily,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.6).sp,
        color = MentoriaCores.TextoEscuro
    )
}


// ======================================================
// CONFIRMAÇÃO (check roxo)
// ======================================================

@Composable
internal fun ConfirmacaoSimulada(
    tituloDoCabecalho: String,
    titulo: String,
    texto: String,
    textoDoBotao: String,
    onVoltarCabecalho: () -> Unit,
    onBotao: () -> Unit
) {

    val formaIcone = RoundedCornerShape(36.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        CabecalhoSeguranca(titulo = tituloDoCabecalho, onVoltar = onVoltarCabecalho)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(118.dp)
                    .clip(formaIcone)
                    .background(Color(0xFF7C3AED)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = titulo,
                fontFamily = ManropeFamily,
                fontSize = 34.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.6).sp,
                textAlign = TextAlign.Center,
                color = MentoriaCores.TextoEscuro
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = texto,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                color = MentoriaCores.TextoMudo
            )

            Spacer(modifier = Modifier.height(20.dp))

            BotaoDegrade(texto = textoDoBotao, onClick = onBotao)
        }
    }
}


// ======================================================
// ÍCONES DESENHADOS (bandeira e cadeado não existem no projeto)
// ======================================================

@Composable
internal fun IconeBandeira(cor: Color, tamanho: Dp = 26.dp) {

    Canvas(modifier = Modifier.size(tamanho)) {

        val s = size.width / 24f
        val traco = Stroke(width = 1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val bandeira = Path().apply {
            moveTo(6f * s, 4f * s)
            lineTo(19f * s, 4f * s)
            lineTo(15.5f * s, 8.5f * s)
            lineTo(19f * s, 13f * s)
            lineTo(6f * s, 13f * s)
        }

        drawPath(path = bandeira, color = cor, style = traco)
        drawLine(
            color = cor,
            start = Offset(6f * s, 3f * s),
            end = Offset(6f * s, 21f * s),
            strokeWidth = 1.8f * s,
            cap = StrokeCap.Round
        )
    }
}

@Composable
internal fun IconeCadeado(cor: Color, tamanho: Dp = 24.dp) {

    Canvas(modifier = Modifier.size(tamanho)) {

        val s = size.width / 24f
        val traco = Stroke(width = 1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawRoundRect(
            color = cor,
            topLeft = Offset(4.5f * s, 11f * s),
            size = Size(15f * s, 10f * s),
            cornerRadius = CornerRadius(2.5f * s, 2.5f * s),
            style = traco
        )

        drawArc(
            color = cor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(8f * s, 3.5f * s),
            size = Size(8f * s, 8f * s),
            style = traco
        )

        drawLine(color = cor, start = Offset(8f * s, 7.5f * s), end = Offset(8f * s, 11f * s), strokeWidth = 1.8f * s, cap = StrokeCap.Round)
        drawLine(color = cor, start = Offset(16f * s, 7.5f * s), end = Offset(16f * s, 11f * s), strokeWidth = 1.8f * s, cap = StrokeCap.Round)
    }
}
