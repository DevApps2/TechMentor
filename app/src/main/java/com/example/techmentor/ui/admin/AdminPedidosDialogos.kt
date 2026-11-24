package com.example.techmentor.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.techmentor.data.AdminPedidoParceriaResponse
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha

private val PedidoFundo = Color(0xFFF8F8FB)
private val PedidoTinta = Color(0xFF20202A)
private val PedidoCinza = Color(0xFF77717E)
private val PedidoVermelho = Color(0xFFB42336)

@Composable
internal fun CartaoPedido(pedido: AdminPedidoParceriaResponse, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Color.White)
            .clickable(role = Role.Button, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconeLinha(Icone.PREDIO, AuthColors.Purple, tamanho = 28.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(pedido.nome, color = PedidoTinta, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${pedido.responsavel} · ${pedido.email}", color = PedidoCinza, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Enviado em ${dataCurta(pedido.criadoEm)}", color = PedidoCinza, fontSize = 10.sp)
        }
        SeloSituacaoPedido(pedido.situacao)
    }
}

@Composable
private fun SeloSituacaoPedido(situacao: String) {
    val (texto, cor, fundo) = when (situacao) {
        "APROVADO" -> Triple("Aprovado", Color(0xFF23865D), Color(0xFFE2F5EC))
        "RECUSADO" -> Triple("Recusado", PedidoVermelho, Color(0xFFFFE9EB))
        else -> Triple("Pendente", Color(0xFF9A620C), Color(0xFFFFF2D9))
    }
    Text(texto, color = cor, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(fundo).padding(horizontal = 8.dp, vertical = 5.dp))
}

@Composable
internal fun DialogoDetalhePedido(
    pedido: AdminPedidoParceriaResponse,
    enviando: Boolean,
    erro: String?,
    aoAprovar: () -> Unit,
    aoRecusar: () -> Unit,
    aoApagar: () -> Unit,
    aoFechar: () -> Unit
) {
    val pendente = pedido.situacao == "PENDENTE"
    AlertDialog(
        onDismissRequest = { if (!enviando) aoFechar() },
        title = { Text(pedido.nome, color = PedidoTinta, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SeloSituacaoPedido(pedido.situacao)
                Campo("Responsável", pedido.responsavel)
                Campo("E-mail", pedido.email)
                Campo("Tipo", tipoLegivel(pedido.tipo))
                Campo("Documento", pedido.documento ?: "Não informado")
                Campo("Site", pedido.site ?: "Não informado")
                Campo("Domínio do e-mail", when (pedido.dominioDoEmailConfereComSite) {
                    true -> "Confere com o site"
                    false -> "Não confere ou é e-mail pessoal"
                    null -> "Sem site informado"
                })
                Campo("Mensagem", pedido.mensagem)
                Campo("Enviado em", dataCurta(pedido.criadoEm))
                Campo("Política de privacidade", "Aceita em ${dataCurta(pedido.consentimentoEm)}${pedido.politicaVersao?.let { " · versão $it" } ?: ""}")
                if (!pendente) {
                    Campo("Decisão", "${pedido.decididoPor ?: "—"} · ${dataCurta(pedido.decididoEm)}")
                    pedido.motivoDaRecusa?.let { Campo("Motivo da recusa", it) }
                    if (pedido.situacao == "APROVADO") Text("Já virou uma parceira; acompanhe em Parceiras.", color = PedidoCinza, fontSize = 11.sp)
                }

                if (pendente) {
                    TextButton(enabled = !enviando, onClick = aoAprovar) {
                        Text(if (enviando) "Enviando…" else "Aprovar e enviar convite", color = AuthColors.Purple, fontWeight = FontWeight.Bold)
                    }
                    TextButton(enabled = !enviando, onClick = aoRecusar) { Text("Recusar", color = PedidoVermelho) }
                }
                if (pedido.situacao != "APROVADO") {
                    TextButton(enabled = !enviando, onClick = aoApagar) { Text("Apagar os dados deste pedido", color = PedidoVermelho) }
                }
                erro?.let { Text(it, color = PedidoVermelho, fontSize = 11.sp) }
            }
        },
        confirmButton = { TextButton(onClick = aoFechar) { Text("Fechar", color = AuthColors.Purple) } },
        containerColor = Color.White,
        titleContentColor = PedidoTinta,
        textContentColor = PedidoTinta,
        tonalElevation = 0.dp
    )
}

