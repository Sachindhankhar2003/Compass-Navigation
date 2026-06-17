package com.sachin.compassnav.ui.screens

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.sachin.compassnav.location.LocationHelper
import com.sachin.compassnav.ui.theme.CardBorder
import com.sachin.compassnav.ui.theme.DeepBlack
import com.sachin.compassnav.ui.theme.ElectricPurple
import com.sachin.compassnav.ui.theme.GlassDark
import com.sachin.compassnav.ui.theme.HotPink
import com.sachin.compassnav.ui.theme.NeonCyan
import com.sachin.compassnav.ui.theme.PureWhite
import com.sachin.compassnav.ui.theme.RadialSpaceGradient
import com.sachin.compassnav.ui.theme.SoftLavender
import kotlinx.coroutines.launch
import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

// Retro dark night style for the map
private const val DARK_MAP_STYLE_JSON = """
[
  {
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#121212"
      }
    ]
  },
  {
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#8f8f8f"
      }
    ]
  },
  {
    "elementType": "labels.text.stroke",
    "stylers": [
      {
        "color": "#121212"
      }
    ]
  },
  {
    "featureType": "administrative",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#333333"
      }
    ]
  },
  {
    "featureType": "poi",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#181818"
      }
    ]
  },
  {
    "featureType": "poi",
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#6e6e6e"
      }
    ]
  },
  {
    "featureType": "road",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#1f1f1f"
      }
    ]
  },
  {
    "featureType": "road",
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#808080"
      }
    ]
  },
  {
    "featureType": "transit",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#181818"
      }
    ]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#080c14"
      }
    ]
  }
]
"""

