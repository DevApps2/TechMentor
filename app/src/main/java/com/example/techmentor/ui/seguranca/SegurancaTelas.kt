package com.example.techmentor.ui.seguranca

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.techmentor.data.ConteudoSegurancaResponse
import com.example.techmentor.data.DenunciaResponse
import com.example.techmentor.ui.home.parseDataHora
import com.example.techmentor.ui.auth.AuthField
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.auth.OutlinedAuthButton
import com.example.techmentor.ui.components.AvisoDemo
import com.example.techmentor.ui.mentoria.ChipOpcao
import com.example.techmentor.ui.mentoria.EsqueletoBloco
import com.example.techmentor.ui.mentoria.MentoriaCores
import java.text.SimpleDateFormat
import java.util.Locale

private const val LIMITE_DESCRICAO = 2000
private const val MINIMO_DESCRICAO = 10


// ======================================================
// DENUNCIAR USUÁRIO
// ======================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DenunciarUsuarioTela(
    navController: NavController,
    estado: SegurancaUiState,
    onBuscar: (String) -> Unit,
    onEnviar: (usuarioId: Long?, motivo: MotivoDenuncia, descricao: String) -> Unit,
    onBloquear: (usuarioId: Long, aoTerminar: () -> Unit) -> Unit,
    onVoltar: () -> Unit
) {

    var busca by rememberSaveable { mutableStateOf("") }
    var escolhidoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var escolhidoNome by rememberSaveable { mutableStateOf<String?>(null) }
    var motivoEscolhido by rememberSaveable { mutableStateOf<String?>(null) }
    var descricao by rememberSaveable { mutableStateOf("") }
    var tentou by rememberSaveable { mutableStateOf(false) }

    if (estado.denunciaEnviada) {

        val denunciadoId = escolhidoId
        val denunciadoNome = escolhidoNome
        val jaBloqueado = denunciadoId != null && estado.bloqueados.any { it.usuarioId == denunciadoId }

        // Só oferece bloquear quando a denúncia foi sobre uma pessoa (denúncia geral não tem a quem bloquear)
        val extra: (@Composable ColumnScope.() -> Unit)? = if (denunciadoId != null && denunciadoNome != null) {
            {
                if (jaBloqueado) {
                    Text(
                        text = "✓ Bloqueio registrado",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                } else {
                    OutlinedAuthButton(
                        text = if (estado.enviando) "Bloqueando..." else "Bloquear $denunciadoNome também",
                        onClick = { if (!estado.enviando) onBloquear(denunciadoId) { } }
                    )
                }

                if (estado.erroAcao != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    MensagemDeErro(estado.erroAcao)
                }
            }
        } else {
            null
        }

        ConfirmacaoDaAcao(
            navController = navController,
            tituloDoCabecalho = "Denunciar usuário",
            titulo = "Denúncia registrada",
            texto = "Avisamos a equipe do TechMentor por e-mail. Você acompanha o andamento em \"Minhas denúncias\".",
            textoDoBotao = "Voltar à segurança",
            onVoltar = onVoltar,
            extra = extra
        )

        return
    }

    val motivo = MotivoDenuncia.entries.firstOrNull { it.name == motivoEscolhido }
    val motivoOk = motivo != null
    val descricaoOk = descricao.trim().length >= MINIMO_DESCRICAO

    TelaInterna(
        navController = navController,
        titulo = "Denunciar usuário",
        onVoltar = onVoltar
    ) {

        TituloDeSecao("Conte o que aconteceu")

        if (escolhidoNome != null) {

            UsuarioEscolhido(
                rotulo = "Quem você quer denunciar? (opcional)",
                nome = escolhidoNome.orEmpty(),
                onTrocar = {
                    escolhidoId = null
                    escolhidoNome = null
                }
            )

        } else {

            BuscaDeUsuario(
                rotulo = "Quem você quer denunciar? (opcional)",
                texto = busca,
                onTexto = {
                    busca = it
                    onBuscar(it)
                },
                resultados = estado.resultadosBusca,
                buscando = estado.buscando,
                onEscolher = {
                    escolhidoId = it.id
                    escolhidoNome = it.nome
                    busca = ""
                    onBuscar("")
                }
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = "Motivo",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF514C58)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MotivoDenuncia.entries.forEach { opcao ->
                    ChipOpcao(
                        texto = opcao.rotulo,
                        selecionado = opcao == motivo,
                        onClick = { motivoEscolhido = opcao.name }
                    )
                }
            }

            if (tentou && !motivoOk) MensagemDeErro("Selecione um motivo.")
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            AuthField(
                label = "Descrição",
                value = descricao,
                onValueChange = { if (it.length <= LIMITE_DESCRICAO) descricao = it },
                placeholder = "Descreva a situação (mínimo de $MINIMO_DESCRICAO caracteres)",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                keyboardActions = KeyboardActions.Default,
                multilinha = true
            )

            if (tentou && !descricaoOk) {
                MensagemDeErro("Descreva a situação com pelo menos $MINIMO_DESCRICAO caracteres.")
            }
        }

        if (estado.erroAcao != null) MensagemDeErro(estado.erroAcao)

        GradientButton(
            text = if (estado.enviando) "Enviando..." else "Enviar denúncia",
            onClick = {
                tentou = true
                if (motivo != null && descricaoOk && !estado.enviando) {
                    onEnviar(escolhidoId, motivo, descricao)
                }
            }
        )
    }
}


