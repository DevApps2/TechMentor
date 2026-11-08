package com.example.techmentor.ui.oportunidades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.techmentor.data.OportunidadeResponse
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.components.AvisoDemo
import com.example.techmentor.ui.home.BottomNavigationBar
import com.example.techmentor.ui.mentoria.CabecalhoVoltar
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.platform.LocalUriHandler
import com.example.techmentor.ui.auth.OutlinedAuthButton

private val LOCALE_BR: Locale = Locale.forLanguageTag("pt-BR")


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

private fun rotuloDoStatus(status: String): String = when (status) {
    "ENVIADA" -> "Enviada"
    "EM_ANALISE" -> "Em análise"
    "APROVADA" -> "Aprovada"
    "RECUSADA" -> "Recusada"
    else -> status
}


// ======================================================
// ENTRADA: detalhes -> confirmação
// ======================================================

@Composable
internal fun OportunidadeDetalhe(
    navController: NavController,
    oportunidade: OportunidadeResponse,
    // status da participação da usuária (ENVIADA, EM_ANALISE...) ou null se ainda não participou
    statusParticipacao: String?,
    enviando: Boolean,
    erroEnvio: String?,
    confirmada: Boolean,
    onParticipar: () -> Unit,
    onVoltarAosDetalhes: () -> Unit,
    onVoltar: () -> Unit,
    onVoltarAsOportunidades: () -> Unit
) {

    if (confirmada) {

        ConfirmacaoEnvio(
            navController = navController,
            oportunidade = oportunidade,
            onVoltarAosDetalhes = onVoltarAosDetalhes,
            onVoltarAsOportunidades = onVoltarAsOportunidades
        )

    } else {

        DetalheDaOportunidade(
            navController = navController,
            oportunidade = oportunidade,
            statusParticipacao = statusParticipacao,
            enviando = enviando,
            erroEnvio = erroEnvio,
            onVoltar = onVoltar,
            onParticipar = onParticipar
        )
    }
}


// ======================================================
// DETALHES
// ======================================================

@Composable
private fun DetalheDaOportunidade(
    navController: NavController,
    oportunidade: OportunidadeResponse,
    statusParticipacao: String?,
    enviando: Boolean,
    erroEnvio: String?,
    onVoltar: () -> Unit,
    onParticipar: () -> Unit
) {

    val estilo = estiloDoTipo(oportunidade.tipo)
    val ehEvento = oportunidade.tipo.equals("EVENTO", ignoreCase = true)

    val modalidade = oportunidade.localidade?.takeIf { it.isNotBlank() }
    val prazo = formatarPrazoCompleto(oportunidade.prazoInscricao)
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        CabecalhoVoltar(
            titulo = "Oportunidade",
            onVoltar = onVoltar
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Column {

                // Ex.: "ESTÁGIO · Programação"
                Text(
                    text = listOfNotNull(estilo.rotulo, oportunidade.area?.takeIf { it.isNotBlank() })
                        .joinToString("  ·  "),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = MentoriaCores.Roxo
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = oportunidade.titulo,
                    fontFamily = ManropeFamily,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    color = MentoriaCores.TextoEscuro
                )

                // Ex.: "Nexa Tecnologia · Remoto"
                val subtitulo = listOfNotNull(
                    oportunidade.organizacao?.takeIf { it.isNotBlank() },
                    modalidade
                ).joinToString("  ·  ")

                if (subtitulo.isNotEmpty()) {

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = subtitulo,
                        fontSize = 13.sp,
                        color = MentoriaCores.TextoMudo
                    )
                }
            }

            AvisoDemo(
                texto = "Vaga e organização fictícias, sem parceria real. Sua participação fica registrada apenas aqui, no TechMentor."
            )

            BlocoDeTexto(titulo = "Sobre a oportunidade", texto = oportunidade.descricao)
            BlocoDeTexto(titulo = "Requisitos", texto = oportunidade.requisitos)
            BlocoDeTexto(titulo = "Benefícios", texto = oportunidade.beneficios)

            // Modalidade | Prazo de inscrição
            val forma = RoundedCornerShape(17.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(forma)
                    .background(Color.White)
                    .border(1.dp, MentoriaCores.Borda, forma)
                    .padding(14.dp)
            ) {

                InfoCurta(
                    rotulo = "Modalidade",
                    valor = modalidade ?: "Não informada",
                    modifier = Modifier.weight(1f)
                )

                InfoCurta(
                    rotulo = "Prazo de inscrição",
                    valor = prazo ?: "Sem prazo",
                    modifier = Modifier.weight(1f)
                )
            }

            // Link da vaga no site da organização (só aparece se a admin preencheu). Só abre http/https.
            val link = oportunidade.linkExterno?.trim()?.takeIf { it.startsWith("http", ignoreCase = true) }

            if (link != null) {
                OutlinedAuthButton(
                    text = "Ver no site da organização",
                    onClick = { runCatching { uriHandler.openUri(link) } }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (statusParticipacao != null) {

                JaParticipa(
                    texto = if (ehEvento) "Inscrição confirmada" else "Candidatura enviada",
                    status = rotuloDoStatus(statusParticipacao)
                )

            } else {

                if (erroEnvio != null) {
                    Text(
                        text = erroEnvio,
                        fontSize = 11.sp,
                        color = AuthColors.Error
                    )
                }

                GradientButton(
                    text = when {
                        enviando -> "Enviando..."
                        ehEvento -> "Inscrever-me"
                        else -> "Candidatar-me"
                    },
                    onClick = { if (!enviando) onParticipar() }
                )
            }
        }

        BottomNavigationBar(
            navController = navController,
            rotaAtual = "opportunities"
        )
    }
}