@Composable
fun MapScreen(
    prefilledAddress: String,
    onStartCompassNavigation: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val locationHelper = remember { LocationHelper(context) }

    DisposableEffect(Unit) {
        locationHelper.startLocationUpdates()
        onDispose {
            locationHelper.stopLocationUpdates()
        }
    }

    val currentLocation by locationHelper.location.collectAsState()

    // Map & Search state
    var mapSource by remember { mutableStateOf(MapSource.OPEN_STREET_MAP) }
    var searchAddress by remember { mutableStateOf(prefilledAddress) }
    var resolvedAddressName by remember { mutableStateOf(prefilledAddress) }
    var destinationLocation by remember { mutableStateOf<Location?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState()
    var hasCenteredOnUser by remember { mutableStateOf(false) }

    // Geocoding function logic
    fun triggerGeocodeSearch(address: String) {
        if (address.isNotBlank()) {
            coroutineScope.launch {
                isSearching = true
                val loc = LocationHelper.getCoordinatesFromAddress(context, address)
                if (loc != null) {
                    destinationLocation = loc
                    resolvedAddressName = address
                    if (mapSource == MapSource.GOOGLE_MAPS) {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(
                            LatLng(loc.latitude, loc.longitude),
                            14f
                        )
                    }
                }
                isSearching = false
            }
        }
    }

    // Center camera on user's first GPS location match
    LaunchedEffect(currentLocation) {
        currentLocation?.let { curr ->
            if (!hasCenteredOnUser) {
                if (mapSource == MapSource.GOOGLE_MAPS) {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                        LatLng(curr.latitude, curr.longitude),
                        14f
                    )
                }
                hasCenteredOnUser = true
            }
        }
    }

    // Run search on pre-filled launcher query
    LaunchedEffect(prefilledAddress) {
        if (prefilledAddress.isNotBlank()) {
            triggerGeocodeSearch(prefilledAddress)
        }
    }

    // Dark Map and parameters
    val mapProperties = remember {
        MapProperties(
            isMyLocationEnabled = true,
            mapStyleOptions = MapStyleOptions(DARK_MAP_STYLE_JSON),
            minZoomPreference = 2f,
            maxZoomPreference = 20f
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            myLocationButtonEnabled = true,
            zoomControlsEnabled = false
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RadialSpaceGradient)
    ) {
        // 1. Map View
        if (mapSource == MapSource.GOOGLE_MAPS) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings
            ) {
                // Drop a Red Pin Marker at destination
                destinationLocation?.let { dest ->
                    Marker(
                        state = MarkerState(position = LatLng(dest.latitude, dest.longitude)),
                        title = resolvedAddressName,
                        snippet = "Destination"
                    )

                    // Draw Neon Cyan Polyline connecting current location and destination
                    currentLocation?.let { curr ->
                        Polyline(
                            points = listOf(
                                LatLng(curr.latitude, curr.longitude),
                                LatLng(dest.latitude, dest.longitude)
                            ),
                            color = NeonCyan,
                            width = 12f
                        )
                    }
                }
            }
        } else {
            LeafletMapView(
                userLocation = currentLocation,
                destinationLocation = destinationLocation,
                destinationName = resolvedAddressName,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Top Bar & Search Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GlassDark)
                        .border(1.dp, CardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PureWhite
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Search Address OutlinedTextField with neon cyan glow outline
                OutlinedTextField(
                    value = searchAddress,
                    onValueChange = { searchAddress = it },
                    modifier = Modifier
                        .weight(1f)
                        .shadow(4.dp, CircleShape, spotColor = NeonCyan, ambientColor = NeonCyan),
                    placeholder = {
                        Text(
                            text = "Search address...",
                            color = SoftLavender.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = HotPink,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = {
                                focusManager.clearFocus()
                                triggerGeocodeSearch(searchAddress)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = NeonCyan
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = DeepBlack.copy(alpha = 0.9f),
                        unfocusedContainerColor = DeepBlack.copy(alpha = 0.8f),
                        cursorColor = HotPink,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        triggerGeocodeSearch(searchAddress)
                    })
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Glassmorphic Map Source Toggle Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    MapSource.OPEN_STREET_MAP to "OpenStreetMap (Free)",
                    MapSource.GOOGLE_MAPS to "Google Maps (API Key)"
                ).forEach { (source, label) ->
                    val isSelected = mapSource == source
                    val backgroundBrush = if (isSelected) {
                        Brush.horizontalGradient(listOf(ElectricPurple, HotPink))
                    } else {
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                    Button(
                        onClick = { mapSource = source },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(backgroundBrush),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = PureWhite,
                            disabledContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) PureWhite else SoftLavender.copy(alpha = 0.8f)
                        )
                    }
                }
            }

        }

        // 3. Bottom Stats Panel & Navigation Button (with Purple -> Pink border gradient)
        destinationLocation?.let { dest ->
            var distanceInKm = 0f
            currentLocation?.let { curr ->
                val distanceInMeters = curr.distanceTo(dest)
                distanceInKm = distanceInMeters / 1000f
            }

            val estTimeMinutes = calculateEstTimeMinutes(distanceInKm)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(ElectricPurple, HotPink)),
                        RoundedCornerShape(20.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = DeepBlack.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = resolvedAddressName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "DISTANCE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (currentLocation == null) "Calculating..." else "${String.format(java.util.Locale.US, "%.2f", distanceInKm)} km",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "EST. DRIVE TIME",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = HotPink,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (currentLocation == null) "Calculating..." else formatDuration(estTimeMinutes),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { onStartCompassNavigation(resolvedAddressName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(25.dp),
                                clip = false,
                                ambientColor = ElectricPurple,
                                spotColor = HotPink
                            )
                            .background(Brush.horizontalGradient(listOf(ElectricPurple, HotPink))),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = "Start Compass Navigation",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }
            }
        }
    }
}

// Average driving speed of 40 km/h
private fun calculateEstTimeMinutes(distanceInKm: Float): Int {
    if (distanceInKm <= 0) return 0
    val timeHours = distanceInKm / 40f
    return (timeHours * 60f).roundToInt()
}

