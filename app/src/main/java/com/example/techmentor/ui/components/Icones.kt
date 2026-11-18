package com.example.techmentor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Ícones de linha do protótipo (grade 24x24, traço 1,8, pontas arredondadas). */
enum class Icone {
    USUARIOS,
    SETA,
    CODIGO,
    PLAY,
    LIVRO,
    MALETA,
    ESCUDO,
    CASA,
    BUSCA,
    TROFEU,
    PERFIL,
    SINO,
    PREDIO,
    CHAPEU,
    ESTATISTICAS,
    CONFIGURACOES,
    CHECK,
    RELOGIO,
    ATUALIZAR
}

@Composable
fun IconeLinha(
    icone: Icone,
    cor: Color,
    modifier: Modifier = Modifier,
    tamanho: Dp = 22.dp,
    espessura: Float = 1.8f
) {

    Canvas(
        modifier = modifier.size(tamanho)
    ) {

        val estilo = Stroke(
            width = espessura,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // Os desenhos foram feitos numa grade de 24 unidades; aqui escalamos para o tamanho pedido
        scale(scale = size.minDimension / 24f, pivot = Offset.Zero) {

            when (icone) {

                Icone.USUARIOS -> {
                    drawCircle(cor, radius = 3f, center = Offset(9f, 8f), style = estilo)
                    tracar("M3 20a6 6 0 0 1 12 0M16 4a3 3 0 0 1 0 6M17 14a5 5 0 0 1 4 5", cor, estilo)
                }

                Icone.SETA -> tracar("m9 18 6-6-6-6", cor, estilo)

                Icone.CODIGO -> tracar("m8 9-4 3 4 3M16 9l4 3-4 3M14 5l-4 14", cor, estilo)

                Icone.PLAY -> {
                    tracar("m8 5 11 7-11 7z", cor, estilo)
                    preencher("m8 5 11 7-11 7z", cor)
                }

                Icone.LIVRO -> {
                    tracar("M4 5.5A3.5 3.5 0 0 1 7.5 2H11v18H7.5A3.5 3.5 0 0 0 4 23z", cor, estilo)
                    tracar("M20 5.5A3.5 3.5 0 0 0 16.5 2H13v18h3.5A3.5 3.5 0 0 1 20 23z", cor, estilo)
                }

                Icone.MALETA -> {
                    drawRoundRect(
                        color = cor,
                        topLeft = Offset(3f, 7f),
                        size = Size(18f, 13f),
                        cornerRadius = CornerRadius(2f, 2f),
                        style = estilo
                    )
                    tracar("M8 7V4h8v3M3 12h18M10 12v2h4v-2", cor, estilo)
                }

                Icone.ESCUDO -> {
                    tracar("M12 22s8-3.5 8-10V5l-8-3-8 3v7c0 6.5 8 10 8 10", cor, estilo)
                    tracar("m9 12 2 2 4-5", cor, estilo)
                }

                Icone.CASA -> {
                    tracar("m3 11 9-8 9 8", cor, estilo)
                    tracar("M5 10v10h14V10", cor, estilo)
                    tracar("M9 20v-6h6v6", cor, estilo)
                }

                Icone.BUSCA -> {
                    drawCircle(cor, radius = 7f, center = Offset(11f, 11f), style = estilo)
                    tracar("m20 20-4-4", cor, estilo)
                }

                Icone.TROFEU -> {
                    tracar("M8 21h8M12 17v4M7 4h10v4a5 5 0 0 1-10 0z", cor, estilo)
                    tracar("M7 6H4v2a4 4 0 0 0 4 4M17 6h3v2a4 4 0 0 1-4 4", cor, estilo)
                }

                Icone.PERFIL -> {
                    drawCircle(cor, radius = 4f, center = Offset(12f, 8f), style = estilo)
                    tracar("M4 21a8 8 0 0 1 16 0", cor, estilo)
                }

                Icone.SINO -> {
                    tracar("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", cor, estilo)
                    tracar("M10.3 21a1.94 1.94 0 0 0 3.4 0", cor, estilo)
                }

                Icone.PREDIO -> {
                    tracar("M4 21V5a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v16M2 21h20M8 7h2m3 0h1M8 11h2m3 0h1M8 15h2m3 0h1M17 10h2a2 2 0 0 1 2 2v9", cor, estilo)
                }

                Icone.CHAPEU -> {
                    tracar("m2 10 10-6 10 6-10 6zM6 12v5c3.6 2.8 8.4 2.8 12 0v-5M22 10v6", cor, estilo)
                    drawCircle(cor, radius = 1.1f, center = Offset(22f, 17f))
                }

                Icone.ESTATISTICAS -> {
                    tracar("M4 20V11h4v9M10 20V4h4v16M16 20v-7h4v7", cor, estilo)
                }

                Icone.CONFIGURACOES -> {
                    drawCircle(cor, radius = 3.2f, center = Offset(12f, 12f), style = estilo)
                    tracar("M12 2v3M12 19v3M4.9 4.9l2.1 2.1m10 10 2.1 2.1M2 12h3m14 0h3M4.9 19.1 7 17m10-10 2.1-2.1", cor, estilo)
                }

                Icone.CHECK -> tracar("m5 12 4 4L19 6", cor, estilo)

                Icone.RELOGIO -> {
                    drawCircle(cor, radius = 9f, center = Offset(12f, 12f), style = estilo)
                    tracar("M12 7v5l3 2", cor, estilo)
                }

                Icone.ATUALIZAR -> tracar("M20 7v5h-5M4 17v-5h5M5.6 9a7 7 0 0 1 11.8-2L20 12M4 12l2.6 5a7 7 0 0 0 11.8-2", cor, estilo)
            }
        }
    }
}

private fun DrawScope.tracar(caminho: String, cor: Color, estilo: Stroke) {
    drawPath(PathParser().parsePathString(caminho).toPath(), cor, style = estilo)
}

private fun DrawScope.preencher(caminho: String, cor: Color) {
    drawPath(PathParser().parsePathString(caminho).toPath(), cor, style = Fill)
}
