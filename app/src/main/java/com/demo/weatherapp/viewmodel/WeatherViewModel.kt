package com.demo.weatherapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.weatherapp.model.WeatherData
import com.demo.weatherapp.model.WeatherRepository
import kotlinx.coroutines.launch

// ViewModel class for weather information.
class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {

    // Private LiveData for weather data which can be mutated within the ViewModel.
    private val _weatherData = MutableLiveData<WeatherData?>()
    // Public LiveData for weather data which can be observed by the UI.
    val weatherData: MutableLiveData<WeatherData?> get() = _weatherData

    // Private LiveData for error messages which can be mutated within the ViewModel.
    private val _errorMessage = MutableLiveData<String>()
    // Public LiveData for error messages which can be observed by the UI.
    val errorMessage: LiveData<String> get() = _errorMessage

    // Fetches weather data for the given city.
    fun fetchWeather(city: String) {
        Log.d("WeatherViewModel", "Fetching weather for city: $city")

        // Using viewModelScope to launch a coroutine.
        viewModelScope.launch {
            try {
                // Log before fetching geolocation for the city.
                Log.d("WeatherViewModel", "Attempting to fetch geolocation for city: $city")
                // Fetch geolocation data for the given city from the repository.
                val geoLocation = repository.getGeoLocationForCity(city)

                if (geoLocation != null) {
                    Log.d("WeatherViewModel", "Got geolocation: ${geoLocation.lat}, ${geoLocation.lon}. Now fetching weather data.")
                    // Fetch weather data using the geolocation coordinates.
                    val response = repository.getWeatherByCoordinates(geoLocation.lat, geoLocation.lon)

                    // Update the weatherData LiveData with the fetched data.
                    _weatherData.value = response
                } else {
                    // Update the errorMessage LiveData if geolocation fetch failed.
                    _errorMessage.value = "Failed to fetch geolocation for city: $city"
                }
            } catch (e: Exception) {
                // Log the exception and update the errorMessage LiveData with the error message.
                Log.e("WeatherViewModel", "Error fetching weather for city: $city. Error: ${e.message}", e)
                _errorMessage.value = e.message
            }
        }
    }

    // Fetches weather data for the given latitude and longitude.
    // This is used when getting the device's current location.
    fun fetchWeatherDevice(lat: Double, lon: Double) {
        // Using viewModelScope to launch a coroutine.
        viewModelScope.launch {
            try {
                // Log before fetching weather for the coordinates.
                Log.d("WeatherViewModel", "Fetching weather for coordinates: Lat: $lat, Lon: $lon")
                // Fetch weather data for the given coordinates from the repository.
                val response = repository.getWeatherByCoordinates(lat, lon)

                // Update the weatherData LiveData with the fetched data.
                _weatherData.value = response
            } catch (e: Exception) {
                // Log the exception and update the errorMessage LiveData with the error message.
                Log.e("WeatherViewModel", "Error fetching weather for coordinates: Lat: $lat, Lon: $lon. Error: ${e.message}", e)
                _errorMessage.value = e.message
            }
        }
    }
}




