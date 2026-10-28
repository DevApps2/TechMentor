package com.example.techmentor.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // Emulador: 10.0.2.2 | Celular real: o IP do seu computador (ex.: 192.168.18.241)
    // Precisa terminar com "/"
    private const val BASE_URL = "https://moonlight-outsmart-backing.ngrok-free.dev/techmentor/api/"

    val api: TechMentorApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TechMentorApi::class.java)
    }
}