package com.example.techmentor.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.Sessao
import com.example.techmentor.data.TrocarSenhaRequest
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.AuthField
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.mentoria.CabecalhoVoltar
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.mentoria.mensagemDaApi
import com.example.techmentor.ui.theme.ManropeFamily
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

// Mesmos limites da senha no cadastro e no backend
private const val SENHA_MINIMA = 8
private const val SENHA_MAXIMA = 72

/**
 * Troca de senha de quem já está logada: senha atual, nova senha e confirmação.
 *
 * obrigatoria = true é o primeiro acesso com a senha provisória (a que a administradora cadastrou): não dá para
 * voltar até trocar e, como a troca derruba o token, depois dela a pessoa volta ao login para entrar com a senha nova.
 */
@Composable
fun TrocarSenhaScreen(
    navController: NavController,
    obrigatoria: Boolean = false
) {

    // As senhas ficam só em memória (remember), nunca no estado salvo do sistema
    var atual by remember { mutableStateOf("") }
    var nova by remember { mutableStateOf("") }
    var confirmacao by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(false) }
    var trocada by remember { mutableStateOf(false) }

    val foco = LocalFocusManager.current
    val escopo = rememberCoroutineScope()

    fun trocar() {
        if (carregando) return

        erro = when {
            atual.isEmpty() -> if (obrigatoria) "Informe a senha provisória." else "Informe sua senha atual."
            nova.length < SENHA_MINIMA -> "A nova senha deve ter no mínimo $SENHA_MINIMA caracteres."
            nova.length > SENHA_MAXIMA -> "A nova senha deve ter no máximo $SENHA_MAXIMA caracteres."
            nova == atual -> "A nova senha deve ser diferente da atual."
            nova != confirmacao -> "As senhas não conferem."
            else -> null
        }
        if (erro != null) return

        val usuarioId = Sessao.usuarioId ?: return

        carregando = true
        escopo.launch {
            try {
                ApiClient.api.trocarSenha(usuarioId, TrocarSenhaRequest(atual, nova))
                if (obrigatoria) {
                    sairParaOLogin(navController, Sessao.email)
                } else {
                    trocada = true
                }
            } catch (e: HttpException) {
                erro = when (e.code()) {
                    400 -> mensagemDaApi(e) ?: "Confira os dados informados."
                    429 -> mensagemDaApi(e) ?: "Muitas tentativas. Tente de novo em alguns minutos."
                    404 -> "Usuário não encontrado."
                    in 500..599 -> "O servidor está indisponível no momento. Tente de novo em instantes."
                    else -> ERRO_CONEXAO
                }
            } catch (e: IOException) {
                erro = ERRO_CONEXAO
            } finally {
                carregando = false
            }
        }
    }

    // No primeiro acesso não dá para voltar sem trocar a senha provisória
    BackHandler(enabled = obrigatoria) { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
            .imePadding()
    ) {

        if (obrigatoria) {
            Text(
                text = "Crie sua senha",
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp),
                fontFamily = ManropeFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
                color = MentoriaCores.TextoEscuro
            )
        } else {
            CabecalhoVoltar(
                titulo = "Trocar senha",
                onVoltar = { navController.popBackStack() }
            )
        }

        if (trocada) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Senha alterada!",
                    fontFamily = ManropeFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MentoriaCores.TextoEscuro
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Use a nova senha na próxima vez que entrar.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MentoriaCores.TextoMudo
                )

                Spacer(modifier = Modifier.height(24.dp))

                GradientButton(
                    text = "Concluir",
                    onClick = { navController.popBackStack() }
                )
            }

        } else {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp)
            ) {

                Text(
                    text = if (obrigatoria) {
                        "Você entrou com uma senha provisória. Digite-a abaixo e escolha uma senha só sua para continuar."
                    } else {
                        "Para sua segurança, confirme a senha atual antes de escolher a nova."
                    },
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MentoriaCores.TextoMudo
                )

                Spacer(modifier = Modifier.height(20.dp))

                AuthField(
                    label = if (obrigatoria) "Senha provisória" else "Senha atual",
                    value = atual,
                    onValueChange = {
                        atual = it
                        erro = null
                    },
                    placeholder = "••••••••",
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { foco.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                AuthField(
                    label = "Nova senha",
                    value = nova,
                    onValueChange = {
                        nova = it
                        erro = null
                    },
                    placeholder = "••••••••",
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { foco.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                AuthField(
                    label = "Confirmar nova senha",
                    value = confirmacao,
                    onValueChange = {
                        confirmacao = it
                        erro = null
                    },
                    placeholder = "••••••••",
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            foco.clearFocus()
                            trocar()
                        }
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Dica da senha; some quando há erro, para não empilhar duas mensagens
                Text(
                    text = erro ?: "Mínimo de $SENHA_MINIMA caracteres.",
                    fontSize = 11.sp,
                    color = if (erro != null) AuthColors.Error else MentoriaCores.TextoMudo
                )

                Spacer(modifier = Modifier.height(16.dp))

                GradientButton(
                    text = when {
                        carregando -> "Salvando..."
                        obrigatoria -> "Definir senha"
                        else -> "Trocar senha"
                    },
                    onClick = {
                        foco.clearFocus()
                        trocar()
                    }
                )

                if (obrigatoria) {

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sem voltar nesta tela: quem entrou na conta errada precisa de uma saída
                    Text(
                        text = "Sair da conta",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { sairParaOLogin(navController) }
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuthColors.PurpleDark
                    )
                }
            }
        }
    }
}

/**
 * Encerra a sessão e volta ao login, apagando o histórico de telas.
 * Com [emailDaSenhaAlterada], o login abre com o e-mail preenchido e o aviso "Senha alterada!".
 */
private fun sairParaOLogin(navController: NavController, emailDaSenhaAlterada: String? = null) {
    Sessao.sair()
    navController.navigate("login") {
        popUpTo(0) { inclusive = true }
    }
    if (!emailDaSenhaAlterada.isNullOrEmpty()) {
        // mesmo recurso da redefinição por e-mail; se não der, o login só abre sem o aviso
        runCatching {
            navController.getBackStackEntry("login").savedStateHandle["senhaAlterada"] = emailDaSenhaAlterada
        }
    }
}
