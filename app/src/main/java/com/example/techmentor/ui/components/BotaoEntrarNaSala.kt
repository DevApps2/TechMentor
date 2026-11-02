package com.example.techmentor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.techmentor.ui.home.parseDataHora
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// A sala abre este tanto de minutos antes do começo da mentoria
private const val MINUTOS_ANTES = 15L
private const val DOZE_HORAS = 12 * 60 * 60 * 1000L

/**
 * Botão da sala do Google Meet. Só aparece com a mentoria CONFIRMADA e até o fim dela.
 * Fica desativado até 15 min antes do começo; a tela reavalia o horário sozinha a cada 30 s.
 */
@Composable
fun BotaoEntrarNaSala(
    status: String,
    inicio: String,
    fim: String,
    linkSala: String?
) {

    if (status != "CONFIRMADA") return

    val comeco = parseDataHora(inicio) ?: return
    val termino = parseDataHora(fim) ?: return

    val agora by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }

    // Passou do fim: a sala some da tela
    if (agora >= termino.time) return

    val uriHandler = LocalUriHandler.current
    val liberaEm = comeco.time - MINUTOS_ANTES * 60_000
    val liberada = agora >= liberaEm

    val legenda = when {
        liberada -> "A sala já está aberta"
        liberaEm - agora > DOZE_HORAS -> "Libera $MINUTOS_ANTES min antes da mentoria"
        else -> "Libera às " + SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(liberaEm))
    }

    Column(modifier = Modifier.padding(top = 8.dp)) {

        if (linkSala.isNullOrBlank()) {

            // O Google ainda não devolveu o link; ele chega sozinho (puxe a tela para atualizar)
            Text(
                text = "A sala de vídeo está sendo preparada.",
                fontSize = 11.sp,
                color = Color(0xFF85808C)
            )

        } else {

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (liberada) Color(0xFF7C3AED) else Color(0xFFEDEBF1))
                        .clickable(enabled = liberada, role = Role.Button) {
                            runCatching { uriHandler.openUri(linkSala) }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "Entrar na sala",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (liberada) Color.White else Color(0xFF9A96A3)
                    )
                }

                Text(
                    text = legenda,
                    fontSize = 10.sp,
                    color = Color(0xFF85808C)
                )
            }
        }
    }
}