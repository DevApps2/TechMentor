package com.example.techmentor.ui.cursos

data class Curso(
    val id: Int,
    val titulo: String,
    val dificuldade: String,
    val finalidade: String,
    val tempoMax: Int,
    val totalXP: Int,
    val iniciado: Boolean,
    val modulos: List<Modulo>
)