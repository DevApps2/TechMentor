package com.example.techmentor.ui.mentoria

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.techmentor.data.ApiClient
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha
import com.example.techmentor.ui.theme.ManropeFamily

// Cores do protótipo (as mesmas da Home e do Perfil)
internal object MentoriaCores {
    val Fundo = Color(0xFFF8F8FB)
    val Roxo = Color(0xFF7C3AED)
    val RoxoEscuro = Color(0xFF6D28D9)
    val RoxoClaro = Color(0xFFEDE9FE)
    val TextoEscuro = Color(0xFF17151F)
    val TextoMudo = Color(0xFF6F6A76)
    val Borda = Color(0xFFEEECF2)
    val Estrela = Color(0xFFF59E0B)
}


// ======================================================
// AVATAR (inicial por baixo; a foto, se existir, cobre quando carregar)
// ======================================================

@Composable
internal fun AvatarMentora(
    nome: String,
    fotoUrl: String?,
    tamanho: Dp,
    raio: Dp,
    tamanhoDaInicial: TextUnit
) {

    val inicial = nome.trim().firstOrNull()?.uppercase() ?: "?"

    Box(
        modifier = Modifier
            .size(tamanho)
            .clip(RoundedCornerShape(raio))
            .background(MentoriaCores.RoxoClaro),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = inicial,
            fontFamily = ManropeFamily,
            fontSize = tamanhoDaInicial,
            fontWeight = FontWeight.ExtraBold,
            color = MentoriaCores.RoxoEscuro
        )

        if (!fotoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ApiClient.urlDaFoto(fotoUrl),
                contentDescription = "Foto de ${nome.trim()}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


// ======================================================
// CABEÇALHO COM BOTÃO VOLTAR
// ======================================================

@Composable
internal fun CabecalhoVoltar(
    titulo: String,
    onVoltar: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MentoriaCores.Fundo)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            val forma = RoundedCornerShape(12.dp)

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(forma)
                    .background(Color.White)
                    .border(1.dp, MentoriaCores.Borda, forma)
                    .clickable(role = Role.Button) { onVoltar() }
                    .semantics { contentDescription = "Voltar" },
                contentAlignment = Alignment.Center
            ) {

                // a seta do projeto aponta para a direita; girada fica "voltar"
                IconeLinha(
                    icone = Icone.SETA,
                    cor = MentoriaCores.TextoEscuro,
                    modifier = Modifier.rotate(180f),
                    tamanho = 20.dp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = titulo,
                fontFamily = ManropeFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = MentoriaCores.TextoEscuro
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


// ======================================================
// CARTÃO COM TÍTULO (seções brancas)
// ======================================================

@Composable
internal fun SecaoCartao(
    titulo: String,
    conteudo: @Composable ColumnScope.() -> Unit
) {

    val forma = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(16.dp)
    ) {

        Text(
            text = titulo,
            fontFamily = ManropeFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MentoriaCores.TextoEscuro
        )

        Spacer(modifier = Modifier.height(10.dp))

        conteudo()
    }
}


// ======================================================
// CHIPS
// ======================================================

/** Chip que se toca: filtro de área na lista e horário na tela de agendamento. */
@Composable
internal fun ChipOpcao(
    texto: String,
    selecionado: Boolean,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .clip(forma)
            .background(if (selecionado) MentoriaCores.Roxo else Color.White)
            .border(
                width = 1.dp,
                color = if (selecionado) MentoriaCores.Roxo else Color(0xFFE1DDE7),
                shape = forma
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selecionado) Color.White else Color(0xFF514C58)
        )
    }
}

/** Etiqueta só para mostrar (não se toca): áreas de atuação da mentora. */
@Composable
internal fun TagArea(texto: String) {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MentoriaCores.RoxoClaro)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {

        Text(
            text = texto,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MentoriaCores.RoxoEscuro
        )
    }
}


// ======================================================
// CARREGANDO E ERRO
// ======================================================

@Composable
internal fun EsqueletoBloco(altura: Dp) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(altura)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFECEAF2))
    )
}

@Composable
internal fun EstadoErro(
    mensagem: String,
    onTentarDeNovo: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Ops!",
            fontFamily = ManropeFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF20202A)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = mensagem,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF797486)
        )

        Spacer(modifier = Modifier.height(24.dp))

        GradientButton(
            text = "Tentar novamente",
            onClick = onTentarDeNovo
        )
    }
}
