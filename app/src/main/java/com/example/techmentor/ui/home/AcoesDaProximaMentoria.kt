package com.example.techmentor.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.techmentor.data.AgendamentoResponse

private val RoxoDasAcoes = Color(0xFF7C3AED)
private val VermelhoDasAcoes = Color(0xFFDC2626)

/**
 * Logo abaixo do card da próxima mentoria: a situação (aguardando ou confirmada)
 * e os botões Reagendar e Cancelar. Se não há mentoria, não mostra nada.
 */
@Composable
fun AcoesDaProximaMentoria(
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
                }) { Text("Cancelar mentoria", color = VermelhoDasAcoes) }
            },
            dismissButton = {
                TextButton(onClick = { confirmando = false }) { Text("Voltar") }
            }
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Situação: a mentora ainda vai confirmar, ou já confirmou
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(if (aguardando) Color(0xFFFEF3C7) else Color(0xFFDCFCE7))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {

            Text(
                text = if (aguardando) "Aguardando confirmação da mentora" else "Confirmada",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (aguardando) Color(0xFFB45309) else Color(0xFF15803D)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            AcaoPequena(
                texto = "Reagendar",
                cor = RoxoDasAcoes,
                habilitado = !cancelando,
                onClick = { navController.navigate("reagendar/${proxima.id}/${proxima.mentoraId}") }
            )

            AcaoPequena(
                texto = if (cancelando) "Cancelando..." else "Cancelar",
                cor = VermelhoDasAcoes,
                habilitado = !cancelando,
                onClick = { confirmando = true }
            )
        }

        if (erro != null) {
            Text(text = erro, fontSize = 11.sp, color = VermelhoDasAcoes)
        }
    }
}

@Composable
private fun AcaoPequena(
    texto: String,
    cor: Color,
    habilitado: Boolean,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .clip(forma)
            .background(Color.White)
            .border(1.dp, cor, forma)
            .clickable(enabled = habilitado, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = texto,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = cor
        )
    }
}