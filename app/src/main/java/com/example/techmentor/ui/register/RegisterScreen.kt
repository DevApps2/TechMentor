package com.example.techmentor.ui.register

import android.util.Patterns
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.AuthField
import com.example.techmentor.ui.auth.AuthHeading
import com.example.techmentor.ui.auth.AuthScaffold
import com.example.techmentor.ui.auth.BrandHeader
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.auth.OrDivider
import com.example.techmentor.ui.auth.OutlinedAuthButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.CadastroRequest
import com.example.techmentor.data.ErroResponse
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle

// Mesmos limites do cadastro na API (UsuarioDtos.CreateRequest)
private const val NAME_MAX_LENGTH = 120
private const val EMAIL_MAX_LENGTH = 160
private const val PASSWORD_MIN_LENGTH = 8
private const val PASSWORD_MAX_LENGTH = 72

// Política de Privacidade publicada no Google Sites (a mesma usada na configuração do login com o Google)
private const val URL_POLITICA_DE_PRIVACIDADE =
    "https://sites.google.com/view/techmentor-app/pol%C3%ADtica-de-privacidade"

@Composable
fun RegisterScreen(
    navController: NavController
) {

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    fun submit() {
        if (loading) return

        val nomeLimpo = name.trim().replace(Regex("\\s+"), " ")
        val emailLimpo = email.trim().lowercase()

        error = when {
            nomeLimpo.isEmpty() -> "Informe seu nome."
            nomeLimpo.length > NAME_MAX_LENGTH -> "O nome deve ter no máximo $NAME_MAX_LENGTH caracteres."
            !Patterns.EMAIL_ADDRESS.matcher(emailLimpo).matches() -> "Informe um e-mail válido."
            emailLimpo.length > EMAIL_MAX_LENGTH -> "O e-mail deve ter no máximo $EMAIL_MAX_LENGTH caracteres."
            password.length < PASSWORD_MIN_LENGTH -> "A senha deve ter no mínimo $PASSWORD_MIN_LENGTH caracteres."
            password.length > PASSWORD_MAX_LENGTH -> "A senha deve ter no máximo $PASSWORD_MAX_LENGTH caracteres."
            else -> null
        }
        if (error != null) return

        loading = true
        scope.launch {
            try {
                ApiClient.api.cadastrar(CadastroRequest(name.trim(), email.trim(), password))

                // Conta criada: volta para o login com o e-mail já preenchido
                navController.previousBackStackEntry?.savedStateHandle?.set("emailCriado", email.trim())
                navController.popBackStack()
            } catch (e: HttpException) {
                error = when (e.code()) {
                    409 -> "Esse e-mail já está cadastrado."
                    400 -> mensagemDaApi(e) ?: "Confira os dados informados."
                    else -> ERRO_CONEXAO
                }
            } catch (e: IOException) {
                error = ERRO_CONEXAO
            } finally {
                loading = false
            }
        }
    }

    AuthScaffold {

        BrandHeader()

        Spacer(
            modifier = Modifier.height(56.dp)
        )

        AuthHeading(
            eyebrow = "BEM-VINDA À COMUNIDADE",
            title = "Crie seu futuro em tech.",
            subtitle = "Comece agora a aprender, conectar e crescer."
        )

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        // ==================================================
        // CAMPOS
        // ==================================================

        AuthField(
            label = "Nome completo",
            value = name,
            onValueChange = {
                name = it
                error = null
            },
            placeholder = "Como podemos te chamar?",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        AuthField(
            label = "E-mail",
            value = email,
            onValueChange = {
                email = it
                error = null
            },
            placeholder = "voce@exemplo.com",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        AuthField(
            label = "Senha",
            value = password,
            onValueChange = {
                password = it
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
                    submit()
                }
            )
        )

        // DICA DA SENHA: some quando há erro, para não empilhar duas mensagens
        if (error == null) {
            Text(
                text = "Mínimo de $PASSWORD_MIN_LENGTH caracteres.",
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 11.sp,
                color = Color(0xFF8A8691)
            )
        }

        error?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 11.sp,
                color = AuthColors.Error
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        GradientButton(
            text = if (loading) "Criando conta..." else "Criar minha conta",
            onClick = {
                focusManager.clearFocus()
                submit()
            }
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        AvisoDePrivacidade()

        Spacer(
            modifier = Modifier.height(17.dp)
        )

        OrDivider()

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedAuthButton(
            text = "Já tenho uma conta",
            onClick = {
                navController.popBackStack()
            }
        )
    }
}

/** "Ao criar sua conta, você concorda com a Política de Privacidade." Tocar abre a política no navegador. */
@Composable
private fun AvisoDePrivacidade() {

    val abrir = LocalUriHandler.current

    Text(
        text = buildAnnotatedString {
            append("Ao criar sua conta, você concorda com a ")
            withStyle(SpanStyle(color = AuthColors.PurpleDark, fontWeight = FontWeight.Bold)) {
                append("Política de Privacidade")
            }
            append(".")
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                // Sem navegador instalado o toque só não faz nada, em vez de derrubar o app
                runCatching { abrir.openUri(URL_POLITICA_DE_PRIVACIDADE) }
            }
            .padding(vertical = 4.dp),
        fontSize = 11.sp,
        lineHeight = 16.sp,
        textAlign = TextAlign.Center,
        color = Color(0xFF8A8691)
    )
}

private fun mensagemDaApi(e: HttpException): String? =
    runCatching {
        Gson().fromJson(e.response()?.errorBody()?.string(), ErroResponse::class.java)?.erro
    }.getOrNull()
