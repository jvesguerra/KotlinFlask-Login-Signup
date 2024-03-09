package com.example.portal

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitInstance {
    companion object{
        private const val mainURL = "http://192.168.1.22:5000"
        fun getRetrofitInstance(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(mainURL)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                .build()
        }
    }
}