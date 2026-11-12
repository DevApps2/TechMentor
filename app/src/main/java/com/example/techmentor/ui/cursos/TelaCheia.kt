package com.example.techmentor.ui.cursos

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Mostra a vista de vídeo que o player entregou para a tela cheia, por cima de tudo: gira o celular para deitado,
 * esconde as barras do sistema e ocupa a tela inteira. O botão "voltar" (ou o botão de sair do player) chama [aoSair].
 */
@Composable
internal fun TelaCheiaDeVideo(
    conteudo: View,
    aoSair: () -> Unit
) {

    // Deita o celular enquanto estiver em tela cheia (e desfaz ao sair)
    EfeitoDeTelaCheia(ativa = true)

    Dialog(
        onDismissRequest = aoSair,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {

        val janela = (LocalView.current.parent as? DialogWindowProvider)?.window

        SideEffect {
            janela?.let { w ->
                w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                WindowCompat.setDecorFitsSystemWindows(w, false)
                WindowCompat.getInsetsController(w, w.decorView).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            factory = { contexto -> FrameLayout(contexto).apply { setBackgroundColor(android.graphics.Color.BLACK) } },
            update = { moldura ->
                if (moldura.getChildAt(0) !== conteudo) {
                    moldura.removeAllViews()
                    (conteudo.parent as? ViewGroup)?.removeView(conteudo)
                    moldura.addView(
                        conteudo,
                        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    )
                }
            }
        )
    }
}
