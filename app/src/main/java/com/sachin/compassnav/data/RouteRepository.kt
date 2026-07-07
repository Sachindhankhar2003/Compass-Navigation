package com.sachin.compassnav.data

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class RouteResult(
    val points: List<LatLng>,
    val distanceKm: Double,
    val durationMinutes: Int
)

object RouteRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Fetches a real road route from OSRM public API.
     * Uses GeoJSON geometry to decode precise polyline.
     */
    suspend fun fetchOsrmRoute(
        originLat: Double, originLon: Double,
        destLat: Double, destLon: Double
    ): RouteResult? = withContext(Dispatchers.IO) {
        try {
            val url = "https://router.project-osrm.org/route/v1/driving/" +
                    "$originLon,$originLat;$destLon,$destLat" +
                    "?overview=full&geometries=geojson"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "CompassNav-Android/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val bodyStr = response.body?.string() ?: return@withContext null
            val json = JSONObject(bodyStr)

            val code = json.optString("code", "")
            if (code != "Ok") return@withContext null

            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) return@withContext null

            val route = routes.getJSONObject(0)
            val distanceMeters = route.optDouble("distance", 0.0)
            val durationSeconds = route.optDouble("duration", 0.0)

            val geometry = route.getJSONObject("geometry")
            val coords = geometry.getJSONArray("coordinates")

            val points = mutableListOf<LatLng>()
            for (i in 0 until coords.length()) {
                val coord = coords.getJSONArray(i)
                val lon = coord.getDouble(0)
                val lat = coord.getDouble(1)
                points.add(LatLng(lat, lon))
            }

            RouteResult(
                points = points,
                distanceKm = distanceMeters / 1000.0,
                durationMinutes = (durationSeconds / 60.0).roundToInt()
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Fetches a real road route from Google Directions API.
     * Decodes the encoded polyline string from the response.
     */
    suspend fun fetchGoogleDirectionsRoute(
        originLat: Double, originLon: Double,
        destLat: Double, destLon: Double,
        apiKey: String
    ): RouteResult? = withContext(Dispatchers.IO) {
        try {
            val url = "https://maps.googleapis.com/maps/api/directions/json" +
                    "?origin=$originLat,$originLon" +
                    "&destination=$destLat,$destLon" +
                    "&key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "CompassNav-Android/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val bodyStr = response.body?.string() ?: return@withContext null
            val json = JSONObject(bodyStr)

            val status = json.optString("status", "")
            if (status != "OK") return@withContext null

            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) return@withContext null

            val route = routes.getJSONObject(0)
            val legs = route.getJSONArray("legs")

            var totalDistanceMeters = 0L
            var totalDurationSeconds = 0L

            for (i in 0 until legs.length()) {
                val leg = legs.getJSONObject(i)
                totalDistanceMeters += leg.getJSONObject("distance").getLong("value")
                totalDurationSeconds += leg.getJSONObject("duration").getLong("value")
            }

            val overviewPolyline = route.getJSONObject("overview_polyline").getString("points")
            val points = decodePolyline(overviewPolyline)

            RouteResult(
                points = points,
                distanceKm = totalDistanceMeters / 1000.0,
                durationMinutes = (totalDurationSeconds / 60.0).roundToInt()
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes a Google Encoded Polyline string into a list of LatLng points.
     * Algorithm: https://developers.google.com/maps/documentation/utilities/polylinealgorithm
     */
    fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dLat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dLng

            poly.add(LatLng(lat / 1e5, lng / 1e5))
        }
        return poly
    }

    /**
     * Converts a list of LatLng to a JSON array string for injection into WebView JS.
     * Format: [[lat,lng],[lat,lng],...]
     */
    fun latLngListToJsonArray(points: List<LatLng>): String {
        val sb = StringBuilder("[")
        points.forEachIndexed { i, p ->
            if (i > 0) sb.append(",")
            sb.append("[${p.latitude},${p.longitude}]")
        }
        sb.append("]")
        return sb.toString()
    }
}
