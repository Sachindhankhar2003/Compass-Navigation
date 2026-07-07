package com.sachin.compassnav.ui.screens

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import android.location.Location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.sachin.compassnav.location.LocationHelper
import com.sachin.compassnav.sensor.CompassSensor
import com.sachin.compassnav.ui.theme.BrightOrangeRed
import com.sachin.compassnav.ui.theme.CardBorder
import com.sachin.compassnav.ui.theme.DeepBlack
import com.sachin.compassnav.ui.theme.ElectricPurple
import com.sachin.compassnav.ui.theme.GlassDark
import com.sachin.compassnav.ui.theme.HotPink
import com.sachin.compassnav.ui.theme.NeonCyan
import com.sachin.compassnav.ui.theme.PureWhite
import com.sachin.compassnav.ui.theme.PurpleCyanGradient
import com.sachin.compassnav.ui.theme.PurplePinkGradient
import com.sachin.compassnav.ui.theme.RadialSpaceGradient
import com.sachin.compassnav.ui.theme.SoftLavender
import com.sachin.compassnav.utils.NavigationMath
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Dark map style JSON for mini-map (same as MapScreen)
private const val MINI_MAP_STYLE_JSON = """
[
  {"elementType":"geometry","stylers":[{"color":"#0D0D1A"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#6e6e9e"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#0D0D1A"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#1a1a3a"}]},
  {"featureType":"road.arterial","elementType":"geometry","stylers":[{"color":"#1e1e4a"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#060914"}]},
  {"featureType":"poi","stylers":[{"visibility":"off"}]},
  {"featureType":"transit","stylers":[{"visibility":"off"}]}
]
"""

