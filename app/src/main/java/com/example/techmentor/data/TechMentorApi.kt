package com.example.techmentor.data

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

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

    // ---- Mentoria ----

    @GET("api/areas-atuacao")
    suspend fun listarAreas(): List<AreaResponse>

    // busca e area são opcionais: quando nulos, o Retrofit nem manda o parâmetro
    @GET("api/mentoras")
    suspend fun listarMentoras(
        @Query("busca") busca: String?,
        @Query("area") area: String?
    ): List<MentoraResponse>

    @GET("api/mentoras/{id}")
    suspend fun obterMentora(@Path("id") id: Long): MentoraResponse

    // a API devolve só os horários ainda não reservados
    @GET("api/mentoras/{id}/disponibilidades")
    suspend fun listarHorarios(@Path("id") mentoraId: Long): List<HorarioResponse>

    @POST("api/agendamentos")
    suspend fun criarAgendamento(
        @Header("X-Usuario-Id") usuarioId: Long,
        @Body body: AgendamentoRequest
    ): AgendamentoResponse

    @GET("conquistas/usuarios/{id}")
    suspend fun listarConquistasDoUsuario(@Path("id") id: Long): List<UsuarioConquistaResponse>

    @PUT("usuarios/{id}")
    suspend fun atualizarUsuario(
        @Path("id") id: Long,
        @Body body: AtualizarUsuarioRequest
    ): UsuarioResponse

    @Multipart
    @POST("usuarios/{id}/foto")
    suspend fun enviarFoto(
        @Path("id") id: Long,
        @Part foto: MultipartBody.Part
    ): UsuarioResponse

    @POST("usuarios/senha/esqueci")
    suspend fun esqueciSenha(@Body body: EsqueciSenhaRequest)

    @POST("usuarios/senha/redefinir")
    suspend fun redefinirSenha(@Body body: RedefinirSenhaRequest)
}