package com.example.techmentor.ui.seguranca

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.techmentor.data.UsuarioResumoResponse
import com.example.techmentor.ui.auth.AuthField
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.home.BottomNavigationBar
import com.example.techmentor.ui.mentoria.AvatarMentora
import com.example.techmentor.ui.mentoria.CabecalhoVoltar
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily


// ======================================================
// BASE DAS TELAS INTERNAS
// ======================================================

/** Cabeçalho com voltar, conteúdo que rola e a barra inferior (igual ao perfil da mentora). */
@Composable
internal fun TelaInterna(
    navController: NavController,
    titulo: String,
    onVoltar: () -> Unit,
    conteudo: @Composable ColumnScope.() -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        CabecalhoVoltar(
            titulo = titulo,
            onVoltar = onVoltar
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = conteudo
        )

        BottomNavigationBar(
            navController = navController,
            rotaAtual = "safety"
        )
    }
}

@Composable
internal fun TituloDeSecao(texto: String) {

    Text(
        text = texto,
        fontFamily = ManropeFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
        color = MentoriaCores.TextoEscuro
    )
}

@Composable
internal fun MensagemDeErro(texto: String) {

    Text(
        text = texto,
        fontSize = 11.sp,
        color = Color(0xFFDC2626)
    )
}


// ======================================================
// CARTÕES
// ======================================================

/** Card branco com título e texto; [destaque] (ex.: "Ligue 188") aparece em roxo embaixo. */
@Composable
internal fun CartaoConteudo(titulo: String, texto: String?, destaque: String? = null) {

    val forma = RoundedCornerShape(17.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(14.dp)
    ) {

        Text(
            text = titulo,
            fontFamily = ManropeFamily,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = MentoriaCores.TextoEscuro
        )

        if (!texto.isNullOrBlank()) {

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = texto,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MentoriaCores.TextoMudo
            )
        }

        if (!destaque.isNullOrBlank()) {

            Spacer(modifier = Modifier.height(6.dp))

            if (!destaque.isNullOrBlank()) {

                Spacer(modifier = Modifier.height(6.dp))

                // "Ligue 180" -> 180: se o destaque tem um número de telefone, tocar abre o discador
                val abrir = LocalUriHandler.current
                val numero = destaque.filter { it.isDigit() }.takeIf { it.length in 3..11 }

                Text(
                    text = if (numero != null) "$destaque ›" else destaque,
                    modifier = if (numero != null) {
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) {
                                // abre o discador já com o número; sem app de telefone, só não faz nada
                                runCatching { abrir.openUri("tel:$numero") }
                            }
                            .padding(vertical = 6.dp)
                    } else {
                        Modifier
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MentoriaCores.Roxo
                )
            }
        }
    }
}

/** Caixa com borda tracejada para "nada por aqui ainda". */
@Composable
internal fun CaixaTracejada(texto: String) {

    val raio = 17.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(raio))
            .background(Color.White)
            .drawBehind {
                val espessura = 1.dp.toPx()
                drawRoundRect(
                    color = Color(0xFFCFCBD8),
                    topLeft = Offset(espessura / 2, espessura / 2),
                    size = Size(size.width - espessura, size.height - espessura),
                    cornerRadius = CornerRadius(raio.toPx(), raio.toPx()),
                    style = Stroke(
                        width = espessura,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
                    )
                )
            }
            .padding(horizontal = 16.dp, vertical = 22.dp)
    ) {

        Text(
            text = texto,
            fontSize = 12.sp,
            color = MentoriaCores.TextoMudo
        )
    }
}


// ======================================================
// BUSCA DE USUÁRIO (denunciar e bloquear)
// ======================================================

/** Campo de busca por nome com a lista de resultados logo abaixo. */
@Composable
internal fun BuscaDeUsuario(
    rotulo: String,
    texto: String,
    onTexto: (String) -> Unit,
    resultados: List<UsuarioResumoResponse>,
    buscando: Boolean,
    onEscolher: (UsuarioResumoResponse) -> Unit
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        AuthField(
            label = rotulo,
            value = texto,
            onValueChange = onTexto,
            placeholder = "Busque pelo nome",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions.Default
        )

        if (texto.trim().length >= 2) {

            when {
                buscando -> Text(
                    text = "Buscando...",
                    fontSize = 11.sp,
                    color = MentoriaCores.TextoMudo
                )

                resultados.isEmpty() -> Text(
                    text = "Nenhuma pessoa encontrada.",
                    fontSize = 11.sp,
                    color = MentoriaCores.TextoMudo
                )

                else -> resultados.take(6).forEach { usuario ->
                    LinhaDeUsuario(
                        usuario = usuario,
                        onClick = { onEscolher(usuario) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaDeUsuario(
    usuario: UsuarioResumoResponse,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        AvatarMentora(
            nome = usuario.nome,
            fotoUrl = usuario.fotoUrl,
            tamanho = 36.dp,
            raio = 12.dp,
            tamanhoDaInicial = 15.sp
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = usuario.nome,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF20202A),
            maxLines = 1
        )

        Text(
            text = when (usuario.tipo.uppercase()) {
                "MENTORA" -> "Mentora"
                "ADMIN" -> "Equipe"
                else -> "Aluna"
            },
            fontSize = 10.sp,
            color = MentoriaCores.TextoMudo
        )
    }
}

/** A pessoa escolhida na busca, com a opção de trocar. */
@Composable
internal fun UsuarioEscolhido(
    rotulo: String,
    nome: String,
    onTrocar: () -> Unit
) {

    val forma = RoundedCornerShape(14.dp)

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        Text(
            text = rotulo,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF514C58)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(forma)
                .background(MentoriaCores.RoxoClaro)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            AvatarMentora(
                nome = nome,
                fotoUrl = null,
                tamanho = 32.dp,
                raio = 10.dp,
                tamanhoDaInicial = 14.sp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = nome,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MentoriaCores.RoxoEscuro,
                maxLines = 1
            )

            Text(
                text = "Trocar",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onTrocar)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MentoriaCores.Roxo
            )
        }
    }
}


// ======================================================
// CONFIRMAÇÃO (check roxo)
// ======================================================

@Composable
internal fun ConfirmacaoDaAcao(
    navController: NavController,
    tituloDoCabecalho: String,
    titulo: String,
    texto: String,
    textoDoBotao: String,
    onVoltar: () -> Unit,
    // conteúdo opcional entre o texto e o botão (ex.: "Bloquear também")
    extra: (@Composable ColumnScope.() -> Unit)? = null
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        CabecalhoVoltar(
            titulo = tituloDoCabecalho,
            onVoltar = onVoltar
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MentoriaCores.Roxo),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = titulo,
                fontFamily = ManropeFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                textAlign = TextAlign.Center,
                color = MentoriaCores.TextoEscuro
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = texto,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF797486)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (extra != null) {
                extra()
                Spacer(modifier = Modifier.height(12.dp))
            }

            GradientButton(
                text = textoDoBotao,
                onClick = onVoltar
            )
        }

        BottomNavigationBar(
            navController = navController,
            rotaAtual = "safety"
        )
    }
}


// ======================================================
// ÍCONES DESENHADOS (bandeira e cadeado não existem no projeto)
// ======================================================

@Composable
internal fun IconeBandeira(cor: Color, tamanho: Dp = 22.dp) {

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
internal fun IconeCadeado(cor: Color, tamanho: Dp = 20.dp) {

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
