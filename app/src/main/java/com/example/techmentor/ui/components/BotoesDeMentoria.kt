package com.example.techmentor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Peças dos cartões de mentoria (Home da aluna e Home da mentora): os mesmos botões e selos nas duas

private val RoxoDosBotoes = Color(0xFF7C3AED)
private val AzulDosBotoes = Color(0xFF2563EB)

/**
 * A ação principal do cartão (Entrar na sala, Confirmar): gradiente roxo→azul.
 * Desativado, fica cinza. A largura quem define é o chamador (fillMaxWidth ou weight).
 */
@Composable
fun BotaoPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {

    val forma = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(forma)
            .background(
                if (habilitado) Brush.linearGradient(listOf(RoxoDosBotoes, AzulDosBotoes))
                else SolidColor(Color(0xFFEDEBF1))
            )
            .clickable(enabled = habilitado, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (habilitado) Color.White else Color(0xFF9A96A3)
        )
    }
}

/** As ações de apoio (Reagendar, Cancelar): contorno na cor da ação, fundo branco. */
@Composable
fun BotaoSecundario(
    texto: String,
    cor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {

    val forma = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(forma)
            .background(Color.White)
            .border(1.dp, if (habilitado) cor.copy(alpha = 0.55f) else Color(0xFFE1DDE7), forma)
            .clickable(enabled = habilitado, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (habilitado) cor else Color(0xFF9A96A3)
        )
    }
}

/** A etiqueta arredondada de situação ("Confirmada", "Google Meet"...). */
@Composable
fun SeloDeStatus(
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
