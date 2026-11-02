package com.example.techmentor.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Chamadas das telas de administração, separadas do TechMentorApi para não dar conflito de merge
 * com quem está mexendo nele. Quem altera dados manda o ID da administradora em X-Usuario-Id.
 *
 * Os métodos que apagam devolvem Response<Unit> porque a API responde 204 (sem corpo).
 */
interface AdminApi {

    // ---- Denúncias ----

    @GET("api/denuncias")
    suspend fun listarDenuncias(@Header("X-Usuario-Id") adminId: Long): List<AdminDenuncia>

    @GET("api/denuncias/{id}")
    suspend fun obterDenuncia(
        @Header("X-Usuario-Id") adminId: Long,
        @Path("id") id: Long
    ): AdminDenuncia

    @PATCH("api/admin/denuncias/{id}/status")
    suspend fun alterarStatusDenuncia(
        @Header("X-Usuario-Id") adminId: Long,
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

    @POST("api/oportunidades")
    suspend fun criarOportunidade(
        @Header("X-Usuario-Id") adminId: Long,
        @Body body: AdminOportunidadeRequest
    ): AdminOportunidade

    @PUT("api/oportunidades/{id}")
    suspend fun atualizarOportunidade(
        @Header("X-Usuario-Id") adminId: Long,
        @Path("id") id: Long,
        @Body body: AdminOportunidadeRequest
    ): AdminOportunidade

    @DELETE("api/oportunidades/{id}")
    suspend fun excluirOportunidade(
        @Header("X-Usuario-Id") adminId: Long,
        @Path("id") id: Long
    ): Response<Unit>


    // ---- Mentoras (a lista e o perfil públicos continuam no TechMentorApi) ----

    @POST("api/admin/mentoras")
    suspend fun cadastrarMentora(
        @Header("X-Usuario-Id") adminId: Long,
        @Body body: AdminMentoraCadastroRequest
    ): MentoraResponse

    @PUT("api/mentoras/{id}")
    suspend fun atualizarMentora(
        @Header("X-Usuario-Id") adminId: Long,
        @Path("id") id: Long,
        @Body body: AdminMentoraPerfilRequest
    ): MentoraResponse

    @DELETE("api/mentoras/{id}")
    suspend fun excluirMentora(
        @Header("X-Usuario-Id") adminId: Long,
        @Path("id") id: Long
    ): Response<Unit>

    @POST("api/areas-atuacao")
    suspend fun criarArea(
        @Header("X-Usuario-Id") adminId: Long,
        @Body body: AdminAreaRequest
    ): AreaResponse
}