private fun formatDuration(totalMinutes: Int): String {
    return when {
        totalMinutes <= 0 -> "0 min"
        totalMinutes < 60 -> "$totalMinutes min"
        else -> {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (minutes == 0) "$hours hr" else "$hours hr $minutes min"
        }
    }
}

enum class MapSource {
    GOOGLE_MAPS,
    OPEN_STREET_MAP
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletMapView(
    userLocation: Location?,
    destinationLocation: Location?,
    destinationName: String,
    modifier: Modifier = Modifier
) {
    val htmlContent = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <style>
            html, body, #map {
              height: 100%;
              margin: 0;
              padding: 0;
              background: #080c14;
            }
            .leaflet-container {
              background: #080c14 !important;
            }
            .user-marker, .dest-marker {
              background: transparent;
              border: none;
            }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            var map = L.map('map', {
              zoomControl: false,
              attributionControl: false
            }).setView([20, 0], 2);

            L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
              maxZoom: 20
            }).addTo(map);

            var userMarker = null;
            var destMarker = null;
            var routeLine = null;

            var userIcon = L.divIcon({
              className: 'user-marker',
              html: '<div style="width: 14px; height: 14px; background: #00F0FF; border: 2.5px solid #ffffff; border-radius: 50%; box-shadow: 0 0 10px #00F0FF;"></div>',
              iconSize: [14, 14],
              iconAnchor: [7, 7]
            });

            var destIcon = L.divIcon({
              className: 'dest-marker',
              html: '<div style="width: 18px; height: 18px; background: #FF007F; border: 2.5px solid #ffffff; border-radius: 50%; box-shadow: 0 0 12px #FF007F;"></div>',
              iconSize: [18, 18],
              iconAnchor: [9, 9]
            });

            function updateLocations(userLat, userLng, destLat, destLng, destTitle) {
              var points = [];
              
              if (userLat !== null && userLng !== null) {
                var userPos = [userLat, userLng];
                points.push(userPos);
                if (!userMarker) {
                  userMarker = L.marker(userPos, {icon: userIcon}).addTo(map);
                } else {
                  userMarker.setLatLng(userPos);
                }
              } else if (userMarker) {
                map.removeLayer(userMarker);
                userMarker = null;
              }

              if (destLat !== null && destLng !== null) {
                var destPos = [destLat, destLng];
                points.push(destPos);
                if (!destMarker) {
                  destMarker = L.marker(destPos, {icon: destIcon}).addTo(map);
                  if (destTitle) {
                    destMarker.bindPopup('<b style="color: #121212;">' + destTitle + '</b>').openPopup();
                  }
                } else {
                  destMarker.setLatLng(destPos);
                  if (destTitle) {
                    destMarker.setPopupContent('<b style="color: #121212;">' + destTitle + '</b>');
                  }
                }
              } else if (destMarker) {
                map.removeLayer(destMarker);
                destMarker = null;
              }

              if (routeLine) {
                map.removeLayer(routeLine);
                routeLine = null;
              }

              if (points.length === 2) {
                routeLine = L.polyline(points, {
                  color: '#00F0FF',
                  weight: 4,
                  opacity: 0.8
                }).addTo(map);
                
                var bounds = L.latLngBounds(points);
                map.fitBounds(bounds, { padding: [50, 50] });
              } else if (points.length === 1) {
                map.setView(points[0], 14);
              }
            }
          </script>
        </body>
        </html>
    """.trimIndent()
    }

    var isPageLoaded by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        isPageLoaded = true
                    }
                }
                loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            if (isPageLoaded) {
                val userLat = userLocation?.latitude ?: "null"
                val userLng = userLocation?.longitude ?: "null"
                val destLat = destinationLocation?.latitude ?: "null"
                val destLng = destinationLocation?.longitude ?: "null"
                val destTitle = destinationName.replace("\"", "\\\"")
                webView.evaluateJavascript("updateLocations($userLat, $userLng, $destLat, $destLng, \"$destTitle\")", null)
            }
        }
    )
}
