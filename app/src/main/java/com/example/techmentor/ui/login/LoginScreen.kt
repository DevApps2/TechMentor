package com.example.techmentor.ui.login

import android.util.Patterns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
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
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.AuthField
import com.example.techmentor.ui.auth.AuthHeading
import com.example.techmentor.ui.auth.AuthScaffold
import com.example.techmentor.ui.auth.BrandHeader
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.auth.OrDivider
import com.example.techmentor.ui.auth.OutlinedAuthButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import com.example.techmentor.data.Sessao
@Composable
fun LoginScreen(
    navController: NavController
) {

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    var aviso by rememberSaveable { mutableStateOf<String?>(null) }

    // Quando a conta é criada, a tela de cadastro volta para cá e deixa o e-mail aqui
    val estadoSalvo = navController.currentBackStackEntry?.savedStateHandle
    val emailCriado by (estadoSalvo?.getStateFlow("emailCriado", "") ?: MutableStateFlow("")).collectAsState()

    LaunchedEffect(emailCriado) {
        if (emailCriado.isNotEmpty()) {
            email = emailCriado
            password = ""
            error = null
            aviso = "Conta criada! Entre com seu e-mail e senha."
            estadoSalvo?.set("emailCriado", "")
        }
    }

    fun submit() {
        if (loading) return

        error = when {
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Informe um e-mail válido."
            password.isEmpty() -> "Informe sua senha."
            else -> null
        }
        if (error != null) return

        loading = true
        scope.launch {
            try {
                val usuario = ApiClient.api.login(LoginRequest(email.trim(), password))
                Sessao.entrar(usuario.id)
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            } catch (e: HttpException) {
                error = if (e.code() == 401) "E-mail ou senha incorretos." else "Erro no servidor (${e.code()})."
            } catch (e: IOException) {
                error = "Sem conexão com o servidor."
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
            eyebrow = "BEM-VINDA DE VOLTA",
            title = "Continue sua jornada.",
            subtitle = "Entre para acessar suas trilhas e conexões."
        )

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        // ==================================================
        // CAMPOS
        // ==================================================

        // AVISO DE CONTA CRIADA
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
            label = "E-mail",
            value = email,
            onValueChange = {
                email = it
                error = null
                aviso = null
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
                aviso = null
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

        Spacer(
            modifier = Modifier.height(9.dp)
        )

        // ERRO + ESQUECI MINHA SENHA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = error.orEmpty(),
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                color = AuthColors.Error
            )

            Text(
                text = "Esqueci minha senha",
                modifier = Modifier
                    .clickable(role = Role.Button) {
                        // Será conectado depois
                    }
                    .padding(vertical = 8.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AuthColors.PurpleDark
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        GradientButton(
            text = if (loading) "Entrando..." else "Entrar",
            onClick = {
                focusManager.clearFocus()
                submit()
            }
        )

        Spacer(
            modifier = Modifier.height(17.dp)
        )

        OrDivider()

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedAuthButton(
            text = "Criar conta",
            onClick = {
                navController.navigate("register")
            }
        )
    }
}
