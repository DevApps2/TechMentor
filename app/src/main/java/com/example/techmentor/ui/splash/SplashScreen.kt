package com.example.techmentor.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.techmentor.ui.theme.ManropeFamily
import kotlinx.coroutines.delay

/** Tela de abertura do protótipo: gradiente roxo→azul, logo grande e a frase "Aprenda. Conecte-se. Cresça.". */
@Composable
fun SplashScreen(
    onTerminar: () -> Unit
) {

    val aoTerminar by rememberUpdatedState(onTerminar)

    var visivel by remember { mutableStateOf(false) }

    // Entrada do conteúdo: aparece e sobe 10dp em 0,7 s (animação "rise" do protótipo)
    val progresso by animateFloatAsState(
        targetValue = if (visivel) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "subida"
    )

    LaunchedEffect(Unit) {
        visivel = true
        delay(1700)
        aoTerminar()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    0f to Color(0xFF4C1D95),
                    0.48f to Color(0xFF6D28D9),
                    1f to Color(0xFF1D4ED8)
                )
            )
            .clipToBounds()
    ) {

        // ANÉIS DECORATIVOS
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-190).dp, y = (-90).dp)
                .size(280.dp)
                .border(55.dp, Color.White.copy(alpha = 0.04f), CircleShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 170.dp, y = 120.dp)
                .size(280.dp)
                .border(55.dp, Color.White.copy(alpha = 0.04f), CircleShape)
        )

        // LOGO + NOME + FRASE
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = progresso
                    translationY = (1f - progresso) * 10.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            LogoGrande()

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = buildAnnotatedString {
                    append("Tech")
                    withStyle(SpanStyle(color = Color(0xFFC4D8FF))) {
                        append("Mentor")
                    }
                },
                fontFamily = ManropeFamily,
                fontSize = 34.sp,
                letterSpacing = (-0.8).sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Aprenda. Conecte-se. Cresça.",
                fontSize = 13.sp,
                letterSpacing = 0.4.sp,
                color = Color(0xFFDDD6FE)
            )
        }

        Text(
            text = "TECNOLOGIA FEITA PARA ELAS",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 33.dp),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = Color.White.copy(alpha = 0.52f)
        )
    }
}

@Composable
private fun LogoGrande() {

    val forma = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .size(80.dp)
            .shadow(
                elevation = 16.dp,
                shape = forma,
                ambientColor = Color(0xFF271056).copy(alpha = 0.27f),
                spotColor = Color(0xFF271056).copy(alpha = 0.27f)
            )
            .clip(forma)
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), forma),
        contentAlignment = Alignment.Center
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "<",
                fontFamily = ManropeFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(4.dp)
                    .height(26.dp)
                    .rotate(15f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFBDE3FF))
            )

            Text(
                text = "/>",
                fontFamily = ManropeFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}