// ======================================================
// MINHAS DENÚNCIAS
// ======================================================

@Composable
internal fun MinhasDenunciasTela(
    navController: NavController,
    estado: SegurancaUiState,
    onVoltar: () -> Unit
) {

    val lista = estado.minhasDenuncias

    TelaInterna(
        navController = navController,
        titulo = "Minhas denúncias",
        onVoltar = onVoltar
    ) {

        TituloDeSecao("Acompanhe suas denúncias")

        AvisoDemo(texto = "A situação muda quando a equipe analisa o relato, e você recebe uma notificação.")

        when {

            lista == null -> {
                EsqueletoBloco(altura = 90.dp)
                EsqueletoBloco(altura = 90.dp)
            }

            lista.isEmpty() -> CaixaTracejada(texto = "Você ainda não fez nenhuma denúncia.")

            else -> lista.forEach { denuncia ->
                CartaoDeDenuncia(denuncia = denuncia)
            }
        }
    }
}

private class EstiloDoStatus(val rotulo: String, val fundo: Color, val texto: Color)

private fun estiloDoStatus(status: String): EstiloDoStatus = when (status) {
    "EM_ANALISE" -> EstiloDoStatus("Em análise", Color(0xFFE6EFFF), Color(0xFF2563EB))
    "RESOLVIDA" -> EstiloDoStatus("Resolvida", Color(0xFFDCFCE7), Color(0xFF15803D))
    else -> EstiloDoStatus("Aberta", Color(0xFFFEF3C7), Color(0xFFB45309))
}

/** "2026-11-01T14:30:00" -> "1 nov. 2026, 14:30" */
private fun formatarDataDaDenuncia(criadaEm: String?): String {
    val data = parseDataHora(criadaEm) ?: return ""
    return SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("pt-BR")).format(data)
}

@Composable
private fun CartaoDeDenuncia(denuncia: DenunciaResponse) {

    val forma = RoundedCornerShape(17.dp)
    val estilo = estiloDoStatus(denuncia.status)
    val motivo = MotivoDenuncia.entries.firstOrNull { it.api == denuncia.motivo }?.rotulo ?: denuncia.motivo

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(14.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = motivo,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20202A)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(estilo.fundo)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {

                Text(
                    text = estilo.rotulo,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = estilo.texto
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = if (denuncia.denunciadoNome != null) "Sobre ${denuncia.denunciadoNome}" else "Denúncia geral",
            fontSize = 12.sp,
            color = MentoriaCores.TextoMudo
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = denuncia.descricao,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = Color(0xFF625D6C),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = formatarDataDaDenuncia(denuncia.criadaEm),
            fontSize = 10.sp,
            color = Color(0xFFA19CA5)
        )
    }
}


// ======================================================
// USUÁRIOS BLOQUEADOS
// ======================================================

