package com.example.techmentor.ui.oportunidades

import com.example.techmentor.data.ApiClient
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

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

interface OportunidadesApi {

    // tipo: ESTAGIO, EMPREGO, BOLSA ou EVENTO; nulo = todos
    @GET("api/oportunidades")
    suspend fun listar(
        @Query("tipo") tipo: String?,
        @Query("area") area: String?
    ): List<OportunidadeResponse>
}

/**
 * Cliente só da tela de Oportunidades.
 * O endereço do servidor vem do ApiClient do projeto (ele devolve a BASE_URL quando recebe "/"),
 * então se o grupo trocar o link do ngrok, esta tela acompanha sozinha.
 */
internal object OportunidadesApiClient {

    val api: OportunidadesApi by lazy {

        val baseUrl = ApiClient.urlDaFoto("/") ?: error("BASE_URL não encontrada")

        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OportunidadesApi::class.java)
    }
}