@Composable
internal fun DialogoRecusarPedido(
    nome: String,
    enviando: Boolean,
    erro: String?,
    aoRecusar: (String) -> Unit,
    aoFechar: () -> Unit
) {
    var motivo by remember(nome) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!enviando) aoFechar() },
        title = { Text("Recusar o pedido de $nome?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A instituição recebe este motivo por e-mail. Escreva de um jeito gentil e claro.", color = PedidoCinza, fontSize = 12.sp)
                OutlinedTextField(motivo, { motivo = it.take(500) }, label = { Text("Motivo") }, minLines = 3, colors = coresCampoPedido(), modifier = Modifier.fillMaxWidth())
                erro?.let { Text(it, color = PedidoVermelho, fontSize = 11.sp) }
            }
        },
        confirmButton = {
            TextButton(enabled = !enviando && motivo.isNotBlank(), onClick = { aoRecusar(motivo.trim()) }) {
                Text(if (enviando) "Enviando…" else "Recusar", color = PedidoVermelho)
            }
        },
        dismissButton = { TextButton(enabled = !enviando, onClick = aoFechar) { Text("Cancelar", color = AuthColors.Purple) } },
        containerColor = Color.White,
        titleContentColor = PedidoTinta,
        textContentColor = PedidoTinta,
        tonalElevation = 0.dp
    )
}

@Composable
private fun Campo(rotulo: String, valor: String) {
    Column(Modifier.padding(vertical = 2.dp)) {
        Text(rotulo, color = PedidoCinza, fontSize = 10.sp)
        Text(valor, color = PedidoTinta, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun coresCampoPedido() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = PedidoTinta,
    unfocusedTextColor = PedidoTinta,
    disabledTextColor = PedidoCinza,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    cursorColor = AuthColors.Purple,
    focusedBorderColor = AuthColors.Purple,
    unfocusedBorderColor = Color(0xFFB8B2BD),
    focusedLabelColor = AuthColors.Purple,
    unfocusedLabelColor = PedidoCinza
)

private fun tipoLegivel(tipo: String): String = when (tipo) {
    "EMPRESA" -> "Empresa"
    "ONG" -> "ONG"
    "INDEPENDENTE" -> "Independente"
    else -> tipo
}

// "2026-11-23T14:05:00" -> "23/11/2026 14:05"
private fun dataCurta(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    val partes = iso.split('T')
    val dia = partes[0].split('-')
    if (dia.size != 3) return iso
    val hora = partes.getOrNull(1)?.take(5)
    return "${dia[2]}/${dia[1]}/${dia[0]}" + (hora?.let { " $it" } ?: "")
}

@Composable
internal fun DialogoApagarPedido(
    nome: String,
    enviando: Boolean,
    erro: String?,
    aoApagar: () -> Unit,
    aoFechar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!enviando) aoFechar() },
        title = { Text("Apagar este pedido?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Os dados de $nome (nome, e-mail, responsável e mensagem) serão apagados e não dá para desfazer. O histórico guarda só o número do pedido.", color = PedidoTinta, fontSize = 13.sp)
                erro?.let { Text(it, color = PedidoVermelho, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(enabled = !enviando, onClick = aoApagar) {
                Text(if (enviando) "Apagando…" else "Apagar", color = PedidoVermelho)
            }
        },
        dismissButton = { TextButton(enabled = !enviando, onClick = aoFechar) { Text("Cancelar", color = AuthColors.Purple) } },
        containerColor = Color.White,
        titleContentColor = PedidoTinta,
        textContentColor = PedidoTinta,
        tonalElevation = 0.dp
    )
}

