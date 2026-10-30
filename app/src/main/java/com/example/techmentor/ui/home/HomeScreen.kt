package com.example.techmentor.ui.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.techmentor.data.AgendamentoResponse
import com.example.techmentor.data.ApiClient
import com.example.techmentor.data.MatriculaResponse
import com.example.techmentor.data.Sessao
import com.example.techmentor.data.UsuarioResponse
import com.example.techmentor.ui.auth.GradientButton
import com.example.techmentor.ui.components.Icone
import com.example.techmentor.ui.components.IconeLinha
import com.example.techmentor.ui.theme.ManropeFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Cores do protótipo
private val Fundo = Color(0xFFF8F8FB)
private val Roxo = Color(0xFF7C3AED)
private val RoxoEscuro = Color(0xFF6D28D9)
private val Azul = Color(0xFF2563EB)
private val TextoEscuro = Color(0xFF17151F)
private val LocaleBr: Locale = Locale.forLanguageTag("pt-BR")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()

    // Volta ao login se a sessão não existe mais (ex.: o banco foi reiniciado)
    LaunchedEffect(estado.sessaoExpirada) {
        if (estado.sessaoExpirada) {
            Sessao.sair()
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    // Carrega ao abrir e ao voltar para a Home; fora isso, só puxando a tela para baixo
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fundo)
            .statusBarsPadding()
    ) {

        Box(
            modifier = Modifier.weight(1f)
        ) {

            val usuario = estado.usuario

            when {
                usuario != null -> PullToRefreshBox(
                    isRefreshing = estado.atualizando,
                    onRefresh = { viewModel.carregar(manual = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    HomeContent(
                        usuario = usuario,
                        cursoEmAndamento = estado.cursoEmAndamento,
                        proximaMentoria = estado.proximaMentoria,
                        navController = navController
                    )
                }

                estado.carregando || estado.sessaoExpirada -> HomeCarregando()

                else -> HomeErro(
                    mensagem = estado.erro ?: "Não foi possível carregar a Home.",
                    onTentarDeNovo = { viewModel.carregar() }
                )
            }
        }

        BottomNavigationBar(
            navController = navController
        )
    }
}


// ======================================================
// CONTEÚDO DA HOME
// ======================================================

@Composable
private fun HomeContent(
    usuario: UsuarioResponse,
    cursoEmAndamento: MatriculaResponse?,
    proximaMentoria: AgendamentoResponse?,
    navController: NavController
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = 24.dp
            )
    ) {

        WelcomeSection(
            nome = usuario.nome,
            fotoUrl = usuario.fotoUrl,
            onPerfil = { irPara(navController, "profile") }
        )

        Spacer(modifier = Modifier.height(19.dp))

        JourneyCard(usuario = usuario)

        SectionHeader(
            title = if (proximaMentoria != null) "Sua próxima mentoria" else "Próximo passo",
            action = "Ver mentoras",
            onAction = { navController.navigate("mentorship") }
        )

        NextActionCard(
            titulo = proximaMentoria?.let { "Mentoria com ${it.mentoraNome}" }
                ?: "Converse com uma mentora",
            descricao = proximaMentoria?.let { descricaoDaMentoria(it) }
                ?: "Explore horários e agende uma conversa.",
            onClick = { navController.navigate("mentorship") }
        )

        SectionHeader(
            title = "Continue aprendendo",
            action = "Ver tudo",
            onAction = { navController.navigate("learning") }
        )

        ContinueLearningCard(
            curso = cursoEmAndamento,
            onClick = { navController.navigate("learning") }
        )

        SectionHeader(
            title = "Explore o TechMentor",
            action = null,
            onAction = {}
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            ExploreCard(
                modifier = Modifier.weight(1f),
                title = "Mentoria",
                description = "Converse e evolua",
                icone = Icone.USUARIOS,
                background = Color(0xFFEDE9FE),
                iconColor = RoxoEscuro,
                onClick = { navController.navigate("mentorship") }
            )

            ExploreCard(
                modifier = Modifier.weight(1f),
                title = "Aprender",
                description = "Novas habilidades",
                icone = Icone.LIVRO,
                background = Color(0xFFE6EFFF),
                iconColor = Azul,
                onClick = { navController.navigate("learning") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            ExploreCard(
                modifier = Modifier.weight(1f),
                title = "Oportunidades",
                description = "Dê o próximo passo",
                icone = Icone.MALETA,
                background = Color(0xFFFEF3C7),
                iconColor = Color(0xFFD97706),
                onClick = { navController.navigate("opportunities") }
            )

            ExploreCard(
                modifier = Modifier.weight(1f),
                title = "Segurança",
                description = "Apoio e proteção",
                icone = Icone.ESCUDO,
                background = Color(0xFFFCE7F3),
                iconColor = Color(0xFFDB2777),
                onClick = { navController.navigate("safety") }
            )
        }
    }
}


// ======================================================
// CARREGANDO E ERRO
// ======================================================

@Composable
private fun HomeCarregando() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        EsqueletoBloco(altura = 56.dp)
        EsqueletoBloco(altura = 156.dp)
        EsqueletoBloco(altura = 74.dp)
        EsqueletoBloco(altura = 89.dp)
    }
}

