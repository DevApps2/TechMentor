package com.example.techmentor.ui.seguranca

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.MentoriaCores

private val MOTIVOS = listOf(
    "Assédio",
    "Comportamento inadequado",
    "Spam ou fraude",
    "Discurso de ódio",
    "Perfil falso",
    "Outro"
)

private val CorPlaceholder = Color(0xFF9A96A3)
private val CorBordaCampo = Color(0xFFD9D6E0)


// ======================================================
// DENUNCIAR USUÁRIO (simulado: nada é enviado)
// ======================================================

@Composable
internal fun DenunciarUsuarioTela(onVoltar: () -> Unit) {

    var usuario by rememberSaveable { mutableStateOf("") }
    var motivo by rememberSaveable { mutableStateOf<String?>(null) }
    var descricao by rememberSaveable { mutableStateOf("") }
    var tentou by rememberSaveable { mutableStateOf(false) }
    var enviado by rememberSaveable { mutableStateOf(false) }

    if (enviado) {

        // Voltar do celular na confirmação volta para o formulário
        BackHandler { enviado = false }

        ConfirmacaoSimulada(
            tituloDoCabecalho = "Denunciar usuário",
            titulo = "Relato simulado",
            texto = "Esta é uma confirmação demonstrativa. Nenhum relato foi enviado.",
            textoDoBotao = "Voltar à segurança",
            onVoltarCabecalho = { enviado = false },
            onBotao = onVoltar
        )

        return
    }

    val motivoOk = motivo != null
    val descricaoOk = descricao.trim().length >= 10

    TelaInterna(titulo = "Denunciar usuário", onVoltar = onVoltar) {

        TituloDeSecao("Conte o que aconteceu")

        Spacer(modifier = Modifier.height(24.dp))

        CampoDeTexto(
            rotulo = "Nome ou identificador do usuário",
            valor = usuario,
            onMudar = { usuario = it },
            placeholder = "Ex.: usuário da conversa"
        )

        Spacer(modifier = Modifier.height(20.dp))

        CampoDeMotivo(
            selecionado = motivo,
            onSelecionar = { motivo = it }
        )

        if (tentou && !motivoOk) MensagemDeErro("Selecione um motivo.")

        Spacer(modifier = Modifier.height(20.dp))

        CampoDeTexto(
            rotulo = "Descrição",
            valor = descricao,
            onMudar = { descricao = it },
            placeholder = "Descreva a situação (mínimo de 10 caracteres)",
            linhas = 6
        )

        if (tentou && !descricaoOk) MensagemDeErro("Descreva a situação com pelo menos 10 caracteres.")

        Spacer(modifier = Modifier.height(24.dp))

        BotaoDegrade(
            texto = "Simular envio do relato",
            onClick = {
                tentou = true
                if (motivoOk && descricaoOk) enviado = true
            }
        )
    }
}


// ======================================================
// USUÁRIOS BLOQUEADOS (simulado: lista só na memória)
// ======================================================

@Composable
internal fun UsuariosBloqueadosTela(
    bloqueados: List<String>,
    onBloquear: (String) -> Unit,
    onDesbloquear: (String) -> Unit,
    onVoltar: () -> Unit
) {

    var nome by rememberSaveable { mutableStateOf("") }

    TelaInterna(titulo = "Usuários bloqueados", onVoltar = onVoltar) {

        TituloDeSecao("Usuários bloqueados")

        Spacer(modifier = Modifier.height(16.dp))

        if (bloqueados.isEmpty()) {

            CaixaTracejada(texto = "Nenhum usuário bloqueado nesta simulação.")

        } else {

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                bloqueados.forEach { bloqueado ->
                    LinhaBloqueado(
                        nome = bloqueado,
                        onDesbloquear = { onDesbloquear(bloqueado) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        CampoDeTexto(
            rotulo = "Simular bloqueio de usuário",
            valor = nome,
            onMudar = { nome = it },
            placeholder = "Nome demonstrativo"
        )

        Spacer(modifier = Modifier.height(16.dp))

        BotaoDegrade(
            texto = "Bloquear na simulação",
            onClick = {
                onBloquear(nome)
                nome = ""
            }
        )
    }
}

@Composable
private fun CaixaTracejada(texto: String) {

    val raio = 24.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(raio))
            .background(Color.White)
            .drawBehind {
                val espessura = 1.dp.toPx()
                drawRoundRect(
                    color = Color(0xFFCFCBD8),
                    topLeft = Offset(espessura / 2, espessura / 2),
                    size = Size(size.width - espessura, size.height - espessura),
                    cornerRadius = CornerRadius(raio.toPx(), raio.toPx()),
                    style = Stroke(
                        width = espessura,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
                    )
                )
            }
            .padding(horizontal = 24.dp, vertical = 30.dp)
    ) {

        Text(
            text = texto,
            fontSize = 16.sp,
            color = MentoriaCores.TextoMudo
        )
    }
}

@Composable
private fun LinhaBloqueado(nome: String, onDesbloquear: () -> Unit) {

    val forma = RoundedCornerShape(20.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = nome,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MentoriaCores.TextoEscuro
        )

        Text(
            text = "Desbloquear",
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClick = onDesbloquear)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MentoriaCores.Roxo
        )
    }
}


