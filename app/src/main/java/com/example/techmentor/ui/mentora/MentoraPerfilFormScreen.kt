package com.example.techmentor.ui.mentora

import com.example.techmentor.ui.auth.LegendaDeObrigatorio
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.Sessao
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.AuthField
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.auth.OutlinedAuthButton
import com.example.techmentor.ui.components.TopBarMarca
import com.example.techmentor.ui.mentoria.ChipOpcao
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.theme.ManropeFamily
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.techmentor.ui.components.AvatarEditavel
import com.example.techmentor.ui.components.reduzirFoto

private const val LIMITE_BIO = 2000

/**
 * Formulário do perfil de mentora. Dois modos:
 * - modoEdicao = false: primeiro acesso. Sem barra inferior e sem voltar, até preencher.
 * - modoEdicao = true: aba Perfil. Já vem preenchido e tem a barra inferior.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MentoraPerfilFormScreen(
    navController: NavController,
    modoEdicao: Boolean,
    viewModel: MentoraPerfilFormViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    var cargo by rememberSaveable { mutableStateOf("") }
    var empresa by rememberSaveable { mutableStateOf("") }
    var anos by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var areasEscolhidas by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var preenchido by rememberSaveable { mutableStateOf(false) }
    var tentou by rememberSaveable { mutableStateOf(false) }
    val contexto = LocalContext.current

    val escolherFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            reduzirFoto(contexto, uri)?.let { viewModel.enviarFoto(it) }
        }
    }

    fun sair() {
        Sessao.sair()
        navController.navigate("login") {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    // No primeiro acesso não dá para voltar sem preencher
    BackHandler(enabled = !modoEdicao) { }

    LaunchedEffect(Unit) {
        viewModel.carregar()
    }

    LaunchedEffect(estado.sessaoExpirada) {
        if (estado.sessaoExpirada) sair()
    }

    // Quando os dados chegam, preenche os campos uma vez só (no modo edição, com o perfil atual)
    LaunchedEffect(estado.carregando, estado.perfilAtual) {
        if (!preenchido && !estado.carregando) {
            estado.perfilAtual?.let { perfil ->
                cargo = perfil.cargo.orEmpty()
                empresa = perfil.empresa.orEmpty()
                anos = perfil.anosExperiencia.toString()
                bio = perfil.bio.orEmpty()
                // a API devolve os NOMES das áreas; o formulário trabalha com os ids
                areasEscolhidas = perfil.areas.orEmpty().mapNotNull { nome ->
                    estado.areasDisponiveis.firstOrNull { it.nome == nome }?.id
                }
            }
            preenchido = true
        }
    }

    // Depois de salvar: no primeiro acesso volta para a tela inicial; na edição só mostra o aviso
    LaunchedEffect(estado.salvo) {
        if (estado.salvo && !modoEdicao) {
            viewModel.limparSalvo()
            navController.popBackStack()
        }
    }

    val bioOk = bio.trim().isNotEmpty()
    val cargoOk = cargo.trim().isNotEmpty()
    val empresaOk = empresa.trim().isNotEmpty()
    val areasOk = areasEscolhidas.isNotEmpty()

    var novaAreaAberta by rememberSaveable { mutableStateOf(false) }
    var nomeNovaArea by rememberSaveable { mutableStateOf("") }

    if (novaAreaAberta) {
        DialogoNovaArea(
            nome = nomeNovaArea,
            aoMudarNome = { nomeNovaArea = it },
            criando = estado.criandoArea,
            erro = estado.erroDaArea,
            aoCriar = {
                viewModel.criarArea(nomeNovaArea) { nova ->
                    areasEscolhidas = areasEscolhidas + nova.id
                    nomeNovaArea = ""
                    novaAreaAberta = false
                }
            },
            aoCancelar = {
                novaAreaAberta = false
                nomeNovaArea = ""
                viewModel.limparErroDaArea()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
    ) {

        TopBarMarca()

        when {

            estado.carregando -> Column(
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EsqueletoBloco(altura = 56.dp)
                EsqueletoBloco(altura = 56.dp)
                EsqueletoBloco(altura = 120.dp)
            }

            estado.erro != null && estado.areasDisponiveis.isEmpty() -> Column(modifier = Modifier.weight(1f)) {
                EstadoErro(
                    mensagem = estado.erro.orEmpty(),
                    onTentarDeNovo = { viewModel.carregar() }
                )
            }

            else -> Column(
                modifier = Modifier
                    .weight(1f)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Column {

                    Text(
                        text = if (modoEdicao) "Seu perfil de mentora" else "Complete seu perfil",
                        fontFamily = ManropeFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                        color = MentoriaCores.TextoEscuro
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = if (modoEdicao) {
                            "É assim que as alunas veem você."
                        } else {
                            "Conte um pouco sobre você para as alunas te conhecerem."
                        },
                        fontSize = 13.sp,
                        color = Color(0xFF797486)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LegendaDeObrigatorio()
                }

                FotoDePerfil(
                    nome = estado.perfilAtual?.nome ?: "Você",
                    fotoUrl = estado.fotoUrl,
                    enviando = estado.enviandoFoto,
                    erro = estado.erroFoto,
                    onEscolher = {
                        escolherFoto.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                AuthField(
                    label = "Cargo",
                    obrigatorio = true,
                    value = cargo,
                    onValueChange = { cargo = it },
                    placeholder = "Ex.: Engenheira de software",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions.Default
                )
                if (tentou && !cargoOk) MensagemDoCampo("Informe seu cargo.")

                AuthField(
                    label = "Empresa",
                    obrigatorio = true,
                    value = empresa,
                    onValueChange = { empresa = it },
                    placeholder = "Ex.: Acme Tecnologia",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions.Default
                )
                if (tentou && !empresaOk) MensagemDoCampo("Informe sua empresa.")

                AuthField(
                    label = "Anos de experiência",
                    value = anos,
                    onValueChange = { anos = it.filter { c -> c.isDigit() }.take(2) },
                    placeholder = "Ex.: 5",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions.Default
                )

                AuthField(
                    label = "Apresentação",
                    obrigatorio = true,
                    value = bio,
                    onValueChange = { if (it.length <= LIMITE_BIO) bio = it },
                    placeholder = "Conte sua trajetória e como pode ajudar",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    keyboardActions = KeyboardActions.Default,
                    multilinha = true
                )
                if (tentou && !bioOk) MensagemDoCampo("Escreva uma apresentação.")

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                    Text(
                        text = buildAnnotatedString {
                            append("Áreas de atuação")
                            withStyle(SpanStyle(color = AuthColors.Error)) { append(" *") }
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF514C58)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        estado.areasDisponiveis.forEach { area ->
                            ChipOpcao(
                                texto = area.nome,
                                selecionado = area.id in areasEscolhidas,
                                onClick = {
                                    areasEscolhidas = if (area.id in areasEscolhidas) {
                                        areasEscolhidas - area.id
                                    } else {
                                        areasEscolhidas + area.id
                                    }
                                }
                            )
                        }

                        // NOVO: o último chip abre a criação de uma área nova
                        ChipOpcao(
                            texto = "+ Nova área",
                            selecionado = false,
                            onClick = { novaAreaAberta = true }
                        )
                    }

                    if (tentou && !areasOk) MensagemDoCampo("Escolha pelo menos uma área.")
                }

                if (estado.erroAoSalvar != null) MensagemDoCampo(estado.erroAoSalvar.orEmpty())

                if (estado.salvo && modoEdicao) {
                    Text(
                        text = "✓ Perfil salvo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuthColors.Success
                    )
                }

                GradientButton(
                    text = when {
                        estado.salvando -> "Salvando..."
                        modoEdicao -> "Salvar alterações"
                        else -> "Salvar e continuar"
                    },
                    onClick = {
                        tentou = true
                        if (cargoOk && empresaOk && bioOk && areasOk && !estado.salvando) {
                            viewModel.salvar(
                                cargo = cargo.trim(),
                                empresa = empresa.trim(),
                                anos = anos.toIntOrNull(),
                                bio = bio.trim(),
                                areasIds = areasEscolhidas
                            )
                        }
                    }
                )

                OutlinedAuthButton(
                    text = "Sair da conta",
                    onClick = { sair() }
                )
            }
        }

        if (modoEdicao) {
            BarraInferiorMentora(
                navController = navController,
                rotaAtual = "mentora/perfil"
            )
        }
    }
}

@Composable
private fun MensagemDoCampo(texto: String) {
    Text(
        text = texto,
        modifier = Modifier.fillMaxWidth(),
        fontSize = 11.sp,
        color = AuthColors.Error
    )
}

@Composable
private fun FotoDePerfil(
    nome: String,
    fotoUrl: String?,
    enviando: Boolean,
    erro: String?,
    onEscolher: () -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        AvatarEditavel(
            nome = nome,
            fotoUrl = fotoUrl,
            enviando = enviando,
            onTrocarFoto = onEscolher,
            tamanho = 84.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = "Foto de perfil",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20202A)
            )

            Text(
                text = if (enviando) "Enviando a foto..." else "Toque na câmera para trocar, ou na foto para ampliar.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color(0xFF797486)
            )

            if (erro != null) {
                Text(
                    text = erro,
                    fontSize = 11.sp,
                    color = AuthColors.Error
                )
            }
        }
    }
}

// NOVO: o diálogo para criar uma área de atuação
@Composable
private fun DialogoNovaArea(
    nome: String,
    aoMudarNome: (String) -> Unit,
    criando: Boolean,
    erro: String?,
    aoCriar: () -> Unit,
    aoCancelar: () -> Unit
) {

    AlertDialog(
        onDismissRequest = { if (!criando) aoCancelar() },
        containerColor = Color.White,
        title = {
            Text(
                text = "Nova área de atuação",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20202A)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                Text(
                    text = "Não achou a sua área? Crie uma nova: ela fica disponível para todas as mentoras.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF797486)
                )

                AuthField(
                    label = "Nome da área",
                    value = nome,
                    onValueChange = { if (it.length <= 80) aoMudarNome(it) },
                    placeholder = "Ex.: Segurança da Informação",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions.Default
                )

                if (erro != null) {
                    Text(text = erro, fontSize = 11.sp, color = AuthColors.Error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = aoCriar, enabled = !criando && nome.isNotBlank()) {
                Text(
                    text = if (criando) "Criando..." else "Adicionar",
                    fontWeight = FontWeight.Bold,
                    color = MentoriaCores.RoxoEscuro
                )
            }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar, enabled = !criando) {
                Text(text = "Cancelar", color = Color(0xFF797486))
            }
        }
    )
}