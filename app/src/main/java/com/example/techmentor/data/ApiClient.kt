package com.example.techmentor.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // Emulador: 10.0.2.2 | Celular real: o IP do seu computador (ex.: 192.168.18.241)
    // Precisa terminar com "/"
    private const val BASE_URL = "https://moonlight-outsmart-backing.ngrok-free.dev/techmentor/api/"

    // Um Retrofit só, usado pela API do app e pela API do admin
    private val retrofit: Retrofit by lazy {
        val client = OkHttpClient.Builder()
            // Toda chamada passa por aqui: se há login, vai com o token
            .addInterceptor { chain ->
                val token = Sessao.token
                val pedido = if (token != null) {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(pedido)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
                redactHeader("Authorization") // não deixa o token aparecer no Logcat
            })
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: TechMentorApi by lazy { retrofit.create(TechMentorApi::class.java) }

    /** Telas da administradora (denúncias, oportunidades e mentoras). */
    val adminApi: AdminApi by lazy { retrofit.create(AdminApi::class.java) }

    /**
     * O backend devolve a foto de duas formas:
     * - link completo (https://...), como nas fotos de teste do seed
     * - caminho relativo (/fotos/arquivo.jpg), quando a foto veio da galeria
     * Esta função devolve sempre uma URL pronta para o Coil carregar.
     */
    fun urlDaFoto(fotoUrl: String?): String? = when {
        fotoUrl.isNullOrBlank() -> null
        fotoUrl.startsWith("/") -> BASE_URL.trimEnd('/') + fotoUrl
        else -> fotoUrl
    }
}