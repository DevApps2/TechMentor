package com.example.techmentor.ui.parceira

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.techmentor.data.AceitarConviteParceiraRequest
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.ERRO_CONEXAO
import com.example.techmentor.data.TermosParceiraResponse
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
fun AceitarConviteScreen(navController: NavController) {
    var email by rememberSaveable { mutableStateOf("") }
    var codigo by rememberSaveable { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    var aceitaTermos by rememberSaveable { mutableStateOf(false) }
    var termos by remember { mutableStateOf<TermosParceiraResponse?>(null) }
    var carregandoTermos by remember { mutableStateOf(true) }
    var enviando by remember { mutableStateOf(false) }
    var erro by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun carregarTermos() {
        if (carregandoTermos) return
        carregandoTermos = true
        scope.launch {
            try {
                termos = ApiClient.api.termosDeParceria()
                aceitaTermos = false
                erro = null
            } catch (e: HttpException) {
                erro = "Não foi possível carregar os termos. Tente novamente."
            } catch (e: IOException) {
                erro = ERRO_CONEXAO
            } finally {
                carregandoTermos = false
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            termos = ApiClient.api.termosDeParceria()
        } catch (e: HttpException) {
            erro = "Não foi possível carregar os termos. Tente novamente."
        } catch (e: IOException) {
            erro = ERRO_CONEXAO
        } finally {
            carregandoTermos = false
        }
    }

    fun concluirCadastro() {
        if (enviando) return
        val emailLimpo = email.trim().lowercase()
        erro = when {
            !Patterns.EMAIL_ADDRESS.matcher(emailLimpo).matches() -> "Informe o e-mail que recebeu o convite."
            codigo.isBlank() -> "Informe o código enviado por e-mail."
            termos == null -> "Carregue os Termos de Parceria antes de continuar."
            !aceitaTermos -> "É preciso ler e aceitar os Termos de Parceria."
            senha.length < 8 -> "A senha precisa ter pelo menos 8 caracteres."
            senha != confirmarSenha -> "As senhas não coincidem."
            else -> null
        }
        if (erro != null) return

        val versao = termos!!.versao
        enviando = true
        scope.launch {
            try {
                ApiClient.api.aceitarConviteDeParceria(
                    AceitarConviteParceiraRequest(
                        email = emailLimpo,
                        codigo = codigo.trim(),
                        senha = senha,
                        aceitaTermos = true,
                        versaoTermos = versao
                    )
                )
                val login = navController.previousBackStackEntry
                if (login?.destination?.route == "login") {
                    login.savedStateHandle.set("emailCriado", emailLimpo)
                    navController.popBackStack()
                } else {
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                }
            } catch (e: HttpException) {
                erro = com.example.techmentor.ui.mentoria.mensagemDaApi(e)
                    ?: "Não foi possível concluir o cadastro. Confira o código e os termos."
                if (erro?.contains("termos", ignoreCase = true) == true) carregarTermos()
            } catch (e: IOException) {
                erro = ERRO_CONEXAO
            } finally {
                enviando = false
            }
        }
    }

    AuthScaffold {
        BrandHeader()
        Spacer(Modifier.height(36.dp))
        AuthHeading(
            eyebrow = "CONVITE DE PARCERIA",
            title = "Ative sua conta de parceira.",
            subtitle = "Use o código recebido por e-mail e escolha uma senha para acessar o TechMentor."
        )
        Spacer(Modifier.height(22.dp))

        AuthField(
            label = "E-mail do convite",
            value = email,
            onValueChange = { email = it; erro = null },
            placeholder = "contato@instituicao.com",
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions()
        )
        Spacer(Modifier.height(13.dp))
        AuthField(
            label = "Código recebido por e-mail",
            value = codigo,
            onValueChange = { codigo = it.uppercase().take(20); erro = null },
            placeholder = "K7M2-9QXA",
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions()
        )
        Spacer(Modifier.height(18.dp))

        Text("Leia os termos", color = AuthColors.TextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        when {
            carregandoTermos -> Text("Carregando os Termos de Parceria…", color = AuthColors.TextMuted, fontSize = 12.sp)
            termos != null -> {
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color(0xFFFAF9FC))
                        .border(1.dp, AuthColors.FieldBorder, RoundedCornerShape(13.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(termos!!.titulo, color = AuthColors.TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Versão ${termos!!.versao}", color = AuthColors.TextMuted, fontSize = 10.sp)
                    Text(termos!!.texto, color = AuthColors.TextLabel, fontSize = 12.sp, lineHeight = 18.sp)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = aceitaTermos, onCheckedChange = { aceitaTermos = it })
                    Text("Li e aceito os Termos de Parceria", color = AuthColors.TextLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            else -> OutlinedAuthButton(text = "Carregar os termos", onClick = ::carregarTermos)
        }

        Spacer(Modifier.height(8.dp))
        AuthField(
            label = "Crie uma senha (mínimo 8 caracteres)",
            value = senha,
            onValueChange = { senha = it.take(72); erro = null },
            placeholder = "••••••••",
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(),
            isPassword = true
        )
        Spacer(Modifier.height(12.dp))
        AuthField(
            label = "Confirme sua senha",
            value = confirmarSenha,
            onValueChange = { confirmarSenha = it.take(72); erro = null },
            placeholder = "••••••••",
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(),
            isPassword = true
        )

        erro?.let { Text(it, Modifier.padding(top = 10.dp), color = AuthColors.Error, fontSize = 12.sp) }
        Spacer(Modifier.height(18.dp))
        GradientButton(
            text = if (enviando) "Concluindo cadastro…" else "Aceitar termos e criar conta",
            onClick = ::concluirCadastro
        )
        Spacer(Modifier.height(12.dp))
        OutlinedAuthButton(text = "Voltar ao login", onClick = { navController.popBackStack() })
    }
}
