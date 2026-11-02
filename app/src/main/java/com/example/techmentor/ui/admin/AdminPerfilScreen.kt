package com.example.techmentor.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.Sessao
import com.example.techmentor.data.UsuarioResponse
import com.example.techmentor.ui.auth.OutlinedAuthButton
import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.mentoria.AvatarMentora
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily


@Composable
fun AdminPerfilScreen(
    navController: NavController,
    viewModel: AdminPerfilViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    EfeitoSessaoExpirada(expirada = estado.sessaoExpirada, navController = navController)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar()
    }

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

            val usuario = estado.usuario

            when {
                usuario != null -> PerfilConteudo(
                    usuario = usuario,
                    onSair = {
                        Sessao.sair()
                        navController.navigate("login") {
                            popUpTo("admin") { inclusive = true }
                        }
                    }
                )

                estado.carregando -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EsqueletoBloco(altura = 120.dp)
                    EsqueletoBloco(altura = 48.dp)
                }

                else -> EstadoErro(
                    mensagem = estado.erro ?: "Não foi possível carregar o perfil.",
                    onTentarDeNovo = { viewModel.carregar() }
                )
            }
        }

        AdminBottomBar(
            navController = navController,
            rotaAtual = "admin/perfil"
        )
    }
}


@Composable
private fun PerfilConteudo(
    usuario: UsuarioResponse,
    onSair: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        AvatarMentora(
            nome = usuario.nome,
            fotoUrl = usuario.fotoUrl,
            tamanho = 88.dp,
            raio = 28.dp,
            tamanhoDaInicial = 34.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = usuario.nome,
            fontFamily = ManropeFamily,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            color = MentoriaCores.TextoEscuro
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = usuario.email,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = MentoriaCores.TextoMudo
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Administradora",
            fontSize = 11.sp,
            color = Color(0xFF8A8691)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedAuthButton(
            text = "Sair da conta",
            onClick = onSair
        )
    }
}
