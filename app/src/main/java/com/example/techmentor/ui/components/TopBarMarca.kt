package com.example.techmentor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.techmentor.ui.theme.ManropeFamily

@Composable
fun TopBarMarca() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8FB))
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            LogoPequena()

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = buildAnnotatedString {
                    append("Tech")
                    withStyle(SpanStyle(color = Color(0xFF7C3AED))) {
                        append("Mentor")
                    }
                },
                fontFamily = ManropeFamily,
                fontSize = 19.sp,
                letterSpacing = (-0.6).sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF20202A)
            )
        }

        // linha fina embaixo do cabeçalho
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFEFEDF4))
        )
    }
}

@Composable
private fun LogoPequena() {

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF8B5CF6), Color(0xFF2563EB))
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "<",
                fontFamily = ManropeFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(2.dp)
                    .height(11.dp)
                    .rotate(15f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFBDE3FF))
            )

            Text(
                text = "/>",
                fontFamily = ManropeFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}