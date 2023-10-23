package com.demo.weatherapp.model

import android.util.Log
class WeatherRepository(private val apiService: WeatherApi) {

    suspend fun getGeoLocationForCity(city: String): GeoLocationResponse? {
        try {
            val response = apiService.getGeoLocationByName(city, limit = 1, apiKey = "5d7343fe6e75f8517e276bd751638ebc")
            if (response.isSuccessful) {
                return response.body()?.firstOrNull()
            } else {
                Log.e("GeoLocationError", "Failed to fetch geolocation. Response Code: ${response.code()}, Response Message: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("GeoLocationError", "Exception while fetching geolocation: ${e.message}", e)
        }
        return null
    }

    suspend fun getWeatherByCoordinates(lat: Double, lon: Double): WeatherData? {
        try {
            val response = apiService.getWeather(lat, lon, apiKey = "5d7343fe6e75f8517e276bd751638ebc")
            if (response.isSuccessful) {
                return response.body()
            } else {
                Log.e("WeatherDataError", "Failed to fetch weather data. Response Code: ${response.code()}, Response Message: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("WeatherDataError", "Exception while fetching weather data: ${e.message}", e)
        }
        return null
    }

}
