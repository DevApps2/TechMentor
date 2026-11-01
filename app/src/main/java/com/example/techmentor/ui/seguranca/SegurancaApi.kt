package com.example.techmentor.ui.seguranca

import com.example.techmentor.data.ApiClient
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// tipo chega como "ORIENTACAO" ou "CANAL_AJUDA"; contato só existe nos canais de ajuda
data class ConteudoSegurancaResponse(
    val id: Long,
    val tipo: String,
    val titulo: String,
    val descricao: String?,
    val contato: String?
)

interface SegurancaApi {

    // Devolve orientações e canais de ajuda juntos; a tela separa pelo tipo
    @GET("api/seguranca/conteudos")
    suspend fun listarConteudos(): List<ConteudoSegurancaResponse>
}

/**
 * Cliente só da tela de Segurança.
 * O endereço do servidor vem do ApiClient do projeto (ele devolve a BASE_URL quando recebe "/").
 */
internal object SegurancaApiClient {

    val api: SegurancaApi by lazy {

        val baseUrl = ApiClient.urlDaFoto("/") ?: error("BASE_URL não encontrada")

        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SegurancaApi::class.java)
    }
}
