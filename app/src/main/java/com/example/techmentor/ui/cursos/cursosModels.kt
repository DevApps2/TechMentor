package com.example.techmentor.ui.cursos

data class Curso(
    val id: Int,
    val titulo: String,
    val descricao: String,
    val modulos: List<Modulo>
)