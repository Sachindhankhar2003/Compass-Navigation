package com.sachin.compassnav.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class LocationHelper(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let {
                _location.value = it
            }
        }
    }

    private var nativeLocationListener: android.location.LocationListener? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        // 1. Try to get last known location immediately (fused)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && _location.value == null) {
                    _location.value = loc
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // 2. Request updates from Fused Location Client
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            // Fallback to native LocationManager if FusedLocationProvider fails
            startNativeLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startNativeLocationUpdates() {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            
            // Try to get last known location from system providers
            val lastGps = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            val lastNetwork = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            val bestLast = lastGps ?: lastNetwork
            if (bestLast != null && _location.value == null) {
                _location.value = bestLast
            }

            nativeLocationListener = object : android.location.LocationListener {
                override fun onLocationChanged(location: Location) {
                    _location.value = location
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            // Register for updates
            if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    android.location.LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    nativeLocationListener!!
                )
            }
            if (locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    android.location.LocationManager.NETWORK_PROVIDER,
                    1000L,
                    0f,
                    nativeLocationListener!!
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            nativeLocationListener?.let {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                locationManager.removeUpdates(it)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    companion object {
        suspend fun getCoordinatesFromAddress(context: Context, address: String): Location? {
            return withContext(Dispatchers.IO) {
                // 1. Try standard Android Geocoder
                try {
                    val geocoder = Geocoder(context)
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(address, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        return@withContext Location("geocoder_result").apply {
                            latitude = addr.latitude
                            longitude = addr.longitude
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

                // 2. Fallback to OpenStreetMap Nominatim API if Geocoder fails/returns nothing
                try {
                    val encodedAddress = java.net.URLEncoder.encode(address, "UTF-8")
                    val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedAddress&format=json&limit=1"
                    val url = java.net.URL(urlString)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "CompassNav-Android-App")
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    if (connection.responseCode == 200) {
                        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonArray = org.json.JSONArray(responseText)
                        if (jsonArray.length() > 0) {
                            val firstObj = jsonArray.getJSONObject(0)
                            val lat = firstObj.getDouble("lat")
                            val lon = firstObj.getDouble("lon")
                            return@withContext Location("nominatim_fallback").apply {
                                latitude = lat
                                longitude = lon
                            }
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

                null
            }
        }
    }
}