@Composable
private fun EsqueletoBloco(altura: Dp) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(altura)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFECEAF2))
    )
}

@Composable
private fun HomeErro(
    mensagem: String,
    onTentarDeNovo: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Ops!",
            fontFamily = ManropeFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF20202A)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = mensagem,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF797486)
        )

        Spacer(modifier = Modifier.height(24.dp))

        GradientButton(
            text = "Tentar novamente",
            onClick = onTentarDeNovo
        )
    }
}


// ======================================================
// CABEÇALHO (data, nome e foto)
// ======================================================

@Composable
fun WelcomeSection(
    nome: String,
    fotoUrl: String?,
    onPerfil: () -> Unit
) {

    val date = SimpleDateFormat("EEEE, d 'DE' MMMM", LocaleBr)
        .format(Date())
        .uppercase()

    val primeiroNome = nome.trim().split(" ").firstOrNull().orEmpty().ifEmpty { "você" }
    val inicial = nome.trim().firstOrNull()?.uppercase() ?: "?"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = date,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8C8796)
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Olá, $primeiroNome!",
                fontFamily = ManropeFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
                color = TextoEscuro,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Pronta para dar mais um passo?",
                fontSize = 13.sp,
                color = Color(0xFF797486)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // AVATAR: a inicial fica por baixo e a foto (se existir) cobre quando carregar; toque abre o Perfil
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Color(0xFFEDE9FE))
                .clickable { onPerfil() },
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = inicial,
                fontFamily = ManropeFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = RoxoEscuro
            )

            if (!fotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ApiClient.urlDaFoto(fotoUrl),
                    contentDescription = "Foto de perfil de $primeiroNome",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


// ======================================================
// SUA JORNADA
// ======================================================

@Composable
fun JourneyCard(
    usuario: UsuarioResponse
) {

    val forma = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(
                Brush.linearGradient(
                    0f to Color(0xFF6D28D9),
                    0.56f to Color(0xFF4F46E5),
                    1f to Color(0xFF2563EB)
                )
            )
    ) {

        // anel decorativo no canto superior direito
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 55.dp, y = (-70).dp)
                .size(150.dp)
                .border(28.dp, Color.White.copy(alpha = 0.07f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        text = "SUA JORNADA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.4.sp,
                        color = Color.White.copy(alpha = 0.72f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${formatarNumero(usuario.xpTotal)} XP acumulados",
                        fontFamily = ManropeFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(15.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {

                    Text(
                        text = "Nível ${usuario.nivel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Próximo nível",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "${formatarNumero(usuario.xpProximoNivel)} XP",
                    fontSize = 12.sp,
                    color = Color(0xFFD9D5FA)
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // BARRA DE PROGRESSO DO NÍVEL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(alpha = 0.19f))
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth(fracaoDoNivel(usuario))
                        .height(6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.White, Color(0xFFBFDBFE))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = buildAnnotatedString {
                    append("Faltam ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("${formatarNumero(usuario.xpFaltante)} XP")
                    }
                    append(" · ${textoSequencia(usuario.sequenciaDias)}")
                },
                fontSize = 11.sp,
                color = Color(0xFFDCD8FF)
            )
        }
    }
}


// ======================================================
// TÍTULO DE SEÇÃO
// ======================================================

@Composable
fun SectionHeader(
    title: String,
    action: String?,
    onAction: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            fontFamily = ManropeFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            color = TextoEscuro
        )

        if (action != null) {
            Text(
                text = action,
                modifier = Modifier.clickable { onAction() },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Roxo
            )
        }
    }
}


// ======================================================
// PRÓXIMO PASSO
// ======================================================

@Composable
fun NextActionCard(
    titulo: String,
    descricao: String,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconTile(
                icone = Icone.USUARIOS,
                background = Color(0xFFEDE9FE),
                iconColor = RoxoEscuro
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF20202A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = descricao,
                    fontSize = 12.sp,
                    color = Color(0xFF625D6C)
                )
            }

            Text(
                text = "›",
                fontSize = 26.sp,
                color = Color(0xFF797486)
            )
        }
    }
}