// ======================================================
// CONFIRMAÇÃO
// ======================================================

@Composable
private fun ConfirmacaoEnvio(
    navController: NavController,
    oportunidade: OportunidadeResponse,
    onVoltarAosDetalhes: () -> Unit,
    onVoltarAsOportunidades: () -> Unit
) {

    val ehEvento = oportunidade.tipo.equals("EVENTO", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        CabecalhoVoltar(
            titulo = "Oportunidade",
            onVoltar = onVoltarAosDetalhes
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
                text = if (ehEvento) "Inscrição confirmada!" else "Candidatura enviada!",
                fontFamily = ManropeFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                textAlign = TextAlign.Center,
                color = MentoriaCores.TextoEscuro
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Registramos ${if (ehEvento) "sua inscrição" else "sua candidatura"} em ${oportunidade.titulo}. " +
                    "A organização é fictícia, então nenhuma mensagem é enviada a ela.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF797486)
            )

            Spacer(modifier = Modifier.height(24.dp))

            GradientButton(
                text = "Voltar às oportunidades",
                onClick = onVoltarAsOportunidades
            )
        }

        BottomNavigationBar(
            navController = navController,
            rotaAtual = "opportunities"
        )
    }
}


// ======================================================
// PEDAÇOS REUTILIZADOS
// ======================================================

/** No lugar do botão, quando a usuária já participou desta oportunidade. */
@Composable
private fun JaParticipa(texto: String, status: String) {

    val forma = RoundedCornerShape(13.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(forma)
            .background(Color(0xFFDCFCE7)),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = "✓ $texto  ·  $status",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF15803D)
        )
    }
}

/** Card branco com título e texto. Se o texto vier vazio do servidor, o card nem aparece. */
@Composable
private fun BlocoDeTexto(titulo: String, texto: String?) {

    if (texto.isNullOrBlank()) return

    val forma = RoundedCornerShape(17.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        Text(
            text = titulo,
            fontFamily = ManropeFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MentoriaCores.TextoEscuro
        )

        Text(
            text = texto,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MentoriaCores.TextoMudo
        )
    }
}

@Composable
private fun InfoCurta(rotulo: String, valor: String, modifier: Modifier = Modifier) {

    Column(modifier = modifier) {

        Text(
            text = rotulo,
            fontSize = 10.sp,
            color = Color(0xFF98939D)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = valor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MentoriaCores.TextoEscuro
        )
    }
}
