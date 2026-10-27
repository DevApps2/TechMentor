package com.example.techmentor.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.techmentor.ui.theme.DmSansFamily
import com.example.techmentor.ui.theme.ManropeFamily

// Componentes compartilhados pelas telas de login e de criação de conta.

internal object AuthColors {
    val Purple = Color(0xFF7C3AED)
    val PurpleDark = Color(0xFF6D28D9)
    val Blue = Color(0xFF2563EB)
    val TextDark = Color(0xFF17151F)
    val TextBrand = Color(0xFF20202A)
    val TextMuted = Color(0xFF77717E)
    val TextLabel = Color(0xFF514C58)
    val TextHint = Color(0xFFAAA5AE)
    val FieldBackground = Color(0xFFFAF9FC)
    val FieldBorder = Color(0xFFE1DDE7)
    val Divider = Color(0xFFE8E5EB)
    val Error = Color(0xFFDC2626)
}


// ======================================================
// FUNDO + CONTEÚDO ROLÁVEL
// ======================================================

@Composable
internal fun AuthScaffold(
    content: @Composable ColumnScope.() -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clipToBounds()
    ) {

        // CÍRCULOS DECORATIVOS
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 130.dp, y = (-110).dp)
                .size(230.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDE9FE))
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-110).dp, y = (-30).dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFF6FF))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 28.dp,
                    end = 28.dp,
                    top = 32.dp,
                    bottom = 32.dp
                ),
            content = content
        )
    }
}


// ======================================================
// LOGO TECHMENTOR
// ======================================================

@Composable
internal fun BrandHeader() {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        val logoShape = RoundedCornerShape(16.dp)

        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = logoShape,
                    ambientColor = AuthColors.Purple.copy(alpha = 0.27f),
                    spotColor = AuthColors.Purple.copy(alpha = 0.27f)
                )
                .clip(logoShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6),
                            AuthColors.Blue
                        )
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .width(3.dp)
                        .height(16.dp)
                        .rotate(15f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFBDE3FF))
                )

                Text(
                    text = "/>",
                    fontFamily = ManropeFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Text(
            text = buildAnnotatedString {
                append("Tech")
                withStyle(SpanStyle(color = AuthColors.Purple)) {
                    append("Mentor")
                }
            },
            fontFamily = ManropeFamily,
            fontSize = 25.sp,
            lineHeight = 37.5.sp,
            letterSpacing = (-0.8).sp,
            fontWeight = FontWeight.ExtraBold,
            color = AuthColors.TextBrand
        )
    }
}


// ======================================================
// TÍTULO DA TELA
// ======================================================

@Composable
internal fun AuthHeading(
    eyebrow: String,
    title: String,
    subtitle: String
) {

    Text(
        text = eyebrow,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.4.sp,
        color = AuthColors.Purple
    )

    Spacer(
        modifier = Modifier.height(9.dp)
    )

    Text(
        text = title,
        fontFamily = ManropeFamily,
        fontSize = 28.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1).sp,
        fontWeight = FontWeight.SemiBold,
        color = AuthColors.TextDark
    )

    Spacer(
        modifier = Modifier.height(6.dp)
    )

    Text(
        text = subtitle,
        fontSize = 12.sp,
        color = AuthColors.TextMuted
    )
}


// ======================================================
// CAMPO DE TEXTO
// ======================================================

@Composable
internal fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {

    Column {

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AuthColors.TextLabel
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        val shape = RoundedCornerShape(12.dp)

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = DmSansFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AuthColors.TextLabel
            ),
            cursorBrush = SolidColor(AuthColors.Purple),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            decorationBox = { innerTextField ->

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(47.dp)
                        .clip(shape)
                        .background(AuthColors.FieldBackground)
                        .border(
                            border = BorderStroke(1.dp, AuthColors.FieldBorder),
                            shape = shape
                        )
                        .padding(horizontal = 13.dp),
                    contentAlignment = Alignment.CenterStart
                ) {

                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = AuthColors.TextHint
                        )
                    }

                    innerTextField()
                }
            }
        )
    }
}


// ======================================================
// BOTÕES
// ======================================================

@Composable
internal fun GradientButton(
    text: String,
    onClick: () -> Unit
) {

    val shape = RoundedCornerShape(13.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = AuthColors.PurpleDark.copy(alpha = 0.2f),
                spotColor = AuthColors.PurpleDark.copy(alpha = 0.2f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(AuthColors.Purple, AuthColors.Blue)
                )
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
internal fun OutlinedAuthButton(
    text: String,
    onClick: () -> Unit
) {

    val shape = RoundedCornerShape(11.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .border(
                border = BorderStroke(1.dp, AuthColors.Purple),
                shape = shape
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = AuthColors.PurpleDark
        )
    }
}


// ======================================================
// SEPARADOR "OU"
// ======================================================

@Composable
internal fun OrDivider() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(AuthColors.Divider)
        )

        Text(
            text = "ou",
            modifier = Modifier.padding(horizontal = 10.dp),
            fontSize = 9.sp,
            color = AuthColors.TextHint
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(AuthColors.Divider)
        )
    }
}
