package com.example.techmentor.data

import retrofit2.http.Body
import retrofit2.http.POST

interface TechMentorApi {

    @POST("usuarios/login")
    suspend fun login(@Body body: LoginRequest): UsuarioResponse
}