package com.demo.weatherapp.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.demo.weatherapp.R
import com.demo.weatherapp.model.WeatherApi
import com.demo.weatherapp.model.WeatherRepository
import com.demo.weatherapp.model.WeatherViewModelFactory
import com.demo.weatherapp.viewmodel.WeatherViewModel
import com.google.android.gms.location.*
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SearchWeatherActivity : AppCompatActivity() {

    private lateinit var viewModel: WeatherViewModel

    // Declare FusedLocationProviderClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // Initializing the views
    // Declare your views
    private lateinit var cityEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var weatherDetailsLayout: LinearLayout
    private lateinit var currentTempTextView: TextView
    private lateinit var descriptionView: LinearLayout
    private lateinit var highTempTextView: TextView
    private lateinit var lowTempTextView: TextView
    private lateinit var descriptionTextView: TextView

    // SharedPreferences for saving and retrieving the last searched city
    private val sharedPref by lazy {
        getSharedPreferences("weather_app_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_weather)


        // Bind your views
        cityEditText = findViewById(R.id.cityEditText)
        searchButton = findViewById(R.id.searchButton)
        weatherDetailsLayout = findViewById(R.id.weatherDetailsLayout)
        currentTempTextView = findViewById(R.id.currentTempTextView)
        highTempTextView = findViewById(R.id.highTempTextView)
        lowTempTextView = findViewById(R.id.lowTempTextView)
        descriptionView = findViewById(R.id.desc)
        descriptionTextView = findViewById(R.id.descriptionTextView)

        //Initialize and bind view
        val displaySearchBox = findViewById<TextView>(R.id.displaySearchBox)
        val weatherIconImageView = findViewById<ImageView>(R.id.weatherIconImageView)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Load the last searched city upon app launch
        cityEditText.text = Editable.Factory.getInstance().newEditable(getLastSearchedCity())

        // Initialize the Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()

        // Create the WeatherApi service
        val weatherApiService = retrofit.create(WeatherApi::class.java)

        // Now, create the repository using the initialized WeatherApi service
        val repository = WeatherRepository(weatherApiService)

        val factory = WeatherViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[WeatherViewModel::class.java]


        // Observing LiveData for weather data
        viewModel.weatherData.observe(this) { data ->
            // Update the UI with the weather data
            if (data != null) {
                val tempInKelvin = data.main.temp
                val tempMaxInKelvin = data.main.temp_max
                val tempMinInKelvin = data.main.temp_min

                val tempInFahrenheit = (tempInKelvin - 273.15) * 9/5 + 32
                val tempMaxInFahrenheit = (tempMaxInKelvin - 273.15) * 9/5 + 32
                val tempMinInFahrenheit = (tempMinInKelvin - 273.15) * 9/5 + 32

                currentTempTextView.text = String.format("%.1f°F", tempInFahrenheit)
                highTempTextView.text = String.format("%.1f°F", tempMaxInFahrenheit)
                lowTempTextView.text = String.format("%.1f°F", tempMinInFahrenheit)
                weatherDetailsLayout.visibility = View.VISIBLE
                descriptionView.visibility = View.VISIBLE
                descriptionTextView.text = data.weather[0].description
            }
            // Replace with your logic to get the weather icon URL
            val weatherIconUrl = "https://cdn.iconscout.com/icon/free/png-512/free-weather-191-461610.png?f=webp&w=512"

            // Use Glide to load the image into the ImageView
            Glide.with(this)
                .load(weatherIconUrl)
                .into(weatherIconImageView)

            weatherIconImageView.visibility = View.VISIBLE
        }

        // Observing LiveData for errors
        viewModel.errorMessage.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }

        searchButton.setOnClickListener {
            displaySearchBox.text = cityEditText.text
            displaySearchBox.visibility = View.VISIBLE

            val city = cityEditText.text.toString()
            if (city.isNotEmpty()) {
                // This fetchWeather method internally first fetches the geolocation for the city,
                // and then fetches the weather data using that geolocation.
                viewModel.fetchWeather(city)
            }
        }

        // Check for saved city name and fetch weather for it
        val savedCity = getPreferences(Context.MODE_PRIVATE).getString("lastSearchedCity", "")
        if (savedCity?.isNotEmpty() == true) {
            // Display the last searched city in the displaySearchBox
            displaySearchBox.text = savedCity
            displaySearchBox.visibility = View.VISIBLE
            viewModel.fetchWeather(savedCity)
        } else {
            // Use device location for initial weather load
            fetchWeatherUsingDeviceLocation()
        }
    }

    override fun onPause() {
        super.onPause()
        val city = cityEditText.text.toString()
        if (city.isNotEmpty()) {
            val sharedPref = getPreferences(Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("lastSearchedCity", city)
                apply()
            }
        }
    }


    private fun getLastSearchedCity(): String {
        return sharedPref.getString("LAST_SEARCHED_CITY", "") ?: ""
    }

    private fun fetchWeatherUsingDeviceLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                Log.d("LOCATION_DEBUG", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
                viewModel.fetchWeatherDevice(location.latitude, location.longitude)
            } ?: run {
                Log.d("LOCATION_DEBUG", "Location is null, requesting new location data.")
                requestNewLocationData()
            }
        }
    }

    // Handle the permissions result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchWeatherUsingDeviceLocation()
            } else {
                // Permission was denied, handle accordingly.
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    //
    private fun requestNewLocationData() {
        val locationRequest = LocationRequest().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 0
            fastestInterval = 0
            numUpdates = 1
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult?) {
                        locationResult?.let {
                            val lastLocation = it.lastLocation
                            viewModel.fetchWeatherDevice(lastLocation.latitude, lastLocation.longitude)
                        }
                    }
                },
                Looper.myLooper()
            )
        }
    }

}