@Composable
fun CompassScreen(
    destinationAddress: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    val compassSensor = remember { CompassSensor(context) }
    val locationHelper = remember { LocationHelper(context) }

    DisposableEffect(Unit) {
        compassSensor.start()
        locationHelper.startLocationUpdates()
        onDispose {
            compassSensor.stop()
            locationHelper.stopLocationUpdates()
        }
    }

    var destinationLocation by remember { mutableStateOf<Location?>(null) }
    var isGeocodingError by remember { mutableStateOf(false) }
    var isGeocodingLoading by remember { mutableStateOf(true) }

    LaunchedEffect(destinationAddress) {
        isGeocodingLoading = true
        isGeocodingError = false
        val loc = LocationHelper.getCoordinatesFromAddress(context, destinationAddress)
        if (loc != null) {
            destinationLocation = loc
        } else {
            isGeocodingError = true
        }
        isGeocodingLoading = false
    }

    val currentHeading by compassSensor.heading.collectAsState()
    val currentLocation by locationHelper.location.collectAsState()

    var bearingToDestination = 0f
    var distanceInKm = 0f
    var hasValidGps = false

    currentLocation?.let { curr ->
        hasValidGps = true
        destinationLocation?.let { dest ->
            bearingToDestination = curr.bearingTo(dest)
            val distanceInMeters = curr.distanceTo(dest)
            distanceInKm = distanceInMeters / 1000f
        }
    }

    val targetNeedleAngle = bearingToDestination - currentHeading

    var needleAngleAccumulated by remember { mutableStateOf(0f) }
    LaunchedEffect(targetNeedleAngle) {
        needleAngleAccumulated = NavigationMath.getShortestAngleTarget(needleAngleAccumulated, targetNeedleAngle)
    }

    val animatedNeedleAngle by animateFloatAsState(
        targetValue = needleAngleAccumulated,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "needleRotation"
    )

    // Ambient dial rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val ambientDialRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambientRotation"
    )

    // Pulsing glow on compass ring
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // Colorful confetti generator on arrival screen
    val confetti = remember {
        List(45) {
            Triple(
                (0..1000).random() / 1000f, // X fraction
                (0..1000).random() / 1000f, // Y fraction
                listOf(ElectricPurple, HotPink, NeonCyan, Color.Yellow, Color.Green).random()
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RadialSpaceGradient)
    ) {
        // Neon color glow behind the compass
        Box(
            modifier = Modifier
                .size(420.dp)
                .align(Alignment.Center)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ElectricPurple.copy(alpha = 0.18f * glowPulse),
                            NeonCyan.copy(alpha = 0.07f * glowPulse),
                            Color.Transparent
                        )
                    )
                )
                .blur(30.dp)
        )

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PureWhite
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Compass Navigation",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
        }

        // Main Column Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP DESTINATION CARD with live mini-map background ---
            DestinationMiniMapCard(
                destinationAddress = destinationAddress,
                currentLocation = currentLocation,
                destinationLocation = destinationLocation
            )

            // Middle section: Compass Dial or status screens
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isGeocodingLoading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ElectricPurple)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Locating target coordinates...",
                                color = SoftLavender,
                                fontSize = 14.sp
                            )
                        }
                    }
                    isGeocodingError -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "🧭❌",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Could not resolve address location.",
                                color = HotPink,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Check internet connectivity and try again.",
                                color = SoftLavender,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    else -> {
                        CompassDialView(
                            heading = currentHeading,
                            needleAngle = animatedNeedleAngle,
                            ambientRotation = ambientDialRotation,
                            modifier = Modifier
                                .size(280.dp)
                                .shadow(
                                    elevation = 30.dp,
                                    shape = CircleShape,
                                    clip = false,
                                    ambientColor = ElectricPurple,
                                    spotColor = NeonCyan
                                )
                                .clip(CircleShape)
                                .border(1.5.dp, CardBorder, CircleShape)
                                .background(Color(0xFF06060C))
                        )
                    }
                }
            }

            // Turn-by-Turn Card with animated entrance
            AnimatedVisibility(
                visible = !isGeocodingLoading && !isGeocodingError && hasValidGps,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
            ) {
                if (!isGeocodingLoading && !isGeocodingError && hasValidGps) {
                    val diff = NavigationMath.normalizeAngle(bearingToDestination - currentHeading)
                    val (arrow, instruction) = when {
                        kotlin.math.abs(diff) < 20f -> Pair("↑", "Go Straight")
                        diff in 20f..160f -> Pair("↗", "Turn Right")
                        diff in -160f..-20f -> Pair("↙", "Turn Left")
                        else -> Pair("↓", "Turn Around")
                    }
                    val targetCardinal = NavigationMath.getCardinalDirection(bearingToDestination)
                    val distanceStr = formatDistance(distanceInKm)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = GlassDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .fillMaxHeight()
                                    .background(PurplePinkGradient)
                            )
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = arrow,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = instruction,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                    Text(
                                        text = "$targetCardinal · $distanceStr",
                                        fontSize = 14.sp,
                                        color = SoftLavender
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Metrics Panel
            if (!isGeocodingLoading && !isGeocodingError) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = ElectricPurple.copy(alpha = 0.3f),
                            spotColor = NeonCyan.copy(alpha = 0.2f)
                        ),
                    colors = CardDefaults.cardColors(containerColor = GlassDark),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "HEADING",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HotPink,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${NavigationMath.getCardinalDirection(currentHeading)} ${currentHeading.roundToInt()}°",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .width(1.dp)
                                .background(PureWhite.copy(alpha = 0.15f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "DISTANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (!hasValidGps) "Waiting GPS..." else formatDistance(distanceInKm),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                style = LocalTextStyle.current.copy(brush = PurpleCyanGradient),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Arrival Congrats Overlay Card
        val isArrived = hasValidGps && (distanceInKm * 1000f < 50f)
        if (isArrived) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepBlack.copy(alpha = 0.95f))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    confetti.forEach { conf ->
                        drawRect(
                            color = conf.third,
                            topLeft = Offset(size.width * conf.first, size.height * conf.second),
                            size = Size(8.dp.toPx(), 8.dp.toPx())
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, CardBorder, RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = GlassDark),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🎉", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "You have arrived!",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                style = TextStyle(brush = PurplePinkGradient),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You are within 50m of your destination.",
                                fontSize = 15.sp,
                                color = SoftLavender,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(25.dp))
                                    .background(PurplePinkGradient),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent
                                )
                            ) {
                                Text(
                                    text = "Back to Home",
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
    }
}

