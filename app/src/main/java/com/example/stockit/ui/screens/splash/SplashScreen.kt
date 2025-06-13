package com.example.stockit.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import com.example.stockit.ui.theme.StockItTheme
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun SplashScreen(onNavigateToNext: () -> Unit = {}) {
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
        )
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 200
        )
    )
    
    val logoRotation by animateFloatAsState(
        targetValue = if (startAnimation) 0f else -180f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    // Pulsing animation for the gradient background
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
    
    // Start animation and navigation
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(3000) // Slightly longer to appreciate the animations
        onNavigateToNext()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC).copy(alpha = backgroundPulse),
                        Color(0xFFF1F5F9),
                        Color(0xFFE2E8F0),
                        Color(0xFFCBD5E1)
                    ),
                    radius = 1000f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle background particles effect
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .offset(
                        x = (50 * (index - 2)).dp,
                        y = (-100 + 40 * index).dp
                    )
                    .size(4.dp)
                    .background(
                        Color(0xFF6366F1).copy(
                            alpha = 0.1f * sin(loadingProgress * 3.14f + index).coerceAtLeast(0f)
                        ),
                        RoundedCornerShape(2.dp)
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
            // Enhanced logo container with floating effect
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(
                        elevation = 25.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color(0xFF6366F1).copy(alpha = 0.4f),
                        spotColor = Color(0xFF8B5CF6).copy(alpha = 0.4f)
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6),
                                Color(0xFFA855F7),
                                Color(0xFFEC4899)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = "StockIt Logo",
                    tint = Color.White,
                    modifier = Modifier
                        .size(50.dp)
                        .rotate(logoRotation)
                        .scale(
                            animateFloatAsState(
                                targetValue = if (startAnimation) 1f else 0.3f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ).value
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Enhanced app name with letter animation
            Text(
                text = "StockIt",
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Default,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                letterSpacing = (-2).sp,
                modifier = Modifier
                    .alpha(
                        animateFloatAsState(
                            targetValue = if (startAnimation) 1f else 0f,
                            animationSpec = tween(
                                durationMillis = 1000,
                                delayMillis = 600
                            )
                        ).value
                    )
                    .scale(
                        animateFloatAsState(
                            targetValue = if (startAnimation) 1f else 0.8f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            )
                        ).value
                    )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Enhanced tagline with slide-in effect
            Text(
                text = "Smart. Simple. Sophisticated.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .alpha(
                        animateFloatAsState(
                            targetValue = if (startAnimation) 0.9f else 0f,
                            animationSpec = tween(
                                durationMillis = 800,
                                delayMillis = 1200
                            )
                        ).value
                    )
                    .offset(
                        y = animateFloatAsState(
                            targetValue = if (startAnimation) 0f else 20f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        ).value.dp
                    )
            )
            
            Spacer(modifier = Modifier.height(80.dp))
            
            // Enhanced loading indicator with animated dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(
                    animateFloatAsState(
                        targetValue = if (startAnimation) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = 600,
                            delayMillis = 1800
                        )
                    ).value
                )
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color(0xFF6366F1).copy(
                                    alpha = 0.3f + 0.7f * sin(
                                        loadingProgress * 6.28f - index * 0.5f
                                    ).coerceAtLeast(0f)
                                ),
                                RoundedCornerShape(4.dp)
                            )
                            .scale(
                                1f + 0.3f * sin(
                                    loadingProgress * 6.28f - index * 0.5f
                                ).coerceAtLeast(0f)
                            )
                    )
                }
            }
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