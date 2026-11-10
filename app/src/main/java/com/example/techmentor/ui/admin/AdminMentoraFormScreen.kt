package com.example.techmentor.ui.admin

import com.example.techmentor.ui.auth.LegendaDeObrigatorio
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.auth.OutlinedAuthButton
import com.example.techmentor.ui.mentoria.AvatarMentora
import com.example.techmentor.ui.mentoria.CabecalhoVoltar
import com.example.techmentor.ui.mentoria.ChipOpcao
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores
import com.example.techmentor.ui.mentoria.cargoEEmpresa

// Dois dígitos bastam para anos de experiência
private const val MAXIMO_DE_DIGITOS_DOS_ANOS = 2


/**
 * mentoraId nulo = cadastrar uma nova mentora (conta + perfil).
 * mentoraId preenchido = editar o perfil dela (é o ID do perfil de mentora, o mesmo da lista) e poder excluir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminMentoraFormScreen(
    navController: NavController,
    mentoraId: Long?,
    viewModel: AdminMentoraFormViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()
    val foco = LocalFocusManager.current
    val editando = mentoraId != null

    // Os textos ficam aqui (e não no ViewModel) para a digitação não "pular"; rememberSaveable guarda ao girar a tela
    var nome by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var cargo by rememberSaveable { mutableStateOf("") }
    var empresa by rememberSaveable { mutableStateOf("") }
    var anos by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var novaArea by rememberSaveable { mutableStateOf("") }

    var senhaGerada by remember { mutableStateOf<String?>(null) }

    // No cadastro o formulário já nasce "preenchido" (vazio); na edição espera a mentora chegar da API
    var preenchido by rememberSaveable { mutableStateOf(!editando) }
    var pedindoConfirmacao by remember { mutableStateOf(false) }

    EfeitoSessaoExpirada(expirada = estado.sessaoExpirada, navController = navController)

    LaunchedEffect(mentoraId) {
        viewModel.carregar(mentoraId)
    }

    // Copia a mentora carregada para os campos, uma única vez
    LaunchedEffect(estado.original) {
        val original = estado.original
        if (original != null && !preenchido) {
            cargo = original.cargo.orEmpty()
            empresa = original.empresa.orEmpty()
            anos = original.anosExperiencia.toString()
            bio = original.bio.orEmpty()
            preenchido = true
        }
    }

    // Cadastrou, salvou ou excluiu: volta para a lista
    LaunchedEffect(estado.concluido) {
        if (estado.concluido) navController.popBackStack()
    }

    // Quando uma área nova é criada, o campo de nome esvazia
    LaunchedEffect(estado.areas?.size) {
        novaArea = ""
    }

    // Esta tela não tem a barra inferior (o teclado ocuparia o lugar dela), então respeita a barra do sistema e o teclado
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentoriaCores.Fundo)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {

        CabecalhoVoltar(
            titulo = if (editando) "Editar mentora" else "Cadastrar mentora",
            onVoltar = { navController.popBackStack() }
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {

            val erro = estado.erro
            val original = estado.original

            when {
                erro != null -> EstadoErro(
                    mensagem = erro,
                    onTentarDeNovo = { viewModel.carregar(mentoraId) }
                )

                estado.areas == null || !preenchido -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EsqueletoBloco(altura = 70.dp)
                    EsqueletoBloco(altura = 70.dp)
                    EsqueletoBloco(altura = 70.dp)
                }

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    if (original != null) {

                        // Edição: mostra quem é (nome e e-mail não mudam por aqui)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            AvatarMentora(
                                nome = original.nome,
                                fotoUrl = original.fotoUrl,
                                tamanho = 48.dp,
                                raio = 16.dp,
                                tamanhoDaInicial = 20.sp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {

                                Text(
                                    text = original.nome,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MentoriaCores.TextoEscuro
                                )

                                Text(
                                    text = cargoEEmpresa(original),
                                    fontSize = 12.sp,
                                    color = MentoriaCores.TextoMudo
                                )
                            }
                        }

                    } else {

                        // Cadastro: dados da conta
                        Text(
                            text = "Cria a conta de acesso e o perfil de mentora. Ela entra com o e-mail e a senha provisória e pode trocar a senha em \"Esqueci minha senha\".",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MentoriaCores.TextoMudo
                        )

                        LegendaDeObrigatorio()

                        CampoDeTexto(
                            rotulo = "Nome completo",
                            obrigatorio = true,
                            valor = nome,
                            aoMudar = { nome = it },
                            dica = "Ex.: Maria Silva"
                        )

                        CampoDeTexto(
                            rotulo = "E-mail",
                            obrigatorio = true,
                            valor = email,
                            aoMudar = { email = it },
                            dica = "mentora@email.com",
                            teclado = KeyboardType.Email,
                            maiusculaNoInicio = false
                        )

                        CampoDeTexto(
                            rotulo = "Senha provisória",
                            obrigatorio = true,
                            valor = senha,
                            aoMudar = { senha = it },
                            dica = "Mínimo de 8 caracteres",
                            senha = true,
                            maiusculaNoInicio = false
                        )

                        OutlinedAuthButton(
                            text = "Gerar senha",
                            onClick = {
                                val nova = gerarSenhaProvisoria()
                                senha = nova
                                senhaGerada = nova
                            }
                        )

                        // Só mostra enquanto o campo ainda tem a senha gerada (se o admin digitar outra, some)
                        senhaGerada?.takeIf { it == senha }?.let { gerada ->
                            SelectionContainer {
                                Text(
                                    text = "Senha gerada: $gerada\nAnote ou copie (segure o dedo no texto) e entregue à mentora: depois de salvar, ela não aparece de novo.",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = Color(0xFF514C58)
                                )
                            }
                        }
                    }

                    CampoDeTexto(
                        rotulo = "Cargo",
                        valor = cargo,
                        aoMudar = { cargo = it },
                        dica = "Ex.: Engenheira de Software Sênior"
                    )

                    CampoDeTexto(
                        rotulo = "Empresa",
                        valor = empresa,
                        aoMudar = { empresa = it },
                        dica = "Ex.: Acme Tecnologia"
                    )

                    CampoDeTexto(
                        rotulo = "Anos de experiência",
                        valor = anos,
                        aoMudar = { anos = it.filter { c -> c.isDigit() }.take(MAXIMO_DE_DIGITOS_DOS_ANOS) },
                        dica = "Ex.: 8",
                        teclado = KeyboardType.Number,
                        maiusculaNoInicio = false
                    )

                    CampoDeTexto(
                        rotulo = "Apresentação",
                        valor = bio,
                        aoMudar = { bio = it },
                        dica = "Conte um pouco da trajetória dela",
                        multilinha = true
                    )

                    Text(
                        text = "Áreas de atuação",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MentoriaCores.TextoMudo
                    )

                    val areas = estado.areas.orEmpty()

                    if (areas.isEmpty()) {

                        Text(
                            text = "Nenhuma área cadastrada ainda. Crie a primeira logo abaixo.",
                            fontSize = 12.sp,
                            color = MentoriaCores.TextoMudo
                        )

                    } else {

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            areas.forEach { area ->
                                ChipOpcao(
                                    texto = area.nome,
                                    selecionado = area.id in estado.areasSelecionadas,
                                    onClick = { viewModel.alternarArea(area.id) }
                                )
                            }
                        }
                    }

                    CampoDeTexto(
                        rotulo = "Não achou a área? Crie uma nova",
                        valor = novaArea,
                        aoMudar = { novaArea = it },
                        dica = "Ex.: Segurança da Informação"
                    )

                    OutlinedAuthButton(
                        text = if (estado.criandoArea) "Criando..." else "Adicionar área",
                        onClick = {
                            foco.clearFocus()
                            viewModel.criarArea(novaArea)
                        }
                    )

                    if (estado.erroDaArea != null) {
                        MensagemDeErro(texto = estado.erroDaArea.orEmpty())
                    }

                    if (estado.erroAoSalvar != null) {
                        MensagemDeErro(texto = estado.erroAoSalvar.orEmpty())
                    }

                    GradientButton(
                        text = when {
                            estado.salvando -> "Salvando..."
                            editando -> "Salvar alterações"
                            else -> "Cadastrar mentora"
                        },
                        onClick = {
                            foco.clearFocus()
                            if (mentoraId == null) {
                                viewModel.cadastrar(
                                    nome = nome,
                                    email = email,
                                    senha = senha,
                                    cargo = cargo,
                                    empresa = empresa,
                                    anos = anos,
                                    bio = bio
                                )
                            } else {
                                viewModel.atualizar(
                                    mentoraId = mentoraId,
                                    cargo = cargo,
                                    empresa = empresa,
                                    anos = anos,
                                    bio = bio
                                )
                            }
                        }
                    )

                    if (editando) {

                        Spacer(modifier = Modifier.height(6.dp))

                        BotaoDePerigo(
                            texto = if (estado.excluindo) "Excluindo..." else "Excluir perfil de mentora",
                            onClick = { pedindoConfirmacao = true }
                        )
                    }
                }
            }
        }
    }

    if (pedindoConfirmacao && mentoraId != null) {

        DialogoDeConfirmacao(
            titulo = "Excluir perfil de mentora?",
            texto = "Ela deixa de aparecer na lista de mentoras. A conta de acesso continua existindo, e não é possível excluir quem já tem mentorias agendadas.",
            textoDoBotao = "Excluir",
            onConfirmar = {
                pedindoConfirmacao = false
                viewModel.excluir(mentoraId)
            },
            onCancelar = { pedindoConfirmacao = false }
        )
    }
}

private fun gerarSenhaProvisoria(): String {
    // sem letras parecidas (0/O, 1/l/I) para quem for ler e digitar
    val letras = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789"
    val aleatorio = java.security.SecureRandom()
    return (1..12).map { letras[aleatorio.nextInt(letras.length)] }.joinToString("")
}
