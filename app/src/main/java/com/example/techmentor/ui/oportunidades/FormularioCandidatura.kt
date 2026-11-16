package com.example.techmentor.ui.oportunidades

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.techmentor.data.OportunidadeResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.AuthField
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.auth.LegendaDeObrigatorio
import com.example.techmentor.ui.auth.OutlinedAuthButton
import com.example.techmentor.ui.mentoria.CabecalhoVoltar
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily

private const val LIMITE_PDF_BYTES = 5L * 1024 * 1024
private const val LIMITE_MENSAGEM = 500

/** Tudo o que a candidata preencheu no formulário. */
internal data class DadosCandidatura(
    val nome: String,
    val email: String,
    val telefone: String,
    val cidade: String,
    val linkProfissional: String,
    val mensagem: String,
    // Currículo em PDF (opcional): endereço do arquivo no aparelho e o nome dele
    val curriculo: Uri?,
    val nomeDoCurriculo: String?
)

private class ArquivoEscolhido(val nome: String, val bytes: Long?)

/** Descobre o nome e o tamanho do arquivo que a usuária escolheu. */
private fun lerArquivo(context: Context, uri: Uri): ArquivoEscolhido {

    var nome = "curriculo.pdf"
    var tamanho: Long? = null

    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {

                val colunaNome = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val colunaTamanho = cursor.getColumnIndex(OpenableColumns.SIZE)

                if (colunaNome >= 0) cursor.getString(colunaNome)?.let { nome = it }
                if (colunaTamanho >= 0 && !cursor.isNull(colunaTamanho)) tamanho = cursor.getLong(colunaTamanho)
            }
        }
    }

    return ArquivoEscolhido(nome, tamanho)
}

