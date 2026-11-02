package com.example.techmentor.ui.avaliacao

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.AvaliacaoPublicaResponse
import com.example.techmentor.data.AvaliacoesDaMentoraResponse
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.mentoria.SecaoCartao
import com.example.techmentor.ui.theme.ManropeFamily
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.roundToInt

// Quantas avaliações o app pede ao backend, e quantas mostra antes do "Ver todas"
private const val PEDIR_AO_BACKEND = 20
private const val MOSTRAR_DE_INICIO = 3

class AvaliacoesDaMentoraViewModel : ViewModel() {

    // nulo = ainda não carregou (ou não deu para carregar)
    private val _dados = MutableStateFlow<AvaliacoesDaMentoraResponse?>(null)
    val dados: StateFlow<AvaliacoesDaMentoraResponse?> = _dados.asStateFlow()

    private var tarefa: Job? = null

    fun carregar(mentoraId: Long) {
        // Evita duas cargas ao mesmo tempo
        if (tarefa?.isActive == true) return

        tarefa = viewModelScope.launch {
            try {
                _dados.value = ApiClient.api.avaliacoesDaMentora(mentoraId, PEDIR_AO_BACKEND)
            } catch (e: HttpException) {
                // Não conseguiu: a seção só não aparece e o resto do perfil continua
            } catch (e: IOException) {
                // idem
            }
        }
    }
}

/**
 * Seção "Avaliações" do perfil público da mentora: nota média, barras por estrela e comentários.
 * mentoraId é o id do PERFIL da mentora (o mesmo do MentoraResponse).
 */
@Composable
fun AvaliacoesDaMentora(
    mentoraId: Long,
    viewModel: AvaliacoesDaMentoraViewModel = viewModel()
) {

    val dados by viewModel.dados.collectAsState()
    var mostrarTodas by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar(mentoraId)
    }

    AtualizarAutomaticamente { viewModel.carregar(mentoraId) }

    val resposta = dados ?: return

    SecaoCartao(titulo = "Avaliações") {

        if (resposta.totalAvaliacoes == 0 && resposta.avaliacoes.isEmpty()) {

            Text(
                text = "Esta mentora ainda não tem avaliações.",
                fontSize = 13.sp,
                color = MentoriaCores.TextoMudo
            )

        } else {

            Resumo(resposta)

            if (resposta.avaliacoes.isNotEmpty()) {

                val visiveis = if (mostrarTodas) resposta.avaliacoes else resposta.avaliacoes.take(MOSTRAR_DE_INICIO)

                Spacer(modifier = Modifier.height(14.dp))

                visiveis.forEachIndexed { indice, avaliacao ->

                    if (indice > 0) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MentoriaCores.Borda)
                        )
                    }

                    Comentario(avaliacao)
                }

                if (!mostrarTodas && resposta.avaliacoes.size > MOSTRAR_DE_INICIO) {

                    Text(
                        text = "Ver todas as ${resposta.avaliacoes.size}",
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) { mostrarTodas = true }
                            .padding(vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MentoriaCores.Roxo
                    )
                }
            }
        }
    }
}

@Composable
private fun Resumo(resposta: AvaliacoesDaMentoraResponse) {

    val somaDasBarras = resposta.distribuicao.values.sum().coerceAtLeast(1L)

    Row(verticalAlignment = Alignment.CenterVertically) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = formatarNotaMedia(resposta.notaMedia),
                fontFamily = ManropeFamily,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                color = MentoriaCores.TextoEscuro
            )

            Text(
                text = estrelasEmTexto(resposta.notaMedia.roundToInt()),
                fontSize = 14.sp,
                color = MentoriaCores.Estrela
            )

            Text(
                text = if (resposta.totalAvaliacoes == 1) "1 avaliação" else "${resposta.totalAvaliacoes} avaliações",
                fontSize = 10.sp,
                color = MentoriaCores.TextoMudo
            )
        }

        Spacer(modifier = Modifier.width(18.dp))

        Column(modifier = Modifier.weight(1f)) {

            for (estrela in 5 downTo 1) {

                val quantidade = resposta.distribuicao[estrela.toString()] ?: 0L

                Barra(
                    estrela = estrela,
                    quantidade = quantidade,
                    fracao = quantidade.toFloat() / somaDasBarras
                )
            }
        }
    }
}

@Composable
private fun Barra(
    estrela: Int,
    quantidade: Long,
    fracao: Float
) {

    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "$estrela ★",
            modifier = Modifier.width(28.dp),
            fontSize = 10.sp,
            color = MentoriaCores.TextoMudo
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFEDEAF3))
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth(fracao.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MentoriaCores.Estrela)
            )
        }

        Text(
            text = quantidade.toString(),
            modifier = Modifier
                .width(24.dp)
                .padding(start = 6.dp),
            fontSize = 10.sp,
            textAlign = TextAlign.End,
            color = MentoriaCores.TextoMudo
        )
    }
}

@Composable
private fun Comentario(avaliacao: AvaliacaoPublicaResponse) {

    Column {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Text(
                text = avaliacao.avaliadora,
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MentoriaCores.TextoEscuro
            )

            Text(
                text = diaMesEAno(avaliacao.criadaEm),
                fontSize = 10.sp,
                color = MentoriaCores.TextoMudo
            )
        }

        Text(
            text = estrelasEmTexto(avaliacao.nota),
            fontSize = 13.sp,
            color = MentoriaCores.Estrela
        )

        if (!avaliacao.comentario.isNullOrBlank()) {

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = avaliacao.comentario,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color(0xFF514C58)
            )
        }
    }
}