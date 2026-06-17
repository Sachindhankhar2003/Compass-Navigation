package com.sachin.compassnav

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sachin.compassnav.ui.screens.CompassScreen
import com.sachin.compassnav.ui.screens.HomeScreen
import com.sachin.compassnav.ui.screens.MapScreen
import com.sachin.compassnav.ui.theme.CardBorder
import com.sachin.compassnav.ui.theme.CompassNavTheme
import com.sachin.compassnav.ui.theme.ElectricPurple
import com.sachin.compassnav.ui.theme.GlassDark
import com.sachin.compassnav.ui.theme.HotPink
import com.sachin.compassnav.ui.theme.NeonCyan
import com.sachin.compassnav.ui.theme.PureWhite
import com.sachin.compassnav.ui.theme.PurplePinkGradient
import com.sachin.compassnav.ui.theme.RadialSpaceGradient
import com.sachin.compassnav.ui.theme.SoftLavender

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompassNavTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocationPermissionWrapper {
                        AppNavigation()
                    }
                }
            }
        }
    }
}

@Composable
fun LocationPermissionWrapper(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    // Proactively check permission on start
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    if (hasPermission) {
        content()
    } else {
        // Space-themed screen asking for permission with electric purple and magenta colors
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RadialSpaceGradient)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = GlassDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Glowing Globe/Satellite Emoji
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0x22112244))
                            .border(1.5.dp, NeonCyan, CircleShape)
                    ) {
                        Text(text = "🛰️", fontSize = 48.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Location Access Needed",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "CompassNav requires fine GPS coordinates to accurately calculate the real-time distance and bearing to your destinations.",
                        fontSize = 14.sp,
                        color = SoftLavender,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
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
                            text = "Grant Permission",
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

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToMap = { address ->
                    navController.navigate("map/${address}")
                }
            )
        }

        composable(
            route = "map/{address}",
            arguments = listOf(
                navArgument("address") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: ""
            MapScreen(
                prefilledAddress = address,
                onStartCompassNavigation = { destAddress ->
                    navController.navigate("compass/${destAddress}")
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "compass/{address}",
            arguments = listOf(
                navArgument("address") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: ""
            CompassScreen(
                destinationAddress = address,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
