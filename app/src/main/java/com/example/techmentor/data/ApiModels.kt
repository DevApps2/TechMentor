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
    val criadaEm: String?,
    // link da sala do Google Meet; nulo até a mentora confirmar
    val linkSala: String? = null
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

// Oportunidade (GET /api/oportunidades); prazoInscricao chega como "2026-12-15"
data class OportunidadeResponse(
    val id: Long,
    val tipo: String,
    val titulo: String,
    val organizacao: String?,
    val descricao: String?,
    val requisitos: String?,
    val localidade: String?,
    val linkExterno: String?,
    val prazoInscricao: String?,
    val ativa: Boolean,
    val beneficios: String?,
    val area: String?,
    val nivel: String?
)

// Orientação de segurança ou canal de ajuda (GET /api/seguranca/conteudos).
// tipo: ORIENTACAO ou CANAL_AJUDA; contato só existe nos canais de ajuda
data class ConteudoSegurancaResponse(
    val id: Long,
    val tipo: String,
    val titulo: String,
    val descricao: String?,
    val contato: String?
)

// Dados mínimos de um usuário para buscas (GET /usuarios/busca)
data class UsuarioResumoResponse(
    val id: Long,
    val nome: String,
    val fotoUrl: String?,
    val tipo: String
)

// Corpo do POST /api/denuncias. usuarioDenunciadoId é opcional (denúncia geral).
// motivo: ASSEDIO, COMPORTAMENTO_INADEQUADO, SPAM, DISCURSO_DE_ODIO, PERFIL_FALSO ou OUTRO
data class DenunciaRequest(
    val usuarioDenunciadoId: Long?,
    val motivo: String,
    val descricao: String
)

data class DenunciaResponse(
    val id: Long,
    val denuncianteId: Long,
    val denunciadoId: Long?,
    val denunciadoNome: String?,
    val motivo: String,
    val descricao: String,
    val status: String,
    val criadaEm: String?
)

// Corpo do POST /api/bloqueios e item da lista (GET /api/bloqueios)
data class BloqueioRequest(val usuarioId: Long)

data class BloqueioResponse(
    val usuarioId: Long,
    val nome: String,
    val criadoEm: String?
)

// Candidatura ou inscrição em evento (POST /api/oportunidades/{id}/candidaturas e /inscricoes).
// status: ENVIADA, EM_ANALISE, APROVADA ou RECUSADA
data class InscricaoResponse(
    val id: Long,
    val usuarioId: Long,
    val oportunidadeId: Long,
    val titulo: String,
    val status: String,
    val criadaEm: String?,
)

// Corpo do POST /api/agendamentos: o horário escolhido e, se quiser, o assunto da conversa
data class AgendamentoRequest(
    val horarioId: Long,
    val assunto: String?
)

// Corpo do POST /api/mentoras (criar o perfil) e do PUT /api/mentoras/{id} (editar)
data class MentoraRequest(
    val bio: String?,
    val cargo: String?,
    val empresa: String?,
    val anosExperiencia: Int?,
    val areasIds: List<Long>
)

// Corpo do POST /api/mentoras/{id}/disponibilidades; as datas vão como "2026-11-10T14:00:00"
data class HorarioRequest(
    val inicio: String,
    val fim: String
)

// Corpo do PATCH /api/agendamentos/{id}/reagendamento: o novo horário, da mesma mentora
data class ReagendamentoRequest(val horarioId: Long)

// Mentoria concluída que a pessoa ainda pode avaliar (GET /api/avaliacoes/pendentes). voce: ALUNA ou MENTORA
data class AvaliacaoPendenteResponse(
    val mentoriaId: Long,
    val voce: String,
    val outraPessoaNome: String,
    val outraPessoaFotoUrl: String?,
    val inicio: String,
    val prazoFinal: String
)

// Corpo do POST /api/agendamentos/{id}/avaliacoes: de 1 a 5 estrelas e um comentário opcional
data class AvaliacaoRequest(
    val nota: Int,
    val comentario: String?
)

// Avaliação registrada. autor: ALUNA ou MENTORA
data class AvaliacaoResponse(
    val id: Long,
    val mentoriaId: Long,
    val autor: String,
    val avaliadorNome: String,
    val nota: Int,
    val comentario: String?,
    val criadaEm: String?
)

// GET /api/agendamentos/{id}/avaliacoes: se a pessoa pode avaliar agora (e o motivo, se não), o que já avaliou e o que recebeu
data class AvaliacaoDaMentoriaResponse(
    val mentoriaId: Long,
    val podeAvaliar: Boolean,
    val motivoDeNaoPoder: String?,
    val prazoFinal: String?,
    val minhaAvaliacao: AvaliacaoResponse?,
    val recebida: AvaliacaoResponse?
)

// Avaliação pública de uma mentora; o nome da aluna vem abreviado ("Daniela M.")
data class AvaliacaoPublicaResponse(
    val id: Long,
    val avaliadora: String,
    val nota: Int,
    val comentario: String?,
    val criadaEm: String?
)

// GET /api/mentoras/{id}/avaliacoes. distribuicao: "5", "4", "3", "2", "1" -> quantas avaliações de cada nota
data class AvaliacoesDaMentoraResponse(
    val notaMedia: Double,
    val totalAvaliacoes: Int,
    val distribuicao: Map<String, Long>,
    val avaliacoes: List<AvaliacaoPublicaResponse>
)