@Composable
internal fun UsuariosBloqueadosTela(
    navController: NavController,
    estado: SegurancaUiState,
    onBuscar: (String) -> Unit,
    onBloquear: (usuarioId: Long, aoTerminar: () -> Unit) -> Unit,
    onDesbloquear: (usuarioId: Long) -> Unit,
    onVoltar: () -> Unit
) {

    var busca by rememberSaveable { mutableStateOf("") }
    var escolhidoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var escolhidoNome by rememberSaveable { mutableStateOf<String?>(null) }

    // quem já está bloqueado não precisa aparecer de novo nos resultados
    val resultados = estado.resultadosBusca.filter { r -> estado.bloqueados.none { it.usuarioId == r.id } }

    TelaInterna(
        navController = navController,
        titulo = "Usuários bloqueados",
        onVoltar = onVoltar
    ) {

        TituloDeSecao("Usuários bloqueados")

        if (estado.bloqueados.isEmpty()) {

            CaixaTracejada(texto = "Você ainda não bloqueou ninguém.")

        } else {

            estado.bloqueados.forEach { bloqueado ->
                LinhaBloqueado(
                    nome = bloqueado.nome,
                    onDesbloquear = { onDesbloquear(bloqueado.usuarioId) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        TituloDeSecao("Bloquear alguém")

        if (escolhidoNome != null) {

            UsuarioEscolhido(
                rotulo = "Quem você quer bloquear?",
                nome = escolhidoNome.orEmpty(),
                onTrocar = {
                    escolhidoId = null
                    escolhidoNome = null
                }
            )

            GradientButton(
                text = if (estado.enviando) "Bloqueando..." else "Bloquear ${escolhidoNome.orEmpty()}",
                onClick = {
                    val id = escolhidoId
                    if (id != null && !estado.enviando) {
                        onBloquear(id) {
                            escolhidoId = null
                            escolhidoNome = null
                            busca = ""
                        }
                    }
                }
            )

        } else {

            BuscaDeUsuario(
                rotulo = "Quem você quer bloquear?",
                texto = busca,
                onTexto = {
                    busca = it
                    onBuscar(it)
                },
                resultados = resultados,
                buscando = estado.buscando,
                onEscolher = {
                    escolhidoId = it.id
                    escolhidoNome = it.nome
                    busca = ""
                    onBuscar("")
                }
            )
        }

        if (estado.erroAcao != null) MensagemDeErro(estado.erroAcao)
    }
}

@Composable
private fun LinhaBloqueado(nome: String, onDesbloquear: () -> Unit) {

    val forma = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, MentoriaCores.Borda, forma)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = nome,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MentoriaCores.TextoEscuro,
            maxLines = 1
        )

        Text(
            text = "Desbloquear",
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onDesbloquear)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            fontSize = 12.sp,
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
    navController: NavController,
    estado: SegurancaUiState,
    onTentarDeNovo: () -> Unit,
    onVoltar: () -> Unit
) {

    TelaInterna(
        navController = navController,
        titulo = "Orientações",
        onVoltar = onVoltar
    ) {

        TituloDeSecao("Cuide da sua experiência")

        ConteudosOuEstado(
            estado = estado,
            itens = estado.orientacoes,
            mostrarContato = false,
            onTentarDeNovo = onTentarDeNovo
        )

        OutlinedAuthButton(
            text = "Voltar à segurança",
            onClick = onVoltar
        )
    }
}

@Composable
internal fun CanaisDeAjudaTela(
    navController: NavController,
    estado: SegurancaUiState,
    onTentarDeNovo: () -> Unit,
    onVoltar: () -> Unit
) {

    TelaInterna(
        navController = navController,
        titulo = "Canais de ajuda",
        onVoltar = onVoltar
    ) {

        TituloDeSecao("Onde buscar ajuda")

        ConteudosOuEstado(
            estado = estado,
            itens = estado.canaisDeAjuda,
            mostrarContato = true,
            onTentarDeNovo = onTentarDeNovo
        )

        AvisoDemo(texto = "O TechMentor não oferece atendimento de emergência. Em perigo imediato, ligue 190.")
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

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = estado.erro,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MentoriaCores.TextoMudo
                )

                OutlinedAuthButton(
                    text = "Tentar novamente",
                    onClick = onTentarDeNovo
                )
            }
        }

        estado.conteudos == null -> {

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EsqueletoBloco(altura = 90.dp)
                EsqueletoBloco(altura = 90.dp)
                EsqueletoBloco(altura = 90.dp)
            }
        }

        itens.isEmpty() -> {
            CaixaTracejada(texto = "Nenhum conteúdo disponível por enquanto.")
        }

        else -> {

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
