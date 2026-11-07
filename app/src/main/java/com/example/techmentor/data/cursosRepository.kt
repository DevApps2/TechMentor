package com.example.techmentor.data

import com.example.techmentor.ui.cursos.Aula
import com.example.techmentor.ui.cursos.Curso
import com.example.techmentor.ui.cursos.Modulo

object CursosRepository {

    val modulo1Aula1 = Aula(
        id = 1,
        titulo = "Váriaveis",
        concluida = false
    )

    val modulo1Aula2 = Aula(
        id = 2,
        titulo = "Tipos de dados",
        concluida = false
    )

    val modulo1 = Modulo(
        id = 1,
        titulo = "Variáveis e tipos de valores",
        aulas = mutableListOf(modulo1Aula1, modulo1Aula2)
    )

    val modulo2Aula1 = Aula(
        id = 3,
        titulo = "Laços de repetição",
        concluida = false
    )

    val modulo2Aula2 = Aula(
        id = 4,
        titulo = "Operadores lógicos",
        concluida = false
    )

    val modulo2Aula3 = Aula(
        id = 5,
        titulo = "Arrays",
        concluida = false
    )

    val modulo2 = Modulo(
        id = 2,
        titulo = "Laços de repetição, operadores lógicos e arrays",
        aulas = mutableListOf(modulo2Aula1, modulo2Aula2, modulo2Aula3)
    )

    val cursoPython = Curso(
        id = 1,
        titulo = "Curso de Python",
        descricao = "Um curso sobre a linguagem de programação Python. Considerada uma linguagem simples, ela precisa de menos expressões para criar um código, com seus programas sendo menores que os de outras linguagens.",
        modulos = mutableListOf(modulo1, modulo2)
    )
}