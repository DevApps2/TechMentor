package com.example.techmentor.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.techmentor.data.AgendamentoResponse
import com.example.techmentor.ui.components.BotaoEntrarNaSala
import com.example.techmentor.ui.components.BotaoSecundario
import com.example.techmentor.ui.components.SeloDeStatus


private val RoxoDoCartao = Color(0xFF7C3AED)
private val AzulDoCartao = Color(0xFF2563EB)
private val VermelhoDoCartao = Color(0xFFDC2626)

/**
 * O cartão da próxima mentoria da aluna, tudo num lugar só: quem é a mentora, quando é, a situação
 * (aguardando ou confirmada), o botão da sala e as ações Reagendar e Cancelar. Sem mentoria, não mostra nada.
 */
@Composable
fun CartaoDaProximaMentoria(
    proxima: AgendamentoResponse?,
    cancelando: Boolean,
    erro: String?,
    navController: NavController,
    onCancelar: (agendamentoId: Long) -> Unit
) {

    if (proxima == null) return

    var confirmando by remember { mutableStateOf(false) }
    val aguardando = proxima.status == "AGENDADA"

    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            title = { Text("Cancelar esta mentoria?") },
            text = { Text("${proxima.mentoraNome} será avisada e o horário volta a ficar livre.") },
            confirmButton = {
                TextButton(onClick = {
                    onCancelar(proxima.id)
                    confirmando = false
                }) { Text("Cancelar mentoria", color = VermelhoDoCartao) }
            },
            dismissButton = {
                TextButton(onClick = { confirmando = false }) { Text("Voltar") }
            }
        )
    }

    val forma = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, Color(0xFFECE9F1), forma)
            .padding(14.dp)
    ) {

        // QUEM E QUANDO
        Row(verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), AzulDoCartao))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iniciais(proxima.mentoraNome),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = "Mentoria com ${proxima.mentoraNome}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF20202A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = descricaoDaMentoria(proxima),
                    fontSize = 11.sp,
                    color = Color(0xFF625D6C)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SITUAÇÃO
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

            SeloDeStatus(
                texto = if (aguardando) "Aguardando confirmação da mentora" else "Confirmada",
                fundo = if (aguardando) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                cor = if (aguardando) Color(0xFFB45309) else Color(0xFF15803D)
            )

            if (!proxima.linkSala.isNullOrBlank()) {
                SeloDeStatus(
                    texto = "Google Meet",
                    fundo = Color(0xFFEDE9FE),
                    cor = Color(0xFF6D28D9)
                )
            }
        }

        // SALA (só aparece com a mentoria confirmada)
        BotaoEntrarNaSala(
            status = proxima.status,
            inicio = proxima.inicio,
            fim = proxima.fim,
            linkSala = proxima.linkSala,
            minutosAntes = proxima.minutosParaAbrirSala
        )

        Spacer(modifier = Modifier.height(12.dp))

        // AÇÕES
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            BotaoSecundario(
                texto = "Reagendar",
                cor = RoxoDoCartao,
                habilitado = !cancelando,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate("reagendar/${proxima.id}/${proxima.mentoraId}") }
            )

            BotaoSecundario(
                texto = if (cancelando) "Cancelando..." else "Cancelar",
                cor = VermelhoDoCartao,
                habilitado = !cancelando,
                modifier = Modifier.weight(1f),
                onClick = { confirmando = true }
            )
        }

        if (erro != null) {

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = erro, fontSize = 11.sp, color = VermelhoDoCartao)
        }
    }
}

/** "Maria Silva" -> "MS"; um nome só -> as duas primeiras letras. */
private fun iniciais(nome: String): String {
    val partes = nome.trim().split(" ").filter { it.isNotBlank() }
    return when {
        partes.isEmpty() -> "?"
        partes.size == 1 -> partes[0].take(2).uppercase()
        else -> "${partes.first().first()}${partes.last().first()}".uppercase()
    }
}
