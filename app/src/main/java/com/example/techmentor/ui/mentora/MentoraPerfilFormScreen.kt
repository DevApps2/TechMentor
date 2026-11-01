package com.example.techmentor.ui.mentora

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
                }

                AuthField(
                    label = "Cargo",
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
                        text = "Áreas de atuação",
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