package com.example.techmentor.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.AuthField
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha
import com.example.techmentor.ui.home.BottomNavItem
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily


// ======================================================
// BARRA INFERIOR DA ADMINISTRADORA
// (mesmo visual da barra da aluna; só mudam os itens)
// ======================================================

@Composable
fun AdminBottomBar(
    navController: NavController,
    rotaAtual: String
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {

        // linha fina no topo da barra
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE9E7EE))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(76.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {

            BottomNavItem(
                icone = Icone.CASA,
                label = "Painel",
                selected = rotaAtual == "admin",
                onClick = { irPara(navController, "admin") }
            )

            BottomNavItem(
                icone = Icone.ESCUDO,
                label = "Denúncias",
                selected = rotaAtual == "admin/denuncias",
                onClick = { irPara(navController, "admin/denuncias") }
            )

            // "Vagas" porque "Oportunidades" não cabe no item; a tela se chama Oportunidades
            BottomNavItem(
                icone = Icone.MALETA,
                label = "Vagas",
                selected = rotaAtual == "admin/oportunidades",
                onClick = { irPara(navController, "admin/oportunidades") }
            )

            BottomNavItem(
                icone = Icone.USUARIOS,
                label = "Mentoras",
                selected = rotaAtual == "admin/mentoras",
                onClick = { irPara(navController, "admin/mentoras") }
            )

            BottomNavItem(
                icone = Icone.PERFIL,
                label = "Perfil",
                selected = rotaAtual == "admin/perfil",
                onClick = { irPara(navController, "admin/perfil") }
            )
        }
    }
}

private fun irPara(navController: NavController, rota: String) {
    navController.navigate(rota) {
        popUpTo("admin") { inclusive = false }
        launchSingleTop = true
    }
}


// ======================================================
// SESSÃO EXPIRADA
// ======================================================

/** Volta ao login quando o usuário guardado no aparelho não existe mais (ex.: o banco foi reiniciado). */
@Composable
fun EfeitoSessaoExpirada(
    expirada: Boolean,
    navController: NavController
) {

    LaunchedEffect(expirada) {
        if (expirada) {
            Sessao.sair()
            navController.navigate("login") {
                popUpTo("admin") { inclusive = true }
            }
        }
    }
}


// ======================================================
// TEXTOS E ETIQUETAS
// ======================================================

/** Título grande das telas de aba (Denúncias, Oportunidades...), igual ao da tela de Mentoria. */
@Composable
fun TituloDeAba(
    titulo: String,
    subtitulo: String
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp)
    ) {

        Text(
            text = titulo,
            fontFamily = ManropeFamily,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
            color = MentoriaCores.TextoEscuro
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = subtitulo,
            fontSize = 13.sp,
            color = Color(0xFF797486)
        )
    }
}

/** Etiqueta com a situação da denúncia: vermelha (aberta), âmbar (em análise) ou verde (resolvida). */
@Composable
fun PilulaStatus(status: String) {

    val (corDoTexto, corDoFundo) = when (status) {
        "ABERTA" -> Pair(Color(0xFFDC2626), Color(0xFFFEE2E2))
        "EM_ANALISE" -> Pair(Color(0xFFD97706), Color(0xFFFEF3C7))
        else -> Pair(Color(0xFF16A34A), Color(0xFFDCFCE7))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(corDoFundo)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {

        Text(
            text = nomeDoStatus(status),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = corDoTexto
        )
    }
}

@Composable
fun MensagemDeErro(texto: String) {

    Text(
        text = texto,
        modifier = Modifier.fillMaxWidth(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = AuthColors.Error
    )
}

@Composable
fun MensagemDeSucesso(texto: String) {

    Text(
        text = texto,
        modifier = Modifier.fillMaxWidth(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = AuthColors.Success
    )
}

/** Linha "Rótulo / valor" usada nos detalhes. */
@Composable
fun LinhaDeInformacao(
    rotulo: String,
    valor: String
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = rotulo,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8A8691)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = valor,
            fontSize = 13.sp,
            color = MentoriaCores.TextoEscuro
        )
    }
}


// ======================================================
// BOTÕES E DIÁLOGOS
// ======================================================

/** Botão de contorno vermelho para ações que apagam algo. */
@Composable
fun BotaoDePerigo(
    texto: String,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(11.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(forma)
            .background(Color.White)
            .border(1.dp, AuthColors.Error, forma)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = texto,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AuthColors.Error
        )
    }
}

@Composable
fun DialogoDeConfirmacao(
    titulo: String,
    texto: String,
    textoDoBotao: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        title = {
            Text(
                text = titulo,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                color = MentoriaCores.TextoEscuro
            )
        },
        text = {
            Text(
                text = texto,
                fontSize = 14.sp,
                color = MentoriaCores.TextoMudo
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(
                    text = textoDoBotao,
                    fontWeight = FontWeight.Bold,
                    color = AuthColors.Error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(
                    text = "Cancelar",
                    fontWeight = FontWeight.Bold,
                    color = MentoriaCores.TextoMudo
                )
            }
        }
    )
}


// ======================================================
// CARTÕES DO PAINEL
// ======================================================

/** Número grande com legenda. valor == null mostra "—" (não deu para carregar). */
@Composable
fun CartaoDeNumero(
    modifier: Modifier,
    valor: Int?,
    rotulo: String,
    chamaAtencao: Boolean,
    onClick: (() -> Unit)?
) {

    val forma = RoundedCornerShape(17.dp)

    val base = modifier
        .clip(forma)
        .background(Color.White)
        .border(1.dp, if (chamaAtencao) Color(0xFFFCA5A5) else MentoriaCores.Borda, forma)

    Column(
        modifier = (if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base)
            .padding(16.dp)
    ) {

        Text(
            text = valor?.toString() ?: "—",
            fontFamily = ManropeFamily,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (chamaAtencao) AuthColors.Error else MentoriaCores.TextoEscuro
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = rotulo,
            fontSize = 12.sp,
            color = MentoriaCores.TextoMudo
        )
    }
}

/** Atalho com ícone, no estilo dos cartões "Explorar" da Home. */
@Composable
fun CartaoDeAtalho(
    icone: Icone,
    titulo: String,
    descricao: String,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(17.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MentoriaCores.RoxoClaro),
            contentAlignment = Alignment.Center
        ) {

            IconeLinha(
                icone = icone,
                cor = MentoriaCores.RoxoEscuro,
                tamanho = 21.dp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20202A)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = descricao,
                fontSize = 12.sp,
                color = MentoriaCores.TextoMudo
            )
        }

        Text(
            text = "›",
            fontSize = 26.sp,
            textAlign = TextAlign.End,
            color = Color(0xFF797486)
        )
    }
}


// ======================================================
// CAMPO DE FORMULÁRIO
// ======================================================

/** O AuthField do projeto com os ajustes de teclado que os formulários do admin repetem. */
@Composable
fun CampoDeTexto(
    rotulo: String,
    valor: String,
    aoMudar: (String) -> Unit,
    dica: String,
    multilinha: Boolean = false,
    senha: Boolean = false,
    teclado: KeyboardType = KeyboardType.Text,
    maiusculaNoInicio: Boolean = true,
    obrigatorio: Boolean = false
) {

    AuthField(
        label = rotulo,
        obrigatorio = obrigatorio,
        value = valor,
        onValueChange = aoMudar,
        placeholder = dica,
        keyboardOptions = KeyboardOptions(
            capitalization = if (maiusculaNoInicio) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            keyboardType = teclado
        ),
        keyboardActions = KeyboardActions.Default,
        isPassword = senha,
        multilinha = multilinha
    )
}
