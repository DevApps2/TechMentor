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


@Composable
fun HomeScreen(
    navController: NavController
) {

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8FB))
    ) {

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 18.dp,
                    bottom = 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // ==================================================
            // CABEÇALHO
            // ==================================================

            WelcomeSection()

            // ==================================================
            // SUA JORNADA
            // ==================================================

            JourneyCard()

            // ==================================================
            // PRÓXIMO PASSO
            // ==================================================

            SectionHeader(
                title = "Próximo passo",
                action = "Ver mentoras",
                onAction = {
                    navController.navigate("mentorship")
                }
            )

            NextActionCard(
                onClick = {
                    navController.navigate("mentorship")
                }
            )

            // ==================================================
            // CONTINUE APRENDENDO
            // ==================================================

            SectionHeader(
                title = "Continue aprendendo",
                action = "Ver tudo",
                onAction = {
                    navController.navigate("learning")
                }
            )

            ContinueLearningCard(
                onClick = {
                    navController.navigate("learning")
                }
            )

            // ==================================================
            // EXPLORE TECHMENTOR
            // ==================================================

            Text(
                text = "Explore o TechMentor",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20202A)
            )

            // PRIMEIRA LINHA
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
                    onClick = {
                        navController.navigate("mentorship")
                    }
                )

                ExploreCard(
                    modifier = Modifier.weight(1f),
                    title = "Aprender",
                    description = "Novas habilidades",
                    symbol = "▣",
                    background = Color(0xFFE6EFFF),
                    iconColor = Color(0xFF2563EB),
                    onClick = {
                        navController.navigate("learning")
                    }
                )
            }

            // SEGUNDA LINHA
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
                    onClick = {
                        navController.navigate("opportunities")
                    }
                )

                ExploreCard(
                    modifier = Modifier.weight(1f),
                    title = "Segurança",
                    description = "Apoio e proteção",
                    symbol = "◆",
                    background = Color(0xFFFCE7F3),
                    iconColor = Color(0xFFDB2777),
                    onClick = {
                        navController.navigate("safety")
                    }
                )
            }
        }

        // ==================================================
        // BARRA INFERIOR
        // ==================================================

        BottomNavigationBar(
            navController = navController
        )
    }
}


// ======================================================
// CABEÇALHO
// ======================================================

@Composable
fun WelcomeSection() {

    val date = SimpleDateFormat(
        "EEEE, d 'DE' MMMM",
        Locale.forLanguageTag("pt-BR")
    )
        .format(Date())
        .uppercase()

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

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = "Olá, Júlia!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF17151F)
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = "Pronta para dar mais um passo?",
                fontSize = 14.sp,
                color = Color(0xFF797486)
            )
        }

        // AVATAR
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Color(0xFFEDE9FE)),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "J",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6D28D9)
            )
        }
    }
}


// ======================================================
// CARD DA JORNADA
// ======================================================

@Composable
fun JourneyCard() {

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

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = "1.240 XP acumulados",
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
                        text = "Nível 7",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

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
                    text = "1.500 XP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD9D5FA)
                )
            }

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            // BARRA DE PROGRESSO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Color.White.copy(alpha = 0.20f)
                    )
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.827f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White)
                )
            }

            Spacer(
                modifier = Modifier.height(9.dp)
            )

            Text(
                text = "Faltam 260 XP · 5 dias de sequência",
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

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Converse com uma mentora",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF20202A)
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = "Explore horários e agende uma conversa.",
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

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "PROGRAMAÇÃO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED)
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = "Lógica de Programação",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF20202A)
                )

                Text(
                    text = "8 de 18 aulas",
                    fontSize = 11.sp,
                    color = Color(0xFF85808C)
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFE9E6EF))
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(8f / 18f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF7C3AED))
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

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
            .height(76.dp)
            .background(Color.White)
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