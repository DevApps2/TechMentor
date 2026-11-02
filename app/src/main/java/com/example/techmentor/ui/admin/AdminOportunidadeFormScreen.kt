package com.example.techmentor.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.mentoria.CabecalhoVoltar
import com.example.techmentor.ui.mentoria.ChipOpcao
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.EstadoErro
import com.example.techmentor.ui.mentoria.MentoriaCores


/** oportunidadeId nulo = cadastrar uma nova; preenchido = editar (e poder excluir) a existente. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminOportunidadeFormScreen(
    navController: NavController,
    oportunidadeId: Long?,
    viewModel: AdminOportunidadeFormViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()
    val foco = LocalFocusManager.current
    val editando = oportunidadeId != null

    // Os textos ficam aqui (e não no ViewModel) para a digitação não "pular"; rememberSaveable guarda ao girar a tela
    var tipo by rememberSaveable { mutableStateOf("") }
    var titulo by rememberSaveable { mutableStateOf("") }
    var organizacao by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }
    var nivel by rememberSaveable { mutableStateOf("") }
    var localidade by rememberSaveable { mutableStateOf("") }
    var linkExterno by rememberSaveable { mutableStateOf("") }
    var prazo by rememberSaveable { mutableStateOf("") }
    var descricao by rememberSaveable { mutableStateOf("") }
    var requisitos by rememberSaveable { mutableStateOf("") }
    var beneficios by rememberSaveable { mutableStateOf("") }

    // No cadastro o formulário já nasce "preenchido" (vazio); na edição espera a oportunidade chegar da API
    var preenchido by rememberSaveable { mutableStateOf(!editando) }
    var pedindoConfirmacao by remember { mutableStateOf(false) }

    EfeitoSessaoExpirada(expirada = estado.sessaoExpirada, navController = navController)

    LaunchedEffect(oportunidadeId) {
        viewModel.carregar(oportunidadeId)
    }

    // Copia a oportunidade carregada para os campos, uma única vez
    LaunchedEffect(estado.original) {
        val original = estado.original
        if (original != null && !preenchido) {
            tipo = original.tipo
            titulo = original.titulo
            organizacao = original.organizacao.orEmpty()
            area = original.area.orEmpty()
            nivel = original.nivel.orEmpty()
            localidade = original.localidade.orEmpty()
            linkExterno = original.linkExterno.orEmpty()
            prazo = formatarData(original.prazoInscricao)
            descricao = original.descricao.orEmpty()
            requisitos = original.requisitos.orEmpty()
            beneficios = original.beneficios.orEmpty()
            preenchido = true
        }
    }

    // Salvou ou excluiu: volta para a lista
    LaunchedEffect(estado.concluido) {
        if (estado.concluido) navController.popBackStack()
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
            titulo = if (editando) "Editar oportunidade" else "Nova oportunidade",
            onVoltar = { navController.popBackStack() }
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {

            val erro = estado.erro

            when {
                erro != null -> EstadoErro(
                    mensagem = erro,
                    onTentarDeNovo = { viewModel.carregar(oportunidadeId) }
                )

                !preenchido -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EsqueletoBloco(altura = 48.dp)
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

                    Text(
                        text = "Tipo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MentoriaCores.TextoMudo
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TIPOS_DE_OPORTUNIDADE.forEach { opcao ->
                            ChipOpcao(
                                texto = nomeDoTipoDeOportunidade(opcao),
                                selecionado = tipo == opcao,
                                onClick = { tipo = opcao }
                            )
                        }
                    }

                    CampoDeTexto(
                        rotulo = "Título",
                        valor = titulo,
                        aoMudar = { titulo = it },
                        dica = "Ex.: Estágio em Desenvolvimento Mobile"
                    )

                    CampoDeTexto(
                        rotulo = "Organização",
                        valor = organizacao,
                        aoMudar = { organizacao = it },
                        dica = "Ex.: Acme Tecnologia"
                    )

                    CampoDeTexto(
                        rotulo = "Área",
                        valor = area,
                        aoMudar = { area = it },
                        dica = "Ex.: Programação"
                    )

                    CampoDeTexto(
                        rotulo = "Nível",
                        valor = nivel,
                        aoMudar = { nivel = it },
                        dica = "Ex.: Iniciante"
                    )

                    CampoDeTexto(
                        rotulo = "Localidade",
                        valor = localidade,
                        aoMudar = { localidade = it },
                        dica = "Ex.: Remoto ou São Paulo, SP"
                    )

                    CampoDeTexto(
                        rotulo = "Link para se inscrever",
                        valor = linkExterno,
                        aoMudar = { linkExterno = it },
                        dica = "https://...",
                        teclado = KeyboardType.Uri,
                        maiusculaNoInicio = false
                    )

                    CampoDeTexto(
                        rotulo = "Prazo de inscrição (opcional)",
                        valor = prazo,
                        aoMudar = { prazo = mascaraDeData(it) },
                        dica = "dd/mm/aaaa",
                        teclado = KeyboardType.Number,
                        maiusculaNoInicio = false
                    )

                    CampoDeTexto(
                        rotulo = "Descrição",
                        valor = descricao,
                        aoMudar = { descricao = it },
                        dica = "Conte do que se trata",
                        multilinha = true
                    )

                    CampoDeTexto(
                        rotulo = "Requisitos",
                        valor = requisitos,
                        aoMudar = { requisitos = it },
                        dica = "O que a candidata precisa ter",
                        multilinha = true
                    )

                    CampoDeTexto(
                        rotulo = "Benefícios",
                        valor = beneficios,
                        aoMudar = { beneficios = it },
                        dica = "Bolsa, vale-refeição, mentoria...",
                        multilinha = true
                    )

                    if (estado.erroAoSalvar != null) {
                        MensagemDeErro(texto = estado.erroAoSalvar.orEmpty())
                    }

                    GradientButton(
                        text = when {
                            estado.salvando -> "Salvando..."
                            editando -> "Salvar alterações"
                            else -> "Cadastrar oportunidade"
                        },
                        onClick = {
                            foco.clearFocus()
                            viewModel.salvar(
                                oportunidadeId = oportunidadeId,
                                tipo = tipo,
                                titulo = titulo,
                                organizacao = organizacao,
                                area = area,
                                nivel = nivel,
                                localidade = localidade,
                                linkExterno = linkExterno,
                                prazo = prazo,
                                descricao = descricao,
                                requisitos = requisitos,
                                beneficios = beneficios
                            )
                        }
                    )

                    if (editando) {

                        Spacer(modifier = Modifier.height(6.dp))

                        BotaoDePerigo(
                            texto = if (estado.excluindo) "Excluindo..." else "Excluir oportunidade",
                            onClick = { pedindoConfirmacao = true }
                        )
                    }
                }
            }
        }
    }

    if (pedindoConfirmacao && oportunidadeId != null) {

        DialogoDeConfirmacao(
            titulo = "Excluir oportunidade?",
            texto = "Ela deixa de aparecer para as alunas. Essa ação não pode ser desfeita pelo aplicativo.",
            textoDoBotao = "Excluir",
            onConfirmar = {
                pedindoConfirmacao = false
                viewModel.excluir(oportunidadeId)
            },
            onCancelar = { pedindoConfirmacao = false }
        )
    }
}
