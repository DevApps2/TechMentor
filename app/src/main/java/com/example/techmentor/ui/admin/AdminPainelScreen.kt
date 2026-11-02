package com.example.techmentor.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.ui.components.AtualizarAutomaticamente
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPainelScreen(
    navController: NavController,
    viewModel: AdminPainelViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    EfeitoSessaoExpirada(expirada = estado.sessaoExpirada, navController = navController)

    // Carrega ao abrir e ao voltar para a tela (ex.: depois de resolver uma denúncia, o número muda)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar()
    }

    // Com a tela aberta, atualiza sozinha de tempos em tempos
    AtualizarAutomaticamente { viewModel.carregar() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        TopBarMarca()

        Box(
            modifier = Modifier.weight(1f)
        ) {

            val erro = estado.erro

            when {
                erro != null -> EstadoErro(
                    mensagem = erro,
                    onTentarDeNovo = { viewModel.carregar() }
                )

                estado.carregando -> PainelCarregando()

                else -> PullToRefreshBox(
                    isRefreshing = estado.atualizando,
                    onRefresh = { viewModel.carregar(manual = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    PainelConteudo(
                        estado = estado,
                        navController = navController
                    )
                }
            }
        }

        AdminBottomBar(
            navController = navController,
            rotaAtual = "admin"
        )
    }
}


@Composable
private fun PainelConteudo(
    estado: AdminPainelUiState,
    navController: NavController
) {

    val primeiroNome = estado.nome?.trim()?.substringBefore(" ").orEmpty()
    val pendentes = (estado.denunciasAbertas ?: 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {

        TituloDeAba(
            titulo = "Painel",
            subtitulo = if (primeiroNome.isEmpty()) "Visão geral da plataforma." else "Olá, $primeiroNome! Veja como está a plataforma."
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                CartaoDeNumero(
                    modifier = Modifier.weight(1f),
                    valor = estado.denunciasAbertas,
                    rotulo = "Denúncias abertas",
                    chamaAtencao = pendentes > 0,
                    onClick = { navController.navigate("admin/denuncias") }
                )

                CartaoDeNumero(
                    modifier = Modifier.weight(1f),
                    valor = estado.denunciasEmAnalise,
                    rotulo = "Em análise",
                    chamaAtencao = false,
                    onClick = { navController.navigate("admin/denuncias") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                CartaoDeNumero(
                    modifier = Modifier.weight(1f),
                    valor = estado.oportunidades,
                    rotulo = "Oportunidades ativas",
                    chamaAtencao = false,
                    onClick = { navController.navigate("admin/oportunidades") }
                )

                CartaoDeNumero(
                    modifier = Modifier.weight(1f),
                    valor = estado.mentoras,
                    rotulo = "Mentoras",
                    chamaAtencao = false,
                    onClick = { navController.navigate("admin/mentoras") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                CartaoDeNumero(
                    modifier = Modifier.weight(1f),
                    valor = estado.alunas,
                    rotulo = "Alunas cadastradas",
                    chamaAtencao = false,
                    onClick = null
                )

                // espaço para o cartão ficar com a mesma largura dos outros
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Atalhos",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20202A)
            )

            Spacer(modifier = Modifier.height(10.dp))

            CartaoDeAtalho(
                icone = Icone.USUARIOS,
                titulo = "Cadastrar mentora",
                descricao = "Cria a conta e o perfil de uma nova mentora",
                onClick = { navController.navigate("admin/mentoras/nova") }
            )

            Spacer(modifier = Modifier.height(10.dp))

            CartaoDeAtalho(
                icone = Icone.MALETA,
                titulo = "Nova oportunidade",
                descricao = "Estágio, emprego, bolsa ou evento",
                onClick = { navController.navigate("admin/oportunidades/nova") }
            )

            Spacer(modifier = Modifier.height(10.dp))

            CartaoDeAtalho(
                icone = Icone.ESCUDO,
                titulo = "Revisar denúncias",
                descricao = if (pendentes == 1) "1 denúncia aguardando análise" else "$pendentes denúncias aguardando análise",
                onClick = { navController.navigate("admin/denuncias") }
            )
        }
    }
}

@Composable
private fun PainelCarregando() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        EsqueletoBloco(altura = 56.dp)
        EsqueletoBloco(altura = 84.dp)
        EsqueletoBloco(altura = 84.dp)
        EsqueletoBloco(altura = 84.dp)
    }
}
