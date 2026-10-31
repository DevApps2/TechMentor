package com.example.techmentor.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.techmentor.R

// Fontes variáveis (Google Fonts, licença OFL): o peso é aplicado por FontVariation.
// Antes do Android 8 (API 26) o sistema ignora a variação e usa o peso padrão do arquivo.

// Quanto a fonte fica mais grossa em relação ao peso pedido (100 = um passo; 0 = como estava)
private const val ENGROSSAR = 130

@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight, opticalSize: Boolean = false): Font {

    // Manrope e DM Sans vão até 800 no arquivo; passar disso não faz efeito
    val pesoReal = (weight.weight + ENGROSSAR).coerceAtMost(800)

    val settings = if (opticalSize) {
        FontVariation.Settings(
            FontVariation.weight(pesoReal),
            FontVariation.opticalSizing(14.sp)
        )
    } else {
        FontVariation.Settings(FontVariation.weight(pesoReal))
    }

    return Font(
        resId = resId,
        weight = weight,
        variationSettings = settings
    )
}

// Títulos e logo
val ManropeFamily = FontFamily(
    variableFont(R.font.manrope, FontWeight.Normal),
    variableFont(R.font.manrope, FontWeight.Medium),
    variableFont(R.font.manrope, FontWeight.SemiBold),
    variableFont(R.font.manrope, FontWeight.Bold),
    variableFont(R.font.manrope, FontWeight.ExtraBold)
)

// Texto geral do app
val DmSansFamily = FontFamily(
    variableFont(R.font.dm_sans, FontWeight.Normal, opticalSize = true),
    variableFont(R.font.dm_sans, FontWeight.Medium, opticalSize = true),
    variableFont(R.font.dm_sans, FontWeight.SemiBold, opticalSize = true),
    variableFont(R.font.dm_sans, FontWeight.Bold, opticalSize = true),
    variableFont(R.font.dm_sans, FontWeight.ExtraBold, opticalSize = true)
)
