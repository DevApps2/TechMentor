package com.example.techmentor.ui.avaliacao

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily

// valor que a API espera, e o texto que a pessoa lê
private val MOTIVOS = listOf(
    "COMPORTAMENTO_INADEQUADO" to "Linguagem ofensiva ou desrespeitosa",
    "ASSEDIO" to "Assédio ou intimidação",
    "DISCURSO_DE_ODIO" to "Discurso de ódio",
    "OUTRO" to "Outro motivo"
)

private const val LIMITE_DO_DETALHE = 500

/** Escolher o motivo (e, se quiser, explicar) para denunciar o comentário que escreveram sobre a pessoa. */
@Composable
fun DialogoDeDenunciaDeComentario(
    enviando: Boolean,
    erro: String?,
    onEnviar: (motivo: String, detalhe: String) -> Unit,
    onCancelar: () -> Unit
) {

    var motivo by remember { mutableStateOf(MOTIVOS.first().first) }
    var detalhe by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!enviando) onCancelar() },
        containerColor = Color.White,
        title = {
            Text(
                text = "Denunciar comentário",
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                color = MentoriaCores.TextoEscuro
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                Text(
                    text = "Nossa equipe vai ler o comentário e decidir se ele deve ser escondido. A nota continua valendo.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MentoriaCores.TextoMudo
                )

                Spacer(modifier = Modifier.height(12.dp))

                MOTIVOS.forEach { (valor, rotulo) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !enviando) { motivo = valor },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = motivo == valor,
                            onClick = { motivo = valor },
                            enabled = !enviando,
                            colors = RadioButtonDefaults.colors(selectedColor = MentoriaCores.Roxo)
                        )
                        Text(
                            text = rotulo,
                            fontSize = 14.sp,
                            color = MentoriaCores.TextoEscuro
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = detalhe,
                    onValueChange = { if (it.length <= LIMITE_DO_DETALHE) detalhe = it },
                    enabled = !enviando,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Quer explicar? (opcional)") },
                    minLines = 2,
                    maxLines = 4
                )

                if (erro != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = erro,
                        fontSize = 13.sp,
                        color = AuthColors.Error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !enviando,
                onClick = { onEnviar(motivo, detalhe) }
            ) {
                Text(
                    text = if (enviando) "Enviando..." else "Enviar denúncia",
                    fontWeight = FontWeight.Bold,
                    color = AuthColors.Error
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !enviando, onClick = onCancelar) {
                Text(
                    text = "Cancelar",
                    fontWeight = FontWeight.Bold,
                    color = MentoriaCores.TextoMudo
                )
            }
        }
    )
}