// ======================================================
// ORIENTAÇÕES e CANAIS DE AJUDA (vêm da API)
// ======================================================

@Composable
internal fun OrientacoesTela(
    estado: SegurancaUiState,
    onTentarDeNovo: () -> Unit,
    onVoltar: () -> Unit
) {

    TelaInterna(titulo = "Orientações", onVoltar = onVoltar) {

        TituloDeSecao("Cuide da sua experiência")

        Spacer(modifier = Modifier.height(16.dp))

        ConteudosOuEstado(
            estado = estado,
            itens = estado.orientacoes,
            mostrarContato = false,
            onTentarDeNovo = onTentarDeNovo
        )

        Spacer(modifier = Modifier.height(20.dp))

        BotaoContorno(
            texto = "Voltar à segurança",
            onClick = onVoltar,
            ocuparLargura = false
        )
    }
}

@Composable
internal fun CanaisDeAjudaTela(
    estado: SegurancaUiState,
    onTentarDeNovo: () -> Unit,
    onVoltar: () -> Unit
) {

    TelaInterna(titulo = "Canais de ajuda", onVoltar = onVoltar) {

        TituloDeSecao("Onde buscar ajuda")

        Spacer(modifier = Modifier.height(16.dp))

        ConteudosOuEstado(
            estado = estado,
            itens = estado.canaisDeAjuda,
            mostrarContato = true,
            onTentarDeNovo = onTentarDeNovo
        )

        Spacer(modifier = Modifier.height(16.dp))

        AvisoLateral(texto = "O TechMentor não oferece atendimento de emergência ou moderação neste protótipo.")
    }
}

/** Mostra "carregando", erro com botão de tentar de novo, ou a lista de cartões. */
@Composable
private fun ConteudosOuEstado(
    estado: SegurancaUiState,
    itens: List<ConteudoSegurancaResponse>,
    mostrarContato: Boolean,
    onTentarDeNovo: () -> Unit
) {

    when {

        estado.conteudos == null && estado.erro != null -> {

            val forma = RoundedCornerShape(24.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(forma)
                    .background(Color.White)
                    .border(1.dp, MentoriaCores.Borda, forma)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                Text(
                    text = estado.erro,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = MentoriaCores.TextoMudo
                )

                BotaoContorno(
                    texto = "Tentar de novo",
                    onClick = onTentarDeNovo,
                    ocuparLargura = false
                )
            }
        }

        estado.conteudos == null -> {

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                EsqueletoBloco(altura = 110.dp)
                EsqueletoBloco(altura = 110.dp)
                EsqueletoBloco(altura = 110.dp)
            }
        }

        itens.isEmpty() -> {
            CaixaTracejada(texto = "Nenhum conteúdo disponível por enquanto.")
        }

        else -> {

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                itens.forEach { item ->
                    CartaoConteudo(
                        titulo = item.titulo,
                        texto = item.descricao,
                        destaque = if (mostrarContato) item.contato else null
                    )
                }
            }
        }
    }
}


// ======================================================
// CAMPOS DE FORMULÁRIO
// ======================================================

@Composable
private fun MensagemDeErro(texto: String) {

    Text(
        text = texto,
        modifier = Modifier.padding(top = 6.dp, start = 4.dp),
        fontSize = 13.sp,
        color = Color(0xFFDC2626)
    )
}

@Composable
private fun RotuloDoCampo(texto: String) {

    Text(
        text = texto,
        modifier = Modifier.padding(bottom = 10.dp),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MentoriaCores.TextoEscuro
    )
}

@Composable
private fun CampoDeTexto(
    rotulo: String,
    valor: String,
    onMudar: (String) -> Unit,
    placeholder: String,
    linhas: Int = 1
) {

    Column {

        RotuloDoCampo(rotulo)

        OutlinedTextField(
            value = valor,
            onValueChange = onMudar,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 16.sp,
                    color = CorPlaceholder
                )
            },
            singleLine = linhas == 1,
            minLines = linhas,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = MentoriaCores.Roxo,
                unfocusedBorderColor = CorBordaCampo,
                cursorColor = MentoriaCores.Roxo
            )
        )
    }
}

@Composable
private fun CampoDeMotivo(
    selecionado: String?,
    onSelecionar: (String) -> Unit
) {

    var aberto by rememberSaveable { mutableStateOf(false) }

    val forma = RoundedCornerShape(20.dp)

    Column {

        RotuloDoCampo("Motivo")

        Box {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(forma)
                    .background(Color.White)
                    .border(1.dp, CorBordaCampo, forma)
                    .clickable(role = Role.DropdownList) { aberto = true }
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = selecionado ?: "Selecione um motivo",
                    fontSize = 16.sp,
                    fontWeight = if (selecionado == null) FontWeight.Bold else FontWeight.Medium,
                    color = MentoriaCores.TextoEscuro
                )

                Text(
                    text = "▾",
                    fontSize = 16.sp,
                    color = MentoriaCores.TextoMudo
                )
            }

            DropdownMenu(
                expanded = aberto,
                onDismissRequest = { aberto = false }
            ) {

                MOTIVOS.forEach { motivo ->
                    DropdownMenuItem(
                        text = { Text(motivo) },
                        onClick = {
                            onSelecionar(motivo)
                            aberto = false
                        }
                    )
                }
            }
        }
    }
}
