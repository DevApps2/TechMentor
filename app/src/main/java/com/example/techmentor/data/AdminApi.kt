package com.example.techmentor.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Chamadas das telas de administração, separadas do TechMentorApi para não dar conflito de merge
 * com quem está mexendo nele. O servidor sabe quem é a administradora pelo token do login.
 *
 * Os métodos que apagam devolvem Response<Unit> porque a API responde 204 (sem corpo).
 */
interface AdminApi {

    // ---- Denúncias ----

    // Esconde (oculta = true) ou mostra de novo o comentário de uma avaliação. A nota continua valendo na média.
    @PATCH("api/admin/avaliacoes/{id}/ocultacao")
    suspend fun ocultarAvaliacao(
        @Path("id") avaliacaoId: Long,
        @Body body: AdminOcultacaoRequest
    )

    @GET("api/denuncias")
    suspend fun listarDenuncias(): List<AdminDenuncia>

    @GET("api/denuncias/{id}")
    suspend fun obterDenuncia(
        @Path("id") id: Long
    ): AdminDenuncia

    @PATCH("api/admin/denuncias/{id}/status")
    suspend fun alterarStatusDenuncia(
        @Path("id") id: Long,
        @Body body: AdminDenunciaStatusRequest
    ): AdminDenuncia


    // ---- Usuários (para mostrar nomes e contar alunas) ----

    @GET("usuarios")
    suspend fun listarUsuarios(): List<UsuarioResponse>


    // ---- Oportunidades ----

    // só as ativas; a exclusão na API apenas desativa
    @GET("api/oportunidades")
    suspend fun listarOportunidades(): List<AdminOportunidade>

    @GET("api/oportunidades/{id}")
    suspend fun obterOportunidade(@Path("id") id: Long): AdminOportunidade

    // quem se candidatou (ou se inscreveu) a esta oportunidade
    @GET("api/admin/oportunidades/{id}/candidaturas")
    suspend fun listarCandidaturas(
        @Path("id") oportunidadeId: Long
    ): List<AdminCandidatura>

    // muda a situação da candidatura; a candidata recebe uma notificação
    @PATCH("api/admin/candidaturas/{id}/status")
    suspend fun alterarStatusDaCandidatura(
        @Path("id") candidaturaId: Long,
        @Body body: AdminCandidaturaStatusRequest
    ): AdminCandidatura

    @POST("api/oportunidades")
    suspend fun criarOportunidade(
        @Body body: AdminOportunidadeRequest
    ): AdminOportunidade

    @PUT("api/oportunidades/{id}")
    suspend fun atualizarOportunidade(
        @Path("id") id: Long,
        @Body body: AdminOportunidadeRequest
    ): AdminOportunidade

    @DELETE("api/oportunidades/{id}")
    suspend fun excluirOportunidade(
        @Path("id") id: Long
    ): Response<Unit>


    // ---- Mentoras (a lista e o perfil públicos continuam no TechMentorApi) ----

    @POST("api/admin/mentoras")
    suspend fun cadastrarMentora(
        @Body body: AdminMentoraCadastroRequest
    ): MentoraResponse

    @PUT("api/mentoras/{id}")
    suspend fun atualizarMentora(
        @Path("id") id: Long,
        @Body body: AdminMentoraPerfilRequest
    ): MentoraResponse

    @DELETE("api/mentoras/{id}")
    suspend fun excluirMentora(
        @Path("id") id: Long
    ): Response<Unit>

    @POST("api/areas-atuacao")
    suspend fun criarArea(
        @Body body: AdminAreaRequest
    ): AreaResponse

    // ---- Parceiras ----

    @GET("api/admin/parceiras")
    suspend fun listarParceiras(@retrofit2.http.Query("situacao") situacao: String? = null): List<AdminParceiraResponse>

    @GET("api/admin/parceiras/{id}")
    suspend fun obterParceira(@Path("id") id: Long): AdminParceiraResponse

    @POST("api/admin/parceiras")
    suspend fun convidarParceira(@Body body: AdminConviteParceiraRequest): AdminParceiraResponse

    @POST("api/admin/parceiras/{id}/convite")
    suspend fun reenviarConviteDaParceira(@Path("id") id: Long)

    @PUT("api/admin/parceiras/{id}/verificacao")
    suspend fun verificarParceira(@Path("id") id: Long, @Body body: AdminVerificacaoParceiraRequest): AdminParceiraResponse

    @POST("api/admin/parceiras/{id}/suspensao")
    suspend fun suspenderParceira(@Path("id") id: Long, @Body body: AdminMotivoParceiraRequest): AdminParceiraResponse

    @POST("api/admin/parceiras/{id}/reativacao")
    suspend fun reativarParceira(@Path("id") id: Long): AdminParceiraResponse

    @GET("api/admin/parceiras/{id}/historico")
    suspend fun historicoDaParceira(@Path("id") id: Long): List<AdminHistoricoParceiraResponse>

    // ---- Revisão de cursos ----

    @GET("api/admin/cursos")
    suspend fun listarCursosAdmin(
        @retrofit2.http.Query("situacao") situacao: String? = null
    ): List<AdminCursoResponse>

    @GET("api/admin/cursos/{id}")
    suspend fun obterCursoAdmin(@Path("id") id: Long): AdminCursoResponse

    @PATCH("api/admin/cursos/{id}/situacao")
    suspend fun alterarSituacaoCurso(
        @Path("id") id: Long,
        @Body body: AdminCursoSituacaoRequest
    ): AdminCursoResponse

    @POST("api/admin/cursos/{id}/devolucao")
    suspend fun devolverCursoParaAjustes(
        @Path("id") id: Long,
        @Body body: AdminCursoComentarioRequest
    ): AdminCursoResponse

    @POST("api/admin/cursos/{id}/recusa")
    suspend fun recusarCurso(
        @Path("id") id: Long,
        @Body body: AdminCursoComentarioRequest
    ): AdminCursoResponse
}
