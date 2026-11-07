package com.example.techmentor.ui.cursos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color

@Composable
fun CursoScreen(curso: Curso) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8FB))
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = curso.titulo,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = curso.descricao
                )
                curso.modulos.forEach { modulo ->
                    Text(
                        text = modulo.titulo
                    )
                }

            }
        }
    }
}