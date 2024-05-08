package com.example.portal.api

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitInstance {
    companion object{
        private const val mainURL = "https://d36f-49-147-94-8.ngrok-free.app"
        fun getRetrofitInstance(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(mainURL)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                .build()
        }
    }
}