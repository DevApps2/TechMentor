package com.example.techmentor.ui.notificacoes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha

/** O sino do topo, com a bolinha de não lidas. O número se atualiza toda vez que a tela volta a aparecer. */
@Composable
fun SinoDeNotificacoes(
    onClick: () -> Unit,
    viewModel: SinoViewModel = viewModel()
) {

    val naoLidas by viewModel.naoLidas.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.atualizar()
    }

    AtualizarAutomaticamente { viewModel.atualizar() }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .semantics { contentDescription = "Notificações" }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {

        IconeLinha(
            icone = Icone.SINO,
            cor = Color(0xFF20202A),
            tamanho = 24.dp
        )

        if (naoLidas > 0) {

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 0.dp)
                    .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = if (naoLidas > 99) "99+" else naoLidas.toString(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}