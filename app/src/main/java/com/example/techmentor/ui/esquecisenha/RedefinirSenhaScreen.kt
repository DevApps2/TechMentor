package com.example.techmentor.ui.esquecisenha

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.ErroResponse
import com.example.techmentor.data.EsqueciSenhaRequest
import com.example.techmentor.data.RedefinirSenhaRequest
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.AuthField
import com.example.techmentor.ui.auth.AuthHeading
import com.example.techmentor.ui.auth.AuthScaffold
import com.example.techmentor.ui.auth.BrandHeader
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.auth.OutlinedAuthButton
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

@Composable
fun RedefinirSenhaScreen(
    navController: NavController,
    email: String
) {

    var codigo by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var confirmacao by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var aviso by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var segundos by remember { mutableIntStateOf(60) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // Contagem regressiva do "Reenviar código": a cada segundo, diminui 1
    LaunchedEffect(segundos) {
        if (segundos > 0) {
            delay(1000)
            segundos--
        }
    }

    fun redefinir() {
        if (loading) return

        error = when {
            codigo.length != 6 -> "Digite o código de 6 dígitos."
            senha.length < 8 -> "A senha deve ter pelo menos 8 caracteres."
            senha.length > 72 -> "A senha deve ter no máximo 72 caracteres."
            senha != confirmacao -> "As senhas não conferem."
            else -> null
        }
        if (error != null) return

        loading = true
        scope.launch {
            try {
                ApiClient.api.redefinirSenha(RedefinirSenhaRequest(email, codigo, senha))

                // Avisa a tela de login e volta para ela
                navController.getBackStackEntry("login").savedStateHandle["senhaAlterada"] = email
                navController.popBackStack("login", inclusive = false)
            } catch (e: HttpException) {
                error = if (e.code() == 400) {
                    mensagemDaApi(e) ?: "Confira os dados informados."
                } else {
                    ERRO_CONEXAO
                }
            } catch (e: IOException) {
                error = ERRO_CONEXAO
            } finally {
                loading = false
            }
        }
    }

    fun reenviar() {
        if (segundos > 0 || loading) return

        loading = true
        scope.launch {
            try {
                ApiClient.api.esqueciSenha(EsqueciSenhaRequest(email))
                error = null
                aviso = "Código reenviado! Confira seu e-mail."
                segundos = 60
            } catch (e: HttpException) {
                error = ERRO_CONEXAO
            } catch (e: IOException) {
                error = ERRO_CONEXAO
            } finally {
                loading = false
            }
        }
    }

    AuthScaffold {

        BrandHeader()

        Spacer(modifier = Modifier.height(56.dp))

        AuthHeading(
            eyebrow = "NOVA SENHA",
            title = "Digite o código.",
            subtitle = "Enviamos um código para $email. Ele vale por 10 minutos."
        )

        Spacer(modifier = Modifier.height(25.dp))

        aviso?.let { mensagem ->
            Text(
                text = mensagem,
                modifier = Modifier.padding(bottom = 12.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AuthColors.Success
            )
        }

        AuthField(
            label = "Código",
            value = codigo,
            onValueChange = {
                codigo = it.filter { c -> c.isDigit() }.take(6)
                error = null
            },
            placeholder = "000000",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        AuthField(
            label = "Nova senha",
            value = senha,
            onValueChange = {
                senha = it
                error = null
            },
            placeholder = "••••••••",
            isPassword = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        AuthField(
            label = "Confirmar nova senha",
            value = confirmacao,
            onValueChange = {
                confirmacao = it
                error = null
            },
            placeholder = "••••••••",
            isPassword = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    redefinir()
                }
            )
        )

        Spacer(modifier = Modifier.height(9.dp))

        Text(
            text = error.orEmpty(),
            fontSize = 11.sp,
            color = AuthColors.Error
        )

        Spacer(modifier = Modifier.height(16.dp))

        GradientButton(
            text = if (loading) "Aguarde..." else "Redefinir senha",
            onClick = {
                focusManager.clearFocus()
                redefinir()
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (segundos > 0) "Reenviar código em ${segundos}s" else "Reenviar código",
            modifier = Modifier
                .clickable(enabled = segundos == 0 && !loading, role = Role.Button) { reenviar() }
                .padding(vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (segundos > 0) AuthColors.TextHint else AuthColors.PurpleDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedAuthButton(
            text = "Voltar ao login",
            onClick = { navController.popBackStack("login", inclusive = false) }
        )
    }
}

/** Lê a mensagem {"erro": "..."} que o backend devolve nos erros 400. */
private fun mensagemDaApi(e: HttpException): String? =
    runCatching {
        Gson().fromJson(e.response()?.errorBody()?.string(), ErroResponse::class.java)?.erro
    }.getOrNull()