package com.example.techmentor.ui.oportunidades

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

private val LOCALE_BR: Locale = Locale("pt", "BR")

private val DegradeRoxoAzul = Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB)))

/** "2026-11-18" -> "18 de novembro de 2026" */
private fun formatarPrazoCompleto(prazo: String?): String? {

    if (prazo.isNullOrBlank()) return null

    return try {
        val data = SimpleDateFormat("yyyy-MM-dd", LOCALE_BR).parse(prazo) ?: return prazo
        SimpleDateFormat("d 'de' MMMM 'de' yyyy", LOCALE_BR).format(data)
    } catch (e: ParseException) {
        prazo
    }
}


// ======================================================
// ENTRADA: detalhes -> confirmação
// ======================================================

@Composable
internal fun OportunidadeDetalhe(
    oportunidade: OportunidadeResponse,
    onVoltar: () -> Unit
) {

    // O botão é só uma simulação (o Figma avisa que nenhum envio real é feito)
    var confirmado by rememberSaveable(oportunidade.id) { mutableStateOf(false) }

    if (confirmado) {

        // Voltar do celular na confirmação volta para os detalhes
        BackHandler { confirmado = false }

        ConfirmacaoInteresse(
            onVoltarAosDetalhes = { confirmado = false },
            onVoltarAsOportunidades = onVoltar
        )

    } else {

        DetalheDaOportunidade(
            oportunidade = oportunidade,
            onVoltar = onVoltar,
            onSimular = { confirmado = true }
        )
    }
}


// ======================================================
// CABEÇALHO: [<]  Oportunidade
// ======================================================

@Composable
private fun CabecalhoOportunidade(onVoltar: () -> Unit) {

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
                text = "Oportunidade",
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


// ======================================================
// DETALHES
// ======================================================

@Composable
private fun DetalheDaOportunidade(
    oportunidade: OportunidadeResponse,
    onVoltar: () -> Unit,
    onSimular: () -> Unit
) {

    val estilo = estiloDoTipo(oportunidade.tipo)
    val ehEvento = oportunidade.tipo.equals("EVENTO", ignoreCase = true)

    val textoDoBotao = if (ehEvento) "Simular inscrição" else "Simular candidatura"

    val modalidade = oportunidade.localidade?.takeIf { it.isNotBlank() }
    val prazo = formatarPrazoCompleto(oportunidade.prazoInscricao)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        CabecalhoOportunidade(onVoltar = onVoltar)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {

            // Ex.: "ESTÁGIO · Programação"
            Text(
                text = listOfNotNull(estilo.rotulo, oportunidade.area?.takeIf { it.isNotBlank() })
                    .joinToString("  ·  "),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = MentoriaCores.Roxo
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = oportunidade.titulo,
                fontFamily = ManropeFamily,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.6).sp,
                color = MentoriaCores.TextoEscuro
            )

            // Ex.: "Nexa Tecnologia · Remoto"
            val subtitulo = listOfNotNull(
                oportunidade.organizacao?.takeIf { it.isNotBlank() },
                modalidade
            ).joinToString("  ·  ")

            if (subtitulo.isNotEmpty()) {

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = subtitulo,
                    fontSize = 17.sp,
                    color = MentoriaCores.TextoMudo
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            AvisoDemonstracao()

            BlocoDeTexto(titulo = "Sobre a oportunidade", texto = oportunidade.descricao)
            BlocoDeTexto(titulo = "Requisitos", texto = oportunidade.requisitos)
            BlocoDeTexto(titulo = "Benefícios", texto = oportunidade.beneficios)

            Spacer(modifier = Modifier.height(14.dp))

            // Modalidade | Prazo demonstrativo
            val formaCard = RoundedCornerShape(24.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(formaCard)
                    .background(Color.White)
                    .border(1.dp, MentoriaCores.Borda, formaCard)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {

                InfoCurta(
                    rotulo = "Modalidade",
                    valor = modalidade ?: "Não informada",
                    modifier = Modifier.weight(1f)
                )

                InfoCurta(
                    rotulo = "Prazo demonstrativo",
                    valor = prazo ?: "Sem prazo",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            BotaoDegrade(texto = textoDoBotao, onClick = onSimular)
        }
    }
}


// ======================================================
// CONFIRMAÇÃO: "Interesse registrado"
// ======================================================

@Composable
private fun ConfirmacaoInteresse(
    onVoltarAosDetalhes: () -> Unit,
    onVoltarAsOportunidades: () -> Unit
) {

    val formaIcone = RoundedCornerShape(36.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        CabecalhoOportunidade(onVoltar = onVoltarAosDetalhes)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(118.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = formaIcone,
                        ambientColor = Color(0x667C3AED),
                        spotColor = Color(0x667C3AED)
                    )
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
                text = "Interesse registrado",
                fontFamily = ManropeFamily,
                fontSize = 34.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.6).sp,
                textAlign = TextAlign.Center,
                color = MentoriaCores.TextoEscuro
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Esta é uma confirmação demonstrativa. Nenhuma candidatura ou inscrição foi enviada.",
                fontSize = 18.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                color = MentoriaCores.TextoMudo
            )

            Spacer(modifier = Modifier.height(20.dp))

            BotaoDegrade(texto = "Voltar às oportunidades", onClick = onVoltarAsOportunidades)
        }
    }
}


// ======================================================
// PEDAÇOS REUTILIZADOS
// ======================================================

@Composable
private fun BotaoDegrade(texto: String, onClick: () -> Unit) {

    val forma = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(DegradeRoxoAzul)
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

@Composable
private fun AvisoDemonstracao() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF1EEFB))
    ) {

        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(MentoriaCores.Roxo)
        )

        Text(
            text = "Dados fictícios para demonstração do protótipo. Nenhuma conexão ou envio real é realizado. Organização fictícia, sem parceria real.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MentoriaCores.TextoMudo
        )
    }
}

/** Card branco com título e texto. Se o texto vier vazio do servidor, o card nem aparece. */
@Composable
private fun BlocoDeTexto(titulo: String, texto: String?) {

    if (texto.isNullOrBlank()) return

    val forma = RoundedCornerShape(24.dp)

    Spacer(modifier = Modifier.height(14.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = titulo,
            fontFamily = ManropeFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MentoriaCores.TextoEscuro
        )

        Text(
            text = texto,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = MentoriaCores.TextoMudo
        )
    }
}

@Composable
private fun InfoCurta(rotulo: String, valor: String, modifier: Modifier = Modifier) {

    Column(modifier = modifier) {

        Text(
            text = rotulo,
            fontSize = 14.sp,
            color = MentoriaCores.TextoMudo
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = valor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MentoriaCores.TextoEscuro
        )
    }
}
