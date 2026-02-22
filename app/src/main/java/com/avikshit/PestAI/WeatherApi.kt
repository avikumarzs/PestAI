package com.avikshit.PestAI

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Data models to match the JSON structure from OpenWeatherMap
data class WeatherResponse(
    val main: Main,
    val wind: Wind,
    val weather: List<Weather>,
    val name: String
)

data class Main(val temp: Double)
data class Wind(val speed: Double)
data class Weather(val description: String)

// Retrofit interface for the OpenWeatherMap API
interface WeatherApi {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/"

        fun create(): WeatherApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WeatherApi::class.java)
        }
    }
}