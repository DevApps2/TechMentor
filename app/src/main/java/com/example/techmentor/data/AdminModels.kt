package com.example.techmentor.data

// Modelos das telas de administração. Ficam neste arquivo, com prefixo "Admin", para não
// esbarrar nos modelos de denúncia e de oportunidade que as telas de Segurança e Oportunidades vão criar.

// ---- Denúncias ----

// Denúncia como a API devolve (GET /api/denuncias). motivo e status chegam como texto (ex.: "ASSEDIO", "ABERTA")
data class AdminDenuncia(
    val id: Long,
    val denuncianteId: Long,
    val denunciadoId: Long?,
    val motivo: String,
    val descricao: String?,
    val status: String,
    val criadaEm: String?,
    // preenchidos só quando a denúncia é sobre o comentário de uma avaliação
    val avaliacaoId: Long? = null,
    val avaliacaoNota: Int? = null,
    val avaliacaoComentario: String? = null,
    val avaliacaoOculta: Boolean? = null
)

// Corpo do PATCH /api/admin/avaliacoes/{id}/ocultacao: true esconde o comentário, false mostra de novo
data class AdminOcultacaoRequest(val oculta: Boolean)

// Corpo do PATCH /api/admin/denuncias/{id}/status: ABERTA, EM_ANALISE ou RESOLVIDA
data class AdminDenunciaStatusRequest(val status: String)


// ---- Oportunidades ----

// tipo: ESTAGIO, EMPREGO, BOLSA ou EVENTO | prazoInscricao chega como "2026-12-01"
data class AdminOportunidade(
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

// Corpo do POST e do PUT de /api/oportunidades. Campo nulo = campo vazio
data class AdminOportunidadeRequest(
    val tipo: String,
    val titulo: String,
    val organizacao: String?,
    val descricao: String?,
    val requisitos: String?,
    val localidade: String?,
    val linkExterno: String?,
    val prazoInscricao: String?,
    val beneficios: String?,
    val area: String?,
    val nivel: String?
)


// ---- Mentoras ----

// Corpo do POST /api/admin/mentoras: cria a conta (tipo MENTORA) e o perfil de uma vez
data class AdminMentoraCadastroRequest(
    val nome: String,
    val email: String,
    val senha: String,
    val bio: String?,
    val cargo: String?,
    val empresa: String?,
    val anosExperiencia: Int?,
    val areasIds: List<Long>
)

// Corpo do PUT /api/mentoras/{id}: só o perfil (o nome e o e-mail ficam como estão)
data class AdminMentoraPerfilRequest(
    val bio: String?,
    val cargo: String?,
    val empresa: String?,
    val anosExperiencia: Int?,
    val areasIds: List<Long>
)

// Corpo do POST /api/areas-atuacao
data class AdminAreaRequest(val nome: String)
