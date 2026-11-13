package com.example.techmentor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.example.techmentor.data.ApiClient
import com.example.techmentor.ui.theme.ManropeFamily

private val RoxoDaFoto = Color(0xFF7C3AED)
private val AzulDaFoto = Color(0xFF2563EB)

/**
 * A foto de perfil com tudo junto: tocar na foto abre ela ampliada (com zoom); o botão redondo de câmera troca a foto.
 * Sem foto ainda, tocar no avatar já abre a galeria. [enviando] mostra um indicador por cima enquanto a foto sobe.
 */
@Composable
fun AvatarEditavel(
    nome: String,
    fotoUrl: String?,
    enviando: Boolean,
    onTrocarFoto: () -> Unit,
    modifier: Modifier = Modifier,
    tamanho: Dp = 96.dp
) {

    var ampliada by remember { mutableStateOf(false) }

    val temFoto = !fotoUrl.isNullOrBlank()
    val forma = RoundedCornerShape(tamanho * 0.32f)

    fun abrirGaleria() {
        if (!enviando) onTrocarFoto()
    }

    // folga para o botão de câmera "sair" um pouco do canto
    Box(modifier = modifier.size(tamanho + 8.dp)) {

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(tamanho)
                .shadow(
                    elevation = 10.dp,
                    shape = forma,
                    ambientColor = Color(0xFF281849).copy(alpha = 0.12f),
                    spotColor = Color(0xFF281849).copy(alpha = 0.12f)
                )
                .clip(forma)
                .background(Color.White)
                .padding(4.dp)
                .clip(RoundedCornerShape(tamanho * 0.28f))
                .background(Color(0xFFEDE9FE))
                .clickable(role = Role.Button) {
                    if (temFoto) ampliada = true else abrirGaleria()
                }
                .semantics { contentDescription = if (temFoto) "Ampliar foto de perfil" else "Escolher foto de perfil" },
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = nome.trim().firstOrNull()?.uppercase() ?: "?",
                fontFamily = ManropeFamily,
                fontSize = (tamanho.value * 0.38f).sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF6D28D9)
            )

            if (temFoto) {
                AsyncImage(
                    model = ApiClient.urlDaFoto(fotoUrl),
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (enviando) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(tamanho * 0.3f)
                    )
                }
            }
        }

        // Botão de câmera
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(36.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(RoxoDaFoto, AzulDaFoto)))
                .border(2.dp, Color.White, CircleShape)
                .clickable(role = Role.Button) { abrirGaleria() }
                .semantics { contentDescription = "Trocar foto de perfil" },
            contentAlignment = Alignment.Center
        ) {
            IconeCamera(cor = Color.White, tamanho = 18.dp)
        }
    }

    if (ampliada && temFoto) {
        FotoAmpliada(
            url = ApiClient.urlDaFoto(fotoUrl).orEmpty(),
            aoFechar = { ampliada = false }
        )
    }
}

/** A foto em tela cheia: aperte com dois dedos para dar zoom, toque fora ou no X para fechar. */
@Composable
fun FotoAmpliada(
    url: String,
    aoFechar: () -> Unit
) {

    var escala by remember { mutableFloatStateOf(1f) }
    var deslocamento by remember { mutableStateOf(Offset.Zero) }

    val gestos = rememberTransformableState { zoom, pan, _ ->
        escala = (escala * zoom).coerceIn(1f, 5f)
        deslocamento = if (escala > 1f) deslocamento + pan else Offset.Zero
    }

    Dialog(
        onDismissRequest = aoFechar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = aoFechar
                ),
            contentAlignment = Alignment.Center
        ) {

            AsyncImage(
                model = url,
                contentDescription = "Foto de perfil ampliada",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .graphicsLayer {
                        scaleX = escala
                        scaleY = escala
                        translationX = deslocamento.x
                        translationY = deslocamento.y
                    }
                    .transformable(gestos)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(role = Role.Button, onClick = aoFechar)
                    .semantics { contentDescription = "Fechar" },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✕", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

/** Câmera desenhada: corpo, alça e lente. */
@Composable
private fun IconeCamera(
    cor: Color,
    tamanho: Dp
) {
    Canvas(modifier = Modifier.size(tamanho)) {
        val w = size.width
        val h = size.height
        val traco = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawRoundRect(
            color = cor,
            topLeft = Offset(w * 0.05f, h * 0.27f),
            size = Size(w * 0.9f, h * 0.63f),
            cornerRadius = CornerRadius(w * 0.14f),
            style = traco
        )

        // a "corcova" em cima do corpo
        drawLine(cor, Offset(w * 0.30f, h * 0.27f), Offset(w * 0.38f, h * 0.10f), traco.width, StrokeCap.Round)
        drawLine(cor, Offset(w * 0.38f, h * 0.10f), Offset(w * 0.62f, h * 0.10f), traco.width, StrokeCap.Round)
        drawLine(cor, Offset(w * 0.62f, h * 0.10f), Offset(w * 0.70f, h * 0.27f), traco.width, StrokeCap.Round)

        drawCircle(color = cor, radius = w * 0.2f, center = Offset(w * 0.5f, h * 0.58f), style = traco)
    }
}