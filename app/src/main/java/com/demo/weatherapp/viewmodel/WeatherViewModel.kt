package com.demo.weatherapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.weatherapp.model.WeatherData
import com.demo.weatherapp.model.WeatherRepository
import kotlinx.coroutines.launch

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {

    private val _weatherData = MutableLiveData<WeatherData?>()
    val weatherData: MutableLiveData<WeatherData?> get() = _weatherData

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    fun fetchWeather(city: String) {
        Log.d("WeatherViewModel", "Fetching weather for city: $city")
        viewModelScope.launch {
            try {
                // Log before fetching geolocation
                Log.d("WeatherViewModel", "Attempting to fetch geolocation for city: $city")
                val geoLocation = repository.getGeoLocationForCity(city)

                if (geoLocation != null) {
                    Log.d("WeatherViewModel", "Got geolocation: ${geoLocation.lat}, ${geoLocation.lon}. Now fetching weather data.")
                    val response = repository.getWeatherByCoordinates(geoLocation.lat, geoLocation.lon)

                    _weatherData.value = response
                } else {
                    _errorMessage.value = "Failed to fetch geolocation for city: $city"
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching weather for city: $city. Error: ${e.message}", e)
                _errorMessage.value = e.message
            }
        }
    }

    fun fetchWeatherDevice(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                // Log before fetching weather
                Log.d("WeatherViewModel", "Fetching weather for coordinates: Lat: $lat, Lon: $lon")
                val response = repository.getWeatherByCoordinates(lat, lon)

                _weatherData.value = response
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching weather for coordinates: Lat: $lat, Lon: $lon. Error: ${e.message}", e)
                _errorMessage.value = e.message
            }
        }
    }
}



