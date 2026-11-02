package com.example.techmentor.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay

/**
 * Chama aoAtualizar de tempos em tempos, mas só enquanto a tela está visível:
 * ao minimizar o app ou abrir outra tela por cima, o relógio para; ao voltar, recomeça.
 * Quem usa já recarrega ao voltar (ON_RESUME), então este relógio só cuida do que acontece com a tela aberta.
 */
@Composable
fun AtualizarAutomaticamente(
    intervaloSegundos: Long = 30,
    aoAtualizar: () -> Unit
) {

    // Sempre chama a versão mais recente da função, sem reiniciar o relógio a cada recomposição
    val atualizar by rememberUpdatedState(aoAtualizar)

    var naTela by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { naTela = true }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { naTela = false }

    LaunchedEffect(naTela, intervaloSegundos) {
        if (naTela) {
            while (true) {
                delay(intervaloSegundos * 1000)
                atualizar()
            }
        }
    }
}