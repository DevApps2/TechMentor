package com.example.techmentor.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.techmentor.data.AdminConviteParceiraRequest
import com.example.techmentor.data.AdminParceiraResponse
import com.example.techmentor.data.AdminVerificacaoParceiraRequest
import com.example.techmentor.ui.auth.AuthColors
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha
import com.example.techmentor.ui.mentoria.CabecalhoVoltar

private val AdminFundo = Color(0xFFF8F8FB)
private val AdminTinta = Color(0xFF20202A)
private val AdminCinza = Color(0xFF77717E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminParceirasScreen(
    navController: NavController,
    viewModel: AdminParceirasViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    var convidar by remember { mutableStateOf(false) }
    var selecionada by remember { mutableStateOf<AdminParceiraResponse?>(null) }
    var reenviar by remember { mutableStateOf<AdminParceiraResponse?>(null) }
    var suspender by remember { mutableStateOf<AdminParceiraResponse?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.carregar() }

    LaunchedEffect(estado.sucesso) {
        if (estado.sucesso != null) convidar = false
    }

    selecionada?.let { item ->
        val atual = estado.parceiras.firstOrNull { it.id == item.id } ?: item
        DialogoDetalheParceira(
            parceira = atual,
            enviando = estado.enviando,
            erro = estado.erroAcao,
            carregandoHistorico = estado.carregandoHistorico,
            historico = estado.historico,
            aoVerificar = { checklist -> viewModel.verificar(atual.id, checklist) },
            aoReenviar = { reenviar = atual },
            aoSuspender = { suspender = atual },
            aoReativar = { viewModel.reativar(atual.id) },
            aoCarregarHistorico = { viewModel.carregarHistorico(atual.id) },
            aoFechar = { selecionada = null; viewModel.limparMensagens() }
        )
    }

    if (convidar) {
        DialogoConvidarParceira(
            enviando = estado.enviando,
            erro = estado.erroAcao,
            aoConvidar = viewModel::convidar,
            aoFechar = { convidar = false; viewModel.limparMensagens() }
        )
    }

    reenviar?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!estado.enviando) reenviar = null },
            title = { Text("Enviar um novo convite?") },
            text = { Text("O código anterior de ${item.nome} deixará de funcionar. Um novo código será enviado para ${item.email}.") },
            confirmButton = {
                TextButton(enabled = !estado.enviando, onClick = { viewModel.reenviarConvite(item.id); reenviar = null }) { Text("Reenviar convite", color = AuthColors.Purple) }
            },
            dismissButton = { TextButton(onClick = { reenviar = null }) { Text("Cancelar", color = AuthColors.Purple) } },
            containerColor = Color.White,
            titleContentColor = AdminTinta,
            textContentColor = AdminTinta,
            tonalElevation = 0.dp
        )
    }

    suspender?.let { item ->
        DialogoSuspenderParceira(
            nome = item.nome,
            enviando = estado.enviando,
            erro = estado.erroAcao,
            aoSuspender = { motivo -> viewModel.suspender(item.id, motivo); suspender = null },
            aoFechar = { suspender = null; viewModel.limparMensagens() }
        )
    }

    Column(Modifier.fillMaxSize().background(AdminFundo)) {
        CabecalhoVoltar(titulo = "Parceiras", onVoltar = { navController.popBackStack() })
        Box(Modifier.weight(1f)) {
            when {
                estado.carregando && estado.parceiras.isEmpty() -> AdminParceirasCarregando()
                estado.erro != null && estado.parceiras.isEmpty() -> AdminParceirasErro(estado.erro!!) { viewModel.carregar() }
                else -> PullToRefreshBox(
                    isRefreshing = estado.atualizando,
                    onRefresh = { viewModel.carregar(estado.filtro) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text("Convide e acompanhe instituições parceiras.", color = AdminCinza, fontSize = 13.sp)
                        Spacer(Modifier.height(14.dp))
                        GradientButton(text = "Convidar uma parceira", onClick = { convidar = true })
                        estado.sucesso?.let {
                            Spacer(Modifier.height(10.dp))
                            Text(it, Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFE8F7EF)).padding(12.dp), color = Color(0xFF23865D), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        estado.erro?.let {
                            Spacer(Modifier.height(10.dp))
                            Text(it, color = Color(0xFFB42336), fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(15.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf(null to "Todas", "CONVIDADA" to "Convidadas", "ATIVA" to "Ativas", "SUSPENSA" to "Suspensas").forEach { (filtro, rotulo) ->
                                FilterChip(
                                    selected = estado.filtro == filtro,
                                    onClick = { viewModel.carregar(filtro) },
                                    label = { Text(rotulo, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.White,
                                        labelColor = AdminTinta,
                                        selectedContainerColor = AuthColors.Purple,
                                        selectedLabelColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, if (estado.filtro == filtro) AuthColors.Purple else Color(0xFFE1DDE7))
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        if (estado.parceiras.isEmpty()) {
                            Text("Nenhuma parceira nesta situação.", color = AdminCinza, fontSize = 13.sp, modifier = Modifier.padding(vertical = 24.dp))
                        } else {
                            estado.parceiras.forEach { parceira ->
                                CartaoAdminParceira(parceira) { selecionada = parceira }
                                Spacer(Modifier.height(9.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartaoAdminParceira(parceira: AdminParceiraResponse, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Color.White)
            .clickable(role = Role.Button, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconeLinha(Icone.PREDIO, AuthColors.Purple, tamanho = 28.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(parceira.nome, color = AdminTinta, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${parceira.responsavel} · ${parceira.email}", color = AdminCinza, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (parceira.aceitouOsTermos) "Convite aceito" else "Aguardando aceite", color = AdminCinza, fontSize = 10.sp)
        }
        SeloSituacaoParceira(parceira.situacao)
    }
}

@Composable
private fun SeloSituacaoParceira(situacao: String) {
    val (texto, cor, fundo) = when (situacao) {
        "ATIVA" -> Triple("Ativa", Color(0xFF23865D), Color(0xFFE2F5EC))
        "SUSPENSA" -> Triple("Suspensa", Color(0xFFB42336), Color(0xFFFFE9EB))
        else -> Triple("Convidada", Color(0xFF9A620C), Color(0xFFFFF2D9))
    }
    Text(texto, color = cor, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(fundo).padding(horizontal = 8.dp, vertical = 5.dp))
}

@Composable
private fun DialogoConvidarParceira(
    enviando: Boolean,
    erro: String?,
    aoConvidar: (AdminConviteParceiraRequest) -> Unit,
    aoFechar: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var responsavel by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var documento by remember { mutableStateOf("") }
    var site by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("EMPRESA") }
    var menuTipo by remember { mutableStateOf(false) }
    var erroLocal by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!enviando) aoFechar() },
        title = { Text("Convidar parceira", color = AdminTinta, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nome, { nome = it.take(120) }, label = { Text("Nome da instituição") }, singleLine = true, colors = coresCampoParceira(), modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedButton(
                        onClick = { menuTipo = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AuthColors.Purple),
                        border = BorderStroke(1.dp, Color(0xFFB8B2BD))
                    ) { Text("Tipo: ${tipoParceiraLegivel(tipo)}") }
                    DropdownMenu(expanded = menuTipo, onDismissRequest = { menuTipo = false }, containerColor = Color.White) {
                        listOf("EMPRESA", "ONG", "INDEPENDENTE").forEach { valor ->
                            DropdownMenuItem(text = { Text(tipoParceiraLegivel(valor), color = AdminTinta) }, onClick = { tipo = valor; menuTipo = false })
                        }
                    }
                }
                if (tipo == "EMPRESA") OutlinedTextField(documento, { documento = it.take(18) }, label = { Text("CNPJ") }, singleLine = true, colors = coresCampoParceira(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(site, { site = it.take(200) }, label = { Text("Site (opcional)") }, singleLine = true, colors = coresCampoParceira(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(responsavel, { responsavel = it.take(120) }, label = { Text("Nome de quem responde") }, singleLine = true, colors = coresCampoParceira(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it.take(160) }, label = { Text("E-mail que receberá o convite") }, singleLine = true, colors = coresCampoParceira(), modifier = Modifier.fillMaxWidth())
                Text("A pessoa responsável criará a própria senha pelo link de convite. O código expira em 7 dias.", color = AdminCinza, fontSize = 11.sp)
                (erroLocal ?: erro)?.let { Text(it, color = Color(0xFFB42336), fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(enabled = !enviando, onClick = {
                val emailLimpo = email.trim().lowercase()
                erroLocal = when {
                    nome.isBlank() -> "Informe o nome da instituição."
                    responsavel.isBlank() -> "Informe a pessoa responsável."
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(emailLimpo).matches() -> "Informe um e-mail válido."
                    tipo == "EMPRESA" && documento.isBlank() -> "Informe o CNPJ da empresa."
                    site.isNotBlank() && !site.trim().startsWith("http://") && !site.trim().startsWith("https://") -> "O site deve começar com http:// ou https://."
                    else -> null
                }
                if (erroLocal == null) aoConvidar(AdminConviteParceiraRequest(nome.trim(), tipo, documento.trim().ifBlank { null }, site.trim().ifBlank { null }, responsavel.trim(), emailLimpo))
            }) { Text(if (enviando) "Enviando…" else "Enviar convite", color = AuthColors.Purple) }
        },
        dismissButton = { TextButton(enabled = !enviando, onClick = aoFechar) { Text("Cancelar", color = AuthColors.Purple) } },
        containerColor = Color.White,
        titleContentColor = AdminTinta,
        textContentColor = AdminTinta,
        tonalElevation = 0.dp
    )
}

@Composable
private fun DialogoDetalheParceira(
    parceira: AdminParceiraResponse,
    enviando: Boolean,
    erro: String?,
    carregandoHistorico: Boolean,
    historico: List<com.example.techmentor.data.AdminHistoricoParceiraResponse>,
    aoVerificar: (AdminVerificacaoParceiraRequest) -> Unit,
    aoReenviar: () -> Unit,
    aoSuspender: () -> Unit,
    aoReativar: () -> Unit,
    aoCarregarHistorico: () -> Unit,
    aoFechar: () -> Unit
) {
    var siteConferido by remember(parceira.id) { mutableStateOf(parceira.verificacao?.siteConferido ?: false) }
    var dominioConferido by remember(parceira.id) { mutableStateOf(parceira.verificacao?.dominioDoEmailConferido ?: false) }
    var identificacaoConferida by remember(parceira.id) { mutableStateOf(parceira.verificacao?.identificacaoConferida ?: false) }
    var observacao by remember(parceira.id) { mutableStateOf(parceira.verificacao?.observacao.orEmpty()) }
    val podeAtivar = siteConferido && dominioConferido && identificacaoConferida && parceira.aceitouOsTermos

    LaunchedEffect(parceira.id) { aoCarregarHistorico() }

    AlertDialog(
        onDismissRequest = { if (!enviando) aoFechar() },
        title = { Text(parceira.nome, color = AdminTinta, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SeloSituacaoParceira(parceira.situacao)
                TextoDetalhe("Responsável", parceira.responsavel)
                TextoDetalhe("E-mail", parceira.email)
                TextoDetalhe("Tipo", tipoParceiraLegivel(parceira.tipo))
                TextoDetalhe("Documento", parceira.documento ?: "Não informado")
                TextoDetalhe("Site", parceira.site ?: "Não informado")
                TextoDetalhe("Termos", if (parceira.aceitouOsTermos) "Aceitos${parceira.termosVersao?.let { " · versão $it" } ?: ""}" else "Aguardando aceite")
                TextoDetalhe("Domínio do e-mail", when (parceira.dominioDoEmailConfereComSite) {
                    true -> "Confere com o site"
                    false -> "Não confere ou é e-mail pessoal"
                    null -> "Sem site informado"
                })

                if (parceira.situacao == "CONVIDADA" && !parceira.aceitouOsTermos) {
                    OutlinedButton(
                        onClick = aoReenviar,
                        enabled = !enviando,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AuthColors.Purple),
                        border = BorderStroke(1.dp, Color(0xFFB8B2BD))
                    ) { Text("Reenviar convite") }
                }
                if (parceira.situacao == "CONVIDADA" && parceira.aceitouOsTermos) {
                    Text("Conferência da instituição", color = AdminTinta, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    ItemChecklist("Site conferido", siteConferido) { siteConferido = it }
                    ItemChecklist("Domínio do e-mail conferido", dominioConferido) { dominioConferido = it }
                    ItemChecklist("Identificação conferida", identificacaoConferida) { identificacaoConferida = it }
                    OutlinedTextField(observacao, { observacao = it.take(500) }, label = { Text("Observação (opcional)") }, colors = coresCampoParceira(), modifier = Modifier.fillMaxWidth())
                    TextButton(enabled = !enviando && podeAtivar, onClick = {
                        aoVerificar(AdminVerificacaoParceiraRequest(true, true, true, observacao.trim().ifBlank { null }))
                    }) {
                        Text(
                            if (enviando) "Ativando…" else "Concluir verificação e ativar",
                            color = if (podeAtivar && !enviando) AuthColors.Purple else AdminCinza
                        )
                    }
                }
                if (parceira.situacao == "ATIVA") TextButton(enabled = !enviando, onClick = aoSuspender) { Text("Suspender parceria", color = Color(0xFFB42336)) }
                if (parceira.situacao == "SUSPENSA") TextButton(enabled = !enviando, onClick = aoReativar) { Text("Reativar parceria", color = AuthColors.Purple) }

                Text("Histórico", color = AdminTinta, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                when {
                    carregandoHistorico -> Text("Carregando histórico…", color = AdminCinza, fontSize = 11.sp)
                    historico.isEmpty() -> Text("Sem registros de histórico.", color = AdminCinza, fontSize = 11.sp)
                    else -> historico.take(6).forEach { registro ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text(registro.descricao, color = AdminTinta, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("${registro.autorNome ?: "Sistema"} · ${registro.quando.orEmpty().replace('T', ' ')}", color = AdminCinza, fontSize = 9.sp)
                            registro.detalhe?.takeIf { it.isNotBlank() }?.let { Text(it, color = AdminCinza, fontSize = 10.sp) }
                        }
                    }
                }
                erro?.let { Text(it, color = Color(0xFFB42336), fontSize = 11.sp) }
            }
        },
        confirmButton = { TextButton(onClick = aoFechar) { Text("Fechar", color = AuthColors.Purple) } },
        containerColor = Color.White,
        titleContentColor = AdminTinta,
        textContentColor = AdminTinta,
        tonalElevation = 0.dp
    )
}

@Composable
private fun ItemChecklist(rotulo: String, marcado: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = marcado,
            onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(
                checkedColor = AuthColors.Purple,
                uncheckedColor = AdminCinza,
                checkmarkColor = Color.White
            )
        )
        Text(rotulo, color = AdminTinta, fontSize = 12.sp)
    }
}

@Composable
private fun TextoDetalhe(rotulo: String, valor: String) {
    Column(Modifier.padding(vertical = 2.dp)) {
        Text(rotulo, color = AdminCinza, fontSize = 10.sp)
        Text(valor, color = AdminTinta, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DialogoSuspenderParceira(
    nome: String,
    enviando: Boolean,
    erro: String?,
    aoSuspender: (String) -> Unit,
    aoFechar: () -> Unit
) {
    var motivo by remember(nome) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!enviando) aoFechar() },
        title = { Text("Suspender $nome?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A parceira poderá consultar os dados, mas não editar cursos. O conteúdo publicado continua no ar.", color = AdminCinza, fontSize = 12.sp)
                OutlinedTextField(motivo, { motivo = it.take(500) }, label = { Text("Motivo para a parceira") }, minLines = 3, colors = coresCampoParceira(), modifier = Modifier.fillMaxWidth())
                erro?.let { Text(it, color = Color(0xFFB42336), fontSize = 11.sp) }
            }
        },
        confirmButton = { TextButton(enabled = !enviando && motivo.isNotBlank(), onClick = { aoSuspender(motivo.trim()) }) { Text(if (enviando) "Salvando…" else "Suspender", color = Color(0xFFB42336)) } },
        dismissButton = { TextButton(enabled = !enviando, onClick = aoFechar) { Text("Cancelar", color = AuthColors.Purple) } },
        containerColor = Color.White,
        titleContentColor = AdminTinta,
        textContentColor = AdminTinta,
        tonalElevation = 0.dp
    )
}

@Composable
private fun coresCampoParceira() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AdminTinta,
    unfocusedTextColor = AdminTinta,
    disabledTextColor = AdminCinza,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color(0xFFF5F3F7),
    cursorColor = AuthColors.Purple,
    focusedBorderColor = AuthColors.Purple,
    unfocusedBorderColor = Color(0xFFB8B2BD),
    disabledBorderColor = Color(0xFFE9E7EE),
    focusedLabelColor = AuthColors.Purple,
    unfocusedLabelColor = AdminCinza,
    disabledLabelColor = AdminCinza
)

@Composable
private fun AdminParceirasCarregando() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Carregando parceiras…", color = AdminCinza, fontSize = 13.sp) }
}

@Composable
private fun AdminParceirasErro(mensagem: String, aoTentar: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(mensagem, color = AdminCinza, fontSize = 13.sp)
        TextButton(onClick = aoTentar) { Text("Tentar novamente") }
    }
}

private fun tipoParceiraLegivel(tipo: String): String = when (tipo) {
    "EMPRESA" -> "Empresa"
    "ONG" -> "ONG"
    "INDEPENDENTE" -> "Independente"
    else -> tipo
}
