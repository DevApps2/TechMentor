package com.example.techmentor.ui.esquecisenha

import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.EsqueciSenhaRequest
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.AuthField
import com.example.techmentor.ui.auth.AuthHeading
import com.example.techmentor.ui.auth.AuthScaffold
import com.example.techmentor.ui.auth.BrandHeader
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.auth.OutlinedAuthButton
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

@Composable
fun EsqueciSenhaScreen(
    navController: NavController
) {

    var email by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    fun enviar() {
        if (loading) return

        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            error = "Informe um e-mail válido."
            return
        }

        loading = true
        scope.launch {
            try {
                ApiClient.api.esqueciSenha(EsqueciSenhaRequest(email.trim()))

                // O backend responde igual exista ou não a conta; seguimos para a tela do código
                navController.navigate("redefinir-senha/${Uri.encode(email.trim())}")
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
            eyebrow = "RECUPERAR ACESSO",
            title = "Esqueceu a senha?",
            subtitle = "Informe seu e-mail e enviaremos um código de 6 dígitos, válido por 10 minutos."
        )

        Spacer(modifier = Modifier.height(25.dp))

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
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    enviar()
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
            text = if (loading) "Enviando..." else "Enviar código",
            onClick = {
                focusManager.clearFocus()
                enviar()
            }
        )

        Spacer(modifier = Modifier.height(17.dp))

        OutlinedAuthButton(
            text = "Voltar ao login",
            onClick = { navController.popBackStack() }
        )
    }
}