package com.example.techmentor.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.remember
import com.example.techmentor.data.Sessao
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.techmentor.data.AgendamentoResponse
import com.example.techmentor.data.MatriculaResponse
import com.example.techmentor.data.UsuarioResponse
import com.example.techmentor.ui.auth.GradientButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {

    val estado by viewModel.estado.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Volta ao login se a sessão não existe mais (ex.: o banco foi reiniciado)
    LaunchedEffect(estado.sessaoExpirada) {
        if (estado.sessaoExpirada) {
            Sessao.sair()
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    // Carrega ao abrir e recarrega ao voltar para a Home (ex.: depois de concluir uma aula)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregar()
    }

    // Enquanto a Home está na tela, atualiza sozinha a cada 30 segundos
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(30_000)
                viewModel.carregar()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8FB))
            .statusBarsPadding()
    ) {

        Box(
            modifier = Modifier.weight(1f)
        ) {

            val usuario = estado.usuario

            when {
                usuario != null -> HomeContent(
                    usuario = usuario,
                    cursoEmAndamento = estado.cursoEmAndamento,
                    proximaMentoria = estado.proximaMentoria,
                    navController = navController,
                    onSair = {
                        Sessao.sair()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )

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
    navController: NavController,
    onSair: () -> Unit
) {

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 18.dp,
                bottom = 24.dp
            ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        WelcomeSection(
            nome = usuario.nome,
            onSair = onSair
        )

        JourneyCard(usuario = usuario)

        SectionHeader(
            title = if (proximaMentoria != null) "Sua próxima mentoria" else "Próximo passo",
            action = "Ver mentoras",
            onAction = {
                navController.navigate("mentorship")
            }
        )

        NextActionCard(
            titulo = proximaMentoria?.let { "Mentoria com ${it.mentoraNome}" }
                ?: "Converse com uma mentora",
            descricao = proximaMentoria?.let { descricaoDaMentoria(it) }
                ?: "Explore horários e agende uma conversa.",
            onClick = {
                navController.navigate("mentorship")
            }
        )

        SectionHeader(
            title = "Continue aprendendo",
            action = "Ver tudo",
            onAction = {
                navController.navigate("learning")
            }
        )

        ContinueLearningCard(
            curso = cursoEmAndamento,
            onClick = {
                navController.navigate("learning")
            }
        )

        Text(
            text = "Explore o TechMentor",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF20202A)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ExploreCard(
                modifier = Modifier.weight(1f),
                title = "Mentoria",
                description = "Converse e evolua",
                symbol = "♧",
                background = Color(0xFFEDE9FE),
                iconColor = Color(0xFF6D28D9),
                onClick = { navController.navigate("mentorship") }
            )

            ExploreCard(
                modifier = Modifier.weight(1f),
                title = "Aprender",
                description = "Novas habilidades",
                symbol = "▣",
                background = Color(0xFFE6EFFF),
                iconColor = Color(0xFF2563EB),
                onClick = { navController.navigate("learning") }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ExploreCard(
                modifier = Modifier.weight(1f),
                title = "Oportunidades",
                description = "Dê o próximo passo",
                symbol = "▤",
                background = Color(0xFFFEF3C7),
                iconColor = Color(0xFFD97706),
                onClick = { navController.navigate("opportunities") }
            )

            ExploreCard(
                modifier = Modifier.weight(1f),
                title = "Segurança",
                description = "Apoio e proteção",
                symbol = "◆",
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
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        EsqueletoBloco(altura = 56.dp)
        EsqueletoBloco(altura = 150.dp)
        EsqueletoBloco(altura = 78.dp)
        EsqueletoBloco(altura = 90.dp)
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
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
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
// CABEÇALHO
// ======================================================

@Composable
fun WelcomeSection(
    nome: String,
    onSair: () -> Unit
) {

    val date = SimpleDateFormat(
        "EEEE, d 'DE' MMMM",
        Locale.forLanguageTag("pt-BR")
    )
        .format(Date())
        .uppercase()

    val primeiroNome = nome.trim().split(" ").firstOrNull().orEmpty().ifEmpty { "você" }
    val inicial = nome.trim().firstOrNull()?.uppercase() ?: "?"

    var menuAberto by remember { mutableStateOf(false) }

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
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8C8796)
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Olá, $primeiroNome!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF17151F)
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Pronta para dar mais um passo?",
                fontSize = 14.sp,
                color = Color(0xFF797486)
            )
        }

        // AVATAR (toque para abrir o menu)
        Box {

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color(0xFFEDE9FE))
                    .clickable { menuAberto = true },
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = inicial,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6D28D9)
                )
            }

            DropdownMenu(
                expanded = menuAberto,
                onDismissRequest = { menuAberto = false }
            ) {

                DropdownMenuItem(
                    text = { Text("Sair") },
                    onClick = {
                        menuAberto = false
                        onSair()
                    }
                )
            }
        }
    }
}


