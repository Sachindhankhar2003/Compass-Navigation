package com.sachin.compassnav.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sachin.compassnav.ui.theme.ElectricPurple
import com.sachin.compassnav.ui.theme.GlassDark
import com.sachin.compassnav.ui.theme.HotPink
import com.sachin.compassnav.ui.theme.NeonCyan
import com.sachin.compassnav.ui.theme.PureWhite
import com.sachin.compassnav.ui.theme.RadialSpaceGradient
import com.sachin.compassnav.ui.theme.SoftLavender

@Composable
fun HomeScreen(
    onNavigateToMap: (String) -> Unit
) {
    var destinationAddress by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Pulsing glow animation for the compass logo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val logoRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logoRotation"
    )

    // Glowing ring animation alternating colors (Purple -> Pink -> Cyan)
    val colorIndex by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "colorCycle"
    )

    val ringColor = when {
        colorIndex < 1f -> lerpColor(ElectricPurple, HotPink, colorIndex)
        colorIndex < 2f -> lerpColor(HotPink, NeonCyan, colorIndex - 1f)
        else -> lerpColor(NeonCyan, ElectricPurple, colorIndex - 2f)
    }

    // Static random star dots on the background
    val stars = remember {
        List(25) {
            Triple(
                (0..1000).random() / 1000f, // X percent
                (0..1000).random() / 1000f, // Y percent
                (100..800).random() / 1000f // opacity percent
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RadialSpaceGradient)
    ) {
        // Starry sky background rendering
        Canvas(modifier = Modifier.fillMaxSize()) {
            stars.forEach { star ->
                drawCircle(
                    color = Color.White.copy(alpha = star.third),
                    radius = (1.5.dp + (1.dp * star.third)).toPx(),
                    center = Offset(size.width * star.first, size.height * star.second)
                )
            }
        }

        // Faded central color bubble behind logo
        Box(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.Center)
                .offset(y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ElectricPurple.copy(alpha = 0.15f),
                            HotPink.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .blur(40.dp)
        )

        // Main Layout Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Color-Cycling Compass Logo Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp * glowScale)
                    .shadow(
                        elevation = 24.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = ElectricPurple,
                        spotColor = HotPink
                    )
                    .clip(CircleShape)
                    .background(Color(0xFF0F081C).copy(alpha = 0.8f))
                    .border(2.5.dp, ringColor, CircleShape)
            ) {
                Text(
                    text = "🧭",
                    fontSize = 76.sp,
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = logoRotation
                        }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // App Gradient Heading (Electric Purple to Hot Pink)
            Text(
                text = "CompassNav",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(ElectricPurple, HotPink))
                ),
                letterSpacing = 1.5.sp
            )

            Text(
                text = "Navigate with celestial guidance",
                fontSize = 14.sp,
                color = SoftLavender,
                modifier = Modifier.padding(top = 6.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Neon Purple border with Glassmorphic Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElectricPurple.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = GlassDark),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Set Destination",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = destinationAddress,
                        onValueChange = { destinationAddress = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Enter address (e.g. Paris, France)",
                                color = SoftLavender.copy(alpha = 0.4f),
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricPurple,
                            unfocusedBorderColor = ElectricPurple.copy(alpha = 0.2f),
                            focusedContainerColor = Color(0x33000000),
                            unfocusedContainerColor = Color(0x11000000),
                            cursorColor = HotPink,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            if (destinationAddress.isNotBlank()) {
                                focusManager.clearFocus()
                                onNavigateToMap(destinationAddress)
                            }
                        })
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Start Navigation Button with Gradient & Glow shadow
            val isInputValid = destinationAddress.isNotBlank()
            val buttonGradient = if (isInputValid) {
                Brush.horizontalGradient(listOf(ElectricPurple, HotPink))
            } else {
                Brush.horizontalGradient(listOf(Color(0xFF2C2C2C), Color(0xFF1E1E1E)))
            }

            Button(
                onClick = {
                    if (isInputValid) {
                        focusManager.clearFocus()
                        onNavigateToMap(destinationAddress)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .shadow(
                        elevation = if (isInputValid) 16.dp else 0.dp,
                        shape = RoundedCornerShape(28.dp),
                        clip = false,
                        ambientColor = ElectricPurple,
                        spotColor = HotPink
                    )
                    .background(buttonGradient),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                enabled = isInputValid
            ) {
                Text(
                    text = "Find on Map",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isInputValid) PureWhite else PureWhite.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// Color interpolation helper
private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + fraction * (end.red - start.red),
        green = start.green + fraction * (end.green - start.green),
        blue = start.blue + fraction * (end.blue - start.blue),
        alpha = start.alpha + fraction * (end.alpha - start.alpha)
    )
}
