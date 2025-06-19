package com.example.stockit.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.stockit.R
import com.example.stockit.ui.theme.StockItTheme
import com.example.stockit.utils.AuthManager
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    authManager: AuthManager = hiltViewModel<SplashViewModel>().authManager
) {
    // Get density for dp to px conversion
    val density = LocalDensity.current
    
    // Animation states
    var startAnimation by remember { mutableStateOf(false) }
    
    // Initialize infinite transition
    val infiniteTransition = rememberInfiniteTransition(label = "splash_animations")
    
    // Enhanced animation values
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 200
        ),
        label = "alpha"
    )
    
    val logoRotation by animateFloatAsState(
        targetValue = if (startAnimation) 0f else -180f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "logoRotation"
    )
    
    // Background animation similar to watchlist
    val backgroundPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "background_pulse"
    )
    
    // Loading dots animation
    val loadingProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_progress"
    )
    
    // Start animation and navigation with auth check
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(3000) // Show splash for 3 seconds
        
        // Check if user is logged in and navigate accordingly
        if (authManager.hasValidToken()) {
            onNavigateToHome()
        } else {
            onNavigateToOnboarding()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F172A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background decoration similar to watchlist screen
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 3
            
            drawCircle(
                color = Color.White.copy(alpha = 0.02f * backgroundPulse),
                radius = size.width * 0.6f,
                center = Offset(centerX - size.width * 0.3f, centerY - with(density) { 100.dp.toPx() })
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.015f * backgroundPulse),
                radius = size.width * 0.8f,
                center = Offset(centerX + size.width * 0.2f, centerY + with(density) { 200.dp.toPx() })
            )
        }

        // Floating particles effect
        repeat(8) { index ->
            Box(
                modifier = Modifier
                    .offset(
                        x = (80 * (index - 4)).dp,
                        y = (-150 + 40 * index).dp
                    )
                    .size(6.dp)
                    .background(
                        Color(0xFF6366F1).copy(
                            alpha = 0.1f * sin(loadingProgress * 3.14f + index).coerceAtLeast(0f)
                        ),
                        CircleShape
                    )
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        ) {
            // Logo container with glass morphism effect similar to watchlist cards
            Box(
                modifier = Modifier
                    .size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Brand logo from drawable
                Image(
                    painter = painterResource(id = R.drawable.icon),
                    contentDescription = "StockIt Logo",
                    modifier = Modifier
                        .size(70.dp)
                        .rotate(logoRotation)
                        .scale(
                            animateFloatAsState(
                                targetValue = if (startAnimation) 1f else 0.3f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "icon_scale"
                            ).value
                        )
                        .graphicsLayer(
                            shadowElevation = with(density) { 8.dp.toPx() },
                            ambientShadowColor = Color(0xFF6366F1),
                            spotShadowColor = Color(0xFF8B5CF6)
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // App name with consistent styling
            Text(
                text = "StockIt",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = (-1.5).sp,
                modifier = Modifier
                    .alpha(
                        animateFloatAsState(
                            targetValue = if (startAnimation) 1f else 0f,
                            animationSpec = tween(
                                durationMillis = 1000,
                                delayMillis = 600
                            ),
                            label = "title_alpha"
                        ).value
                    )
                    .scale(
                        animateFloatAsState(
                            targetValue = if (startAnimation) 1f else 0.8f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            ),
                            label = "title_scale"
                        ).value
                    )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tagline with consistent color scheme
            Text(
                text = "Smart. Simple. Sophisticated.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8), // Same as watchlist secondary text
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .alpha(
                        animateFloatAsState(
                            targetValue = if (startAnimation) 0.9f else 0f,
                            animationSpec = tween(
                                durationMillis = 800,
                                delayMillis = 1200
                            ),
                            label = "tagline_alpha"
                        ).value
                    )
                    .offset(
                        y = animateFloatAsState(
                            targetValue = if (startAnimation) 0f else 20f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "tagline_offset"
                        ).value.dp
                    )
            )
            
            Spacer(modifier = Modifier.height(80.dp))
            
            // Loading indicator with consistent brand colors
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.alpha(
                    animateFloatAsState(
                        targetValue = if (startAnimation) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = 600,
                            delayMillis = 1800
                        ),
                        label = "loading_alpha"
                    ).value
                )
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                Color(0xFF6366F1).copy(
                                    alpha = 0.3f + 0.7f * sin(
                                        loadingProgress * 6.28f - index * 0.8f
                                    ).coerceAtLeast(0f)
                                ),
                                CircleShape
                            )
                            .scale(
                                0.8f + 0.4f * sin(
                                    loadingProgress * 6.28f - index * 0.8f
                                ).coerceAtLeast(0f)
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Loading text
            Text(
                text = "Initializing...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                modifier = Modifier.alpha(
                    animateFloatAsState(
                        targetValue = if (startAnimation) 0.8f else 0f,
                        animationSpec = tween(
                            durationMillis = 600,
                            delayMillis = 2000
                        ),
                        label = "loading_text_alpha"
                    ).value
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    StockItTheme {
        SplashScreen()
    }
}