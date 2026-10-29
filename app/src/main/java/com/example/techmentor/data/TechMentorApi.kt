package com.example.techmentor.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface TechMentorApi {

    @POST("usuarios/login")
    suspend fun login(@Body body: LoginRequest): UsuarioResponse

    @POST("usuarios")
    suspend fun cadastrar(@Body body: CadastroRequest): UsuarioResponse

    @GET("usuarios/{id}")
    suspend fun obterUsuario(@Path("id") id: Long): UsuarioResponse

    @GET("matriculas/usuarios/{id}")
    suspend fun listarMatriculas(@Path("id") id: Long): List<MatriculaResponse>

    @GET("api/agendamentos")
    suspend fun listarAgendamentos(@Header("X-Usuario-Id") usuarioId: Long): List<AgendamentoResponse>

    @GET("conquistas/usuarios/{id}")
    suspend fun listarConquistasDoUsuario(@Path("id") id: Long): List<UsuarioConquistaResponse>

    @PUT("usuarios/{id}")
    suspend fun atualizarUsuario(
        @Path("id") id: Long,
        @Body body: AtualizarUsuarioRequest
    ): UsuarioResponse
}