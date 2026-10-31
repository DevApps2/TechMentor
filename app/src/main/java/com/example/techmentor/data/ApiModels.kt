package com.example.techmentor.data

data class LoginRequest(val email: String, val senha: String)

data class UsuarioResponse(
    val id: Long,
    val nome: String,
    val email: String,
    val fotoUrl: String?,
    val tipo: String,
    val xpTotal: Int,
    val nivel: Int,
    val tituloNivel: String?,
    val xpProximoNivel: Int,
    val xpFaltante: Int,
    val sequenciaDias: Int,
    val bio: String?,
    val areasInteresse: List<String>?
)

data class CadastroRequest(val nome: String, val email: String, val senha: String)

data class ErroResponse(val erro: String?)

// Curso em que a usuária está matriculada, já com o progresso (GET /matriculas/usuarios/{id})
data class MatriculaResponse(
    val id: Long,
    val usuarioId: Long,
    val cursoId: Long,
    val tituloCurso: String,
    val categoria: String?,
    val matriculadaEm: String?,
    val concluidoEm: String?,
    val aulasConcluidas: Long,
    val totalAulas: Long,
    val percentualConcluido: Int
)

// Mentoria agendada (GET /api/agendamentos); as datas chegam como "2026-11-10T14:00:00"
data class AgendamentoResponse(
    val id: Long,
    val alunaId: Long,
    val alunaNome: String,
    val mentoraId: Long,
    val mentoraNome: String,
    val horarioId: Long,
    val inicio: String,
    val fim: String,
    val status: String,
    val assunto: String?,
    val criadaEm: String?
)

// Conquista desbloqueada por uma usuária (GET /conquistas/usuarios/{id})
data class UsuarioConquistaResponse(
    val id: Long,
    val usuarioId: Long,
    val conquistaId: Long,
    val nomeConquista: String,
    val desbloqueadaEm: String?
)

// Corpo do PUT /usuarios/{id}. Campos nulos não são enviados:
// bio e areasInteresse omitidos = a API mantém o que já existe.
data class AtualizarUsuarioRequest(
    val nome: String,
    val email: String,
    val fotoUrl: String?,
    val bio: String?,
    val areasInteresse: List<String>?
)

// Corpo do POST /usuarios/senha/esqueci: pede o código por e-mail
data class EsqueciSenhaRequest(val email: String)

// Corpo do POST /usuarios/senha/redefinir: o código recebido e a senha nova
data class RedefinirSenhaRequest(
    val email: String,
    val codigo: String,
    val novaSenha: String
)

// Área de atuação das mentoras, usada nos filtros (GET /api/areas-atuacao)
data class AreaResponse(
    val id: Long,
    val nome: String
)

// Perfil de mentora (GET /api/mentoras e GET /api/mentoras/{id})
data class MentoraResponse(
    val id: Long,
    val usuarioId: Long,
    val nome: String,
    val fotoUrl: String?,
    val bio: String?,
    val cargo: String?,
    val empresa: String?,
    val anosExperiencia: Int,
    val notaMedia: Double,
    val totalAvaliacoes: Int,
    val areas: List<String>?
)

// Horário de uma mentora (GET /api/mentoras/{id}/disponibilidades); as datas chegam como "2026-11-10T14:00:00"
data class HorarioResponse(
    val id: Long,
    val mentoraId: Long,
    val inicio: String,
    val fim: String,
    val reservado: Boolean
)

// Corpo do POST /api/agendamentos: o horário escolhido e, se quiser, o assunto da conversa
data class AgendamentoRequest(
    val horarioId: Long,
    val assunto: String?
)