/**
 * Destination card with live mini-map in lite mode as the background,
 * a frosted semi-transparent overlay, and the destination text on top.
 */
@Composable
fun DestinationMiniMapCard(
    destinationAddress: String,
    currentLocation: Location?,
    destinationLocation: Location?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            // Mini-map background (Google Maps lite mode)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                LiveMiniMapView(
                    currentLocation = currentLocation,
                    destinationLocation = destinationLocation,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Frosted glass overlay gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A0814).copy(alpha = 0.7f),
                                Color(0xFF1A0833).copy(alpha = 0.5f),
                                Color(0xFF0A0814).copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // Content on top of the overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "DESTINATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HotPink,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = destinationAddress,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (currentLocation != null) {
                    Text(
                        text = "📍 Live tracking active",
                        fontSize = 11.sp,
                        color = NeonCyan.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Google Maps lite mode view showing live user location (and optionally destination).
 * Updates camera automatically when currentLocation changes.
 */
@Composable
fun LiveMiniMapView(
    currentLocation: Location?,
    destinationLocation: Location?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var googleMapRef by remember { mutableStateOf<GoogleMap?>(null) }

    AndroidView(
        modifier = modifier,
        factory = {
            MapView(context, GoogleMapOptions().liteMode(true)).apply {
                onCreate(null)
                onResume()
                getMapAsync { gMap ->
                    googleMapRef = gMap
                    gMap.uiSettings.isScrollGesturesEnabled = false
                    gMap.uiSettings.isZoomGesturesEnabled = false
                    gMap.uiSettings.isRotateGesturesEnabled = false
                    gMap.uiSettings.isTiltGesturesEnabled = false
                    gMap.uiSettings.isMyLocationButtonEnabled = false
                    gMap.uiSettings.isMapToolbarEnabled = false
                    try {
                        gMap.setMapStyle(MapStyleOptions(MINI_MAP_STYLE_JSON))
                    } catch (e: Exception) { /* ignore style errors */ }

                    // Initial camera position
                    val loc = currentLocation ?: destinationLocation
                    loc?.let {
                        gMap.moveCamera(
                            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                LatLng(it.latitude, it.longitude), 13f
                            )
                        )
                    }
                }
            }
        },
        update = { mapView ->
            // Update camera to follow user's live location
            val loc = currentLocation ?: destinationLocation
            loc?.let { location ->
                googleMapRef?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude), 13f
                    )
                )
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            // MapView lifecycle handled implicitly by AndroidView
        }
    }
}

