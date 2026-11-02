package com.example.techmentor.ui.avaliacao

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.AvaliacaoPendenteResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.mentoria.AvatarMentora
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

// Quantas mentorias a pendência mostra de uma vez; o resto vira "e mais N"
private const val MAXIMO_NA_TELA = 3

/** As mentorias que a pessoa ainda pode avaliar. Vive em cada tela que mostra o cartão. */
class PendentesViewModel : ViewModel() {

    private val _pendentes = MutableStateFlow<List<AvaliacaoPendenteResponse>>(emptyList())
    val pendentes: StateFlow<List<AvaliacaoPendenteResponse>> = _pendentes.asStateFlow()

    private var tarefa: Job? = null

    fun carregar() {
        val usuarioId = Sessao.usuarioId ?: return

        // Evita duas cargas ao mesmo tempo
        if (tarefa?.isActive == true) return

        tarefa = viewModelScope.launch {
            try {
                _pendentes.value = ApiClient.api.avaliacoesPendentes(usuarioId)
            } catch (e: HttpException) {
                // É um extra da tela: se falhar, o cartão só não aparece
            } catch (e: IOException) {
                // idem
            }
        }
    }
}

/**
 * Cartões "Como foi sua mentoria com ...?" para quem ainda não avaliou. Não ocupa espaço nenhum quando não há pendências.
 * Serve para a Home da aluna e para a Home da mentora.
 */
@Composable
fun AvaliacoesPendentes(
    navController: NavController,
    viewModel: PendentesViewModel = viewModel()
) {

    val pendentes by viewModel.pendentes.collectAsState()

    // Atualiza ao abrir e ao voltar (ex.: depois de avaliar, o cartão some) e, com a tela aberta, de minuto em minuto
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar()
    }

    AtualizarAutomaticamente(intervaloSegundos = 60) { viewModel.carregar() }

    if (pendentes.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Text(
            text = "Avalie suas mentorias",
            fontFamily = ManropeFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            color = MentoriaCores.TextoEscuro
        )

        pendentes.take(MAXIMO_NA_TELA).forEach { pendente ->
            CartaoPendente(
                pendente = pendente,
                onClick = { navController.navigate("avaliar/${pendente.mentoriaId}") }
            )
        }

        if (pendentes.size > MAXIMO_NA_TELA) {

            val restantes = pendentes.size - MAXIMO_NA_TELA

            Text(
                text = "e mais $restantes para avaliar",
                fontSize = 12.sp,
                color = MentoriaCores.TextoMudo
            )
        }
    }
}

@Composable
private fun CartaoPendente(
    pendente: AvaliacaoPendenteResponse,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(17.dp)
    val primeiroNome = pendente.outraPessoaNome.trim().split(" ").firstOrNull().orEmpty()
        .ifEmpty { pendente.outraPessoaNome }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color(0xFFF5F0FF))
            .border(1.dp, Color(0xFFDDD0FB), forma)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        AvatarMentora(
            nome = pendente.outraPessoaNome,
            fotoUrl = pendente.outraPessoaFotoUrl,
            tamanho = 42.dp,
            raio = 14.dp,
            tamanhoDaInicial = 17.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {

            Text(
                text = "Como foi sua mentoria com $primeiroNome?",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MentoriaCores.TextoEscuro,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Mentoria de ${diaEMes(pendente.inicio)} · avalie até ${diaEMes(pendente.prazoFinal)}",
                fontSize = 10.sp,
                color = MentoriaCores.TextoMudo
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MentoriaCores.Roxo)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {

            Text(
                text = "Avaliar",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}