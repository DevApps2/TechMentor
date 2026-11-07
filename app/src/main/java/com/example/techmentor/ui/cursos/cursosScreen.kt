package com.example.techmentor.ui.cursos

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.techmentor.R

@Composable
fun CursoScreen(curso: Curso) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 35.dp, start = 20.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Box(
            modifier = Modifier.size(60.dp),
            contentAlignment = Alignment.Center
        ){
            Image(
                painter = painterResource(R.drawable.ic_launcher_background),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(shape = RoundedCornerShape(size = 12.dp))
            )
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(5.dp)
            )
        }
            Text(
                text = "Techmentor",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 25.sp
            )
    }
        Text(
            text = "Aprenda tecnologia",
            fontWeight =FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 20.dp, top = 40.dp, end = 20.dp)
        )
        Text(
            text = "Trilhas práticas para transformar seu futuro.",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Gray,
            fontSize = 15.sp,
            modifier = Modifier
            .padding(start = 20.dp, bottom = 20.dp, end = 20.dp, top = 0.dp)
        )
    }


//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color(0xFFF8F8FB))
//            .statusBarsPadding()
//            .padding(16.dp)
//    ) {
//        Card(
//            modifier = Modifier
//                .fillMaxWidth()
//        ) {
//            Column(
//                modifier = Modifier
//                    .padding(16.dp)
//            ) {
//                Text(
//                    text = curso.titulo,
//                    style = MaterialTheme.typography.headlineMedium
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Text(
//                    text = curso.descricao
//                )
//                curso.modulos.forEach { modulo ->
//                    Text(
//                        text = modulo.titulo
//                    )
//                }
//
//            }
//        }
//    }
}