@Composable
fun CompassDialView(
    heading: Float,
    needleAngle: Float,
    ambientRotation: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f

        // Draw ambient rotating dial ring with sweep gradient stroke
        rotate(degrees = ambientRotation, pivot = center) {
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(ElectricPurple, NeonCyan, HotPink, ElectricPurple)
                ),
                radius = radius - 15.dp.toPx(),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Draw cardinal ticks — rotate with heading (so they move as the phone moves)
        rotate(degrees = -heading, pivot = center) {
            for (angle in 0 until 360 step 30) {
                val tickLength = if (angle % 90 == 0) 12.dp.toPx() else 6.dp.toPx()
                val tickColor = if (angle % 90 == 0) ElectricPurple else SoftLavender.copy(alpha = 0.4f)
                val strokeWidth = if (angle % 90 == 0) 2.5.dp.toPx() else 1.dp.toPx()

                rotate(degrees = angle.toFloat(), pivot = center) {
                    drawLine(
                        color = tickColor,
                        start = Offset(center.x, 18.dp.toPx()),
                        end = Offset(center.x, 18.dp.toPx() + tickLength),
                        strokeWidth = strokeWidth
                    )
                }
            }
        }

        // Static alignment cursor (Neon Cyan) at top — does NOT rotate
        drawLine(
            color = NeonCyan,
            start = Offset(center.x, 2.dp.toPx()),
            end = Offset(center.x, 14.dp.toPx()),
            strokeWidth = 3.5.dp.toPx()
        )

        // --- FEATURE #1: Fixed cardinal direction labels (do NOT rotate) ---
        val labelRadius = radius - 30.dp.toPx()
        val cardinalAngles = mapOf(
            "N" to 0.0,
            "NE" to 45.0,
            "E" to 90.0,
            "SE" to 135.0,
            "S" to 180.0,
            "SW" to 225.0,
            "W" to 270.0,
            "NW" to 315.0
        )

        drawIntoCanvas { canvas ->
            cardinalAngles.forEach { (label, angleDeg) ->
                val angleRad = Math.toRadians(angleDeg - 90.0)
                val x = center.x + (labelRadius * cos(angleRad)).toFloat()
                val y = center.y + (labelRadius * sin(angleRad)).toFloat()

                val isCardinal = label.length == 1
                val isNorth = label == "N"

                val textPaint = AndroidPaint().apply {
                    isAntiAlias = true
                    textAlign = AndroidPaint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = if (isCardinal) 14.dp.toPx() else 10.dp.toPx()
                    color = when {
                        isNorth -> BrightOrangeRed.copy(alpha = 1.0f).toArgb()
                        isCardinal -> PureWhite.copy(alpha = 0.95f).toArgb()
                        else -> SoftLavender.copy(alpha = 0.75f).toArgb()
                    }
                    setShadowLayer(4.dp.toPx(), 0f, 0f, when {
                        isNorth -> BrightOrangeRed.copy(alpha = 0.6f).toArgb()
                        isCardinal -> ElectricPurple.copy(alpha = 0.5f).toArgb()
                        else -> 0x00000000
                    })
                }

                // Vertically center the text
                val textHeight = textPaint.descent() - textPaint.ascent()
                val textOffset = textHeight / 2 - textPaint.descent()

                canvas.nativeCanvas.drawText(label, x, y + textOffset, textPaint)
            }
        }

        // Draw rotating needle
        rotate(degrees = needleAngle, pivot = center) {
            val needleWidth = 14.dp.toPx()
            val needleLength = radius - 35.dp.toPx()

            val orangeNeedlePath = Path().apply {
                moveTo(center.x, center.y - needleLength)
                lineTo(center.x - needleWidth, center.y)
                lineTo(center.x, center.y - 4.dp.toPx())
                close()
            }
            drawPath(path = orangeNeedlePath, color = BrightOrangeRed)

            val shadowOrangeNeedlePath = Path().apply {
                moveTo(center.x, center.y - needleLength)
                lineTo(center.x + needleWidth, center.y)
                lineTo(center.x, center.y - 4.dp.toPx())
                close()
            }
            drawPath(path = shadowOrangeNeedlePath, color = BrightOrangeRed.copy(alpha = 0.8f))

            val whiteNeedlePath = Path().apply {
                moveTo(center.x, center.y + needleLength * 0.6f)
                lineTo(center.x - needleWidth * 0.8f, center.y)
                lineTo(center.x, center.y - 4.dp.toPx())
                close()
            }
            drawPath(path = whiteNeedlePath, color = PureWhite)

            val shadowWhiteNeedlePath = Path().apply {
                moveTo(center.x, center.y + needleLength * 0.6f)
                lineTo(center.x + needleWidth * 0.8f, center.y)
                lineTo(center.x, center.y - 4.dp.toPx())
                close()
            }
            drawPath(path = shadowWhiteNeedlePath, color = PureWhite.copy(alpha = 0.8f))
        }

        // Center Pivot Cap
        drawCircle(color = DeepBlack, radius = 10.dp.toPx())
        drawCircle(
            brush = Brush.radialGradient(listOf(NeonCyan, Color.Transparent)),
            radius = 6.dp.toPx()
        )
        drawCircle(color = PureWhite, radius = 2.dp.toPx())
    }
}

// Helper: Format distance
private fun formatDistance(distance: Float): String {
    return if (distance < 1f) {
        "${(distance * 1000f).roundToInt()} m"
    } else {
        String.format(java.util.Locale.US, "%.2f km", distance)
    }
}