// ======================================================
// CONTINUE APRENDENDO
// ======================================================

@Composable
fun ContinueLearningCard(
    curso: MatriculaResponse?,
    onClick: () -> Unit
) {

    val categoria = if (curso != null) rotuloCategoria(curso.categoria) else "COMECE AGORA"
    val titulo = curso?.tituloCurso ?: "Escolha sua primeira trilha"
    val detalhe = if (curso != null) {
        "${curso.aulasConcluidas} de ${curso.totalAulas} aulas"
    } else {
        "Aulas curtas e XP a cada passo"
    }
    val fracao: Float? = if (curso != null && curso.totalAulas > 0) {
        curso.aulasConcluidas.toFloat() / curso.totalAulas
    } else {
        null
    }

    val forma = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White)
            .border(1.dp, Color(0xFFECE9F1), forma)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(width = 58.dp, height = 63.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFEDE9FE), Color(0xFFDBEAFE))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {

            IconeLinha(
                icone = Icone.CODIGO,
                cor = RoxoEscuro,
                tamanho = 27.dp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = categoria,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.7.sp,
                color = Roxo
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = titulo,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextoEscuro,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = detalhe,
                fontSize = 9.sp,
                color = Color(0xFF85808C)
            )

            if (fracao != null) {

                Spacer(modifier = Modifier.height(7.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFE9E6EF))
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fracao)
                            .height(6.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Roxo, Azul))
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Roxo)
                .padding(start = 2.dp),
            contentAlignment = Alignment.Center
        ) {

            IconeLinha(
                icone = Icone.PLAY,
                cor = Color.White,
                tamanho = 17.dp
            )
        }
    }
}


// ======================================================
// EXPLORE O TECHMENTOR
// ======================================================

@Composable
fun ExploreCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icone: Icone,
    background: Color,
    iconColor: Color,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(17.dp)

    Row(
        modifier = modifier
            .heightIn(min = 78.dp)
            .clip(forma)
            .background(Color.White)
            .border(1.dp, Color(0xFFEEECF2), forma)
            .clickable { onClick() }
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconTile(
            icone = icone,
            background = background,
            iconColor = iconColor
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextoEscuro,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = Color(0xFF8A8691),
                maxLines = 2
            )
        }

        IconeLinha(
            icone = Icone.SETA,
            cor = TextoEscuro,
            tamanho = 17.dp
        )
    }
}

@Composable
fun IconTile(
    icone: Icone,
    background: Color,
    iconColor: Color
) {

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {

        IconeLinha(
            icone = icone,
            cor = iconColor,
            tamanho = 22.dp
        )
    }
}


// ======================================================
// BARRA INFERIOR
// ======================================================

@Composable
fun BottomNavigationBar(
    navController: NavController,
    rotaAtual: String = "home"
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {

        // linha fina no topo da barra
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE9E7EE))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(76.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {

            BottomNavItem(
                icone = Icone.CASA,
                label = "Início",
                selected = rotaAtual == "home",
                onClick = { irPara(navController, "home") }
            )

            BottomNavItem(
                icone = Icone.BUSCA,
                label = "Explorar",
                selected = rotaAtual in listOf("mentorship", "opportunities", "safety"),
                onClick = { irPara(navController, "mentorship") }
            )

            BottomNavItem(
                icone = Icone.LIVRO,
                label = "Aprender",
                selected = rotaAtual == "learning",
                onClick = { irPara(navController, "learning") }
            )

            BottomNavItem(
                icone = Icone.TROFEU,
                label = "Conquistas",
                selected = rotaAtual == "achievements",
                onClick = { irPara(navController, "achievements") }
            )

            BottomNavItem(
                icone = Icone.PERFIL,
                label = "Perfil",
                selected = rotaAtual == "profile",
                onClick = { irPara(navController, "profile") }
            )
        }
    }
}

private fun irPara(navController: NavController, rota: String) {
    navController.navigate(rota) {
        popUpTo("home") { inclusive = false }
        launchSingleTop = true
    }
}

@Composable
fun BottomNavItem(
    icone: Icone,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    val cor = if (selected) Roxo else Color(0xFF96919E)

    Box(
        modifier = Modifier
            .size(width = 70.dp, height = 62.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.TopCenter
    ) {

        // marca roxa no topo do item ativo
        if (selected) {
            Box(
                modifier = Modifier
                    .width(27.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(Roxo)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            IconeLinha(
                icone = icone,
                cor = cor,
                tamanho = 21.dp,
                espessura = if (selected) 2.4f else 1.8f
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = cor
            )
        }
    }
}