private fun formatarTamanho(bytes: Long?): String {

    if (bytes == null) return ""

    return if (bytes < 1024 * 1024) {
        "${maxOf(1L, bytes / 1024)} KB"
    } else {
        "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}


// ======================================================
// FORMULÁRIO
// ======================================================

@Composable
internal fun FormularioCandidatura(
    oportunidade: OportunidadeResponse,
    enviando: Boolean,
    erroEnvio: String?,
    onEnviar: (DadosCandidatura) -> Unit,
    onVoltar: () -> Unit
) {

    val context = LocalContext.current

    var nome by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf(Sessao.email.orEmpty()) }
    var telefone by rememberSaveable { mutableStateOf("") }
    var cidade by rememberSaveable { mutableStateOf("") }
    var link by rememberSaveable { mutableStateOf("") }
    var mensagem by rememberSaveable { mutableStateOf("") }

    var curriculoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var erroArquivo by rememberSaveable { mutableStateOf<String?>(null) }

    // Só mostra os avisos de campo vazio depois da primeira tentativa de enviar
    var tentou by rememberSaveable { mutableStateOf(false) }

    val arquivo = remember(curriculoUri) { curriculoUri?.let { lerArquivo(context, it) } }

    val seletorDePdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->

        if (uri != null) {

            val info = lerArquivo(context, uri)
            val tipo = context.contentResolver.getType(uri)
            val ehPdf = tipo == "application/pdf" || info.nome.endsWith(".pdf", ignoreCase = true)

            when {
                !ehPdf -> erroArquivo = "Escolha um arquivo em PDF."
                (info.bytes ?: 0L) > LIMITE_PDF_BYTES -> erroArquivo = "O PDF deve ter no máximo 5 MB."
                else -> {
                    erroArquivo = null
                    curriculoUri = uri
                }
            }
        }
    }

    val nomeOk = nome.trim().length >= 3
    val emailOk = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val telefoneOk = telefone.count { it.isDigit() } in 10..11

    val seguinte = KeyboardOptions(imeAction = ImeAction.Next)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        CabecalhoVoltar(
            titulo = "Candidatura",
            onVoltar = onVoltar
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Resumo da vaga
            Column {

                Text(
                    text = "CANDIDATURA PARA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = MentoriaCores.Roxo
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = oportunidade.titulo,
                    fontFamily = ManropeFamily,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MentoriaCores.TextoEscuro
                )

                if (!oportunidade.organizacao.isNullOrBlank()) {

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = oportunidade.organizacao,
                        fontSize = 13.sp,
                        color = MentoriaCores.TextoMudo
                    )
                }
            }

            LegendaDeObrigatorio()

            // ---- Dados básicos ----

            AuthField(
                label = "Nome completo",
                value = nome,
                onValueChange = { nome = it },
                placeholder = "Como você se chama",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions.Default,
                obrigatorio = true
            )
            if (tentou && !nomeOk) ErroDoCampo("Informe seu nome completo.")

            AuthField(
                label = "E-mail",
                value = email,
                onValueChange = { email = it },
                placeholder = "voce@email.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions.Default,
                obrigatorio = true
            )
            if (tentou && !emailOk) ErroDoCampo("Informe um e-mail válido.")

            AuthField(
                label = "Telefone / WhatsApp",
                value = telefone,
                onValueChange = { telefone = it.take(20) },
                placeholder = "(11) 99999-9999",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions.Default,
                obrigatorio = true
            )
            if (tentou && !telefoneOk) ErroDoCampo("Informe o telefone com DDD.")

            AuthField(
                label = "Cidade e estado",
                value = cidade,
                onValueChange = { cidade = it },
                placeholder = "Ex.: São Paulo, SP",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions.Default
            )

            AuthField(
                label = "LinkedIn ou portfólio",
                value = link,
                onValueChange = { link = it },
                placeholder = "https://...",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions.Default
            )

            Column {

                AuthField(
                    label = "Conte um pouco sobre você",
                    value = mensagem,
                    onValueChange = { mensagem = it.take(LIMITE_MENSAGEM) },
                    placeholder = "Por que essa oportunidade é interessante para você?",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    keyboardActions = KeyboardActions.Default,
                    multilinha = true
                )

                Text(
                    text = "${mensagem.length}/$LIMITE_MENSAGEM",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    fontSize = 10.sp,
                    textAlign = TextAlign.End,
                    color = AuthColors.TextMuted
                )
            }

            // ---- Currículo em PDF ----

            CampoDeCurriculo(
                arquivo = arquivo,
                erro = erroArquivo,
                onEscolher = { seletorDePdf.launch(arrayOf("application/pdf")) },
                onRemover = {
                    curriculoUri = null
                    erroArquivo = null
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (erroEnvio != null) {
                Text(
                    text = erroEnvio,
                    fontSize = 11.sp,
                    color = AuthColors.Error
                )
            }

            GradientButton(
                text = if (enviando) "Enviando..." else "Enviar candidatura",
                onClick = {

                    if (enviando) return@GradientButton

                    tentou = true

                    if (nomeOk && emailOk && telefoneOk) {
                        onEnviar(
                            DadosCandidatura(
                                nome = nome.trim(),
                                email = email.trim(),
                                telefone = telefone.trim(),
                                cidade = cidade.trim(),
                                linkProfissional = link.trim(),
                                mensagem = mensagem.trim(),
                                curriculo = curriculoUri,
                                nomeDoCurriculo = arquivo?.nome
                            )
                        )
                    }
                }
            )
        }
    }
}


// ======================================================
// PEDAÇOS DO FORMULÁRIO
// ======================================================

@Composable
private fun ErroDoCampo(texto: String) {

    Text(
        text = texto,
        fontSize = 11.sp,
        color = AuthColors.Error
    )
}

@Composable
private fun CampoDeCurriculo(
    arquivo: ArquivoEscolhido?,
    erro: String?,
    onEscolher: () -> Unit,
    onRemover: () -> Unit
) {

    Column {

        Text(
            text = "Currículo em PDF",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AuthColors.TextLabel
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (arquivo == null) {

            OutlinedAuthButton(
                text = "Anexar PDF",
                onClick = onEscolher
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Opcional. Somente arquivos PDF de até 5 MB.",
                fontSize = 11.sp,
                color = AuthColors.TextMuted
            )

        } else {

            val forma = RoundedCornerShape(14.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(forma)
                    .background(Color.White)
                    .border(1.dp, MentoriaCores.Borda, forma)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0xFFFCE7F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PDF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFDB2777)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = arquivo.nome,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MentoriaCores.TextoEscuro
                    )

                    val tamanho = formatarTamanho(arquivo.bytes)

                    if (tamanho.isNotEmpty()) {
                        Text(
                            text = tamanho,
                            fontSize = 11.sp,
                            color = MentoriaCores.TextoMudo
                        )
                    }
                }

                Text(
                    text = "Remover",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onRemover)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuthColors.Error
                )
            }
        }

        if (erro != null) {

            Spacer(modifier = Modifier.height(6.dp))

            ErroDoCampo(erro)
        }
    }
}