// ======================================================
// CARD DA JORNADA
// ======================================================

@Composable
fun JourneyCard(
    usuario: UsuarioResponse
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6D28D9),
                            Color(0xFF4F46E5),
                            Color(0xFF2563EB)
                        )
                    )
                )
                .padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {

                Column {

                    Text(
                        text = "SUA JORNADA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDCD8FF)
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "${formatarNumero(usuario.xpTotal)} XP acumulados",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.16f),
                            RoundedCornerShape(15.dp)
                        )
                        .padding(
                            horizontal = 11.dp,
                            vertical = 7.dp
                        )
                ) {

                    Text(
                        text = "Nível ${usuario.nivel}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Próximo nível",
                    fontSize = 12.sp,
                    color = Color.White
                )

                Text(
                    text = "${formatarNumero(usuario.xpProximoNivel)} XP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD9D5FA)
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // BARRA DE PROGRESSO DO NÍVEL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(alpha = 0.20f))
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth(fracaoDoNivel(usuario))
                        .height(7.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White)
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = "Faltam ${formatarNumero(usuario.xpFaltante)} XP · ${textoSequencia(usuario.sequenciaDias)}",
                fontSize = 12.sp,
                color = Color(0xFFDCD8FF)
            )
        }
    }
}


// ======================================================
// CABEÇALHO DE SEÇÃO
// ======================================================

@Composable
fun SectionHeader(
    title: String,
    action: String,
    onAction: () -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF20202A)
        )

        Text(
            text = action,
            modifier = Modifier.clickable {
                onAction()
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF7C3AED)
        )
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
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconTile(
                symbol = "♧",
                background = Color(0xFFEDE9FE),
                iconColor = Color(0xFF6D28D9)
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFEDE9FE),
                                Color(0xFFDBEAFE)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "</>",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6D28D9)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = categoria,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF20202A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = detalhe,
                    fontSize = 11.sp,
                    color = Color(0xFF85808C)
                )

                if (fracao != null) {

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFE9E6EF))
                    ) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fracao)
                                .height(5.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFF7C3AED))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(35.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7C3AED)),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "▶",
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}


// ======================================================
// CARD DE EXPLORAÇÃO
// ======================================================

@Composable
fun ExploreCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    symbol: String,
    background: Color,
    iconColor: Color,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier
            .height(125.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            IconTile(
                symbol = symbol,
                background = background,
                iconColor = iconColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF20202A)
                    )

                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = Color(0xFF8A8691),
                        maxLines = 2
                    )
                }

                Text(
                    text = "›",
                    fontSize = 23.sp,
                    color = Color(0xFF8A8691)
                )
            }
        }
    }
}


// ======================================================
// ÍCONE
// ======================================================

@Composable
fun IconTile(
    symbol: String,
    background: Color,
    iconColor: Color
) {

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = symbol,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = iconColor
        )
    }
}


// ======================================================
// BOTTOM NAVIGATION
// ======================================================

@Composable
fun BottomNavigationBar(
    navController: NavController
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .height(76.dp)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {

        BottomNavItem(
            symbol = "⌂",
            label = "Início",
            selected = true,
            onClick = {
                navController.navigate("home") {
                    popUpTo("home") {
                        inclusive = false
                    }
                }
            }
        )

        BottomNavItem(
            symbol = "⌕",
            label = "Explorar",
            selected = false,
            onClick = {
                navController.navigate("mentorship")
            }
        )

        BottomNavItem(
            symbol = "▣",
            label = "Aprender",
            selected = false,
            onClick = {
                navController.navigate("learning")
            }
        )

        BottomNavItem(
            symbol = "★",
            label = "Conquistas",
            selected = false,
            onClick = {
                navController.navigate("achievements")
            }
        )

        BottomNavItem(
            symbol = "●",
            label = "Perfil",
            selected = false,
            onClick = {
                // Será conectado depois
            }
        )
    }
}


// ======================================================
// ITEM DA BOTTOM NAV
// ======================================================

@Composable
fun BottomNavItem(
    symbol: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .size(
                width = 70.dp,
                height = 60.dp
            )
            .clickable {
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = symbol,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) {
                Color(0xFF7C3AED)
            } else {
                Color(0xFF96919E)
            }
        )

        Spacer(
            modifier = Modifier.height(3.dp)
        )

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) {
                Color(0xFF7C3AED)
            } else {
                Color(0xFF96919E)
            }
        )
    }
}