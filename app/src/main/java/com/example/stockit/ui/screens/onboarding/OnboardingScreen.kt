package com.example.stockit.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontFamily
import com.example.stockit.R
import com.example.stockit.ui.theme.StockItTheme
import kotlinx.coroutines.delay

data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val motifResource: Int
)

@Composable
fun OnboardingScreen(
    onSkip: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onSignUp: () -> Unit = {}
) {
    // Define onboarding steps
    val onboardingSteps = listOf(
        OnboardingStep(
            title = "Stock trading suit",
            subtitle = "Streamline your investment decisions\nwith expert guidance.",
            motifResource = R.drawable.onboarding_motif
        ),
        OnboardingStep(
            title = "Smart Analytics",
            subtitle = "Get real-time insights and\ndata-driven recommendations.",
            motifResource = R.drawable.onboarding_motif_2 // Make sure to add this drawable
        )
    )
    
    // Current step state
    var currentStep by remember { mutableStateOf(0) }
    var startAnimation by remember { mutableStateOf(false) }
    
    // Auto-transition between steps
    LaunchedEffect(currentStep) {
        delay(4000) // Show each step for 4 seconds
        currentStep = (currentStep + 1) % onboardingSteps.size
    }
    
    // Initialize infinite transition for background effects
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_animations")
    
    // Background pulse animation
    val backgroundPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "background_pulse"
    )
    
    // Content animations with step transition
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = 100
        ),
        label = "content_alpha"
    )
    
    val contentScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "content_scale"
    )
    
    // Image transition animation
    val imageAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 500,
            easing = EaseInOutCubic
        ),
        label = "image_alpha"
    )
    
    val imageScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "image_scale"
    )
    
    // Start animation
    LaunchedEffect(Unit) {
        startAnimation = true
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
            )
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
                        Color(0xFF6366F1).copy(alpha = 0.1f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
        
        // Skip button with enhanced styling
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .alpha(contentAlpha)
        ) {
            Text(
                text = "Skip",
                color = Color(0xFF6366F1),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .scale(contentScale)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            // Enhanced central motif image container with transition
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = onboardingSteps[currentStep].motifResource),
                    contentDescription = "Onboarding illustration step ${currentStep + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .alpha(imageAlpha)
                        .scale(imageScale),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Enhanced title with transition
            Text(
                text = onboardingSteps[currentStep].title,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Default,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                letterSpacing = (-1).sp,
                lineHeight = 48.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Enhanced subtitle with transition
            Text(
                text = onboardingSteps[currentStep].subtitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Enhanced page indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                repeat(onboardingSteps.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentStep) 24.dp else 8.dp)
                            .background(
                                color = if (index == currentStep) {
                                    Color(0xFF6366F1)
                                } else {
                                    Color(0xFF64748B).copy(alpha = 0.3f)
                                },
                                shape = RoundedCornerShape(if (index == currentStep) 12.dp else 4.dp)
                            )
                            .shadow(
                                elevation = if (index == currentStep) 4.dp else 0.dp,
                                shape = RoundedCornerShape(if (index == currentStep) 12.dp else 4.dp)
                            )
                    )
                }
            }

            // Enhanced buttons with gradient and shadow effects
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Enhanced Sign up button
                Button(
                    onClick = onSignUp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = Color(0xFF6366F1).copy(alpha = 0.4f),
                            spotColor = Color(0xFF8B5CF6).copy(alpha = 0.4f)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6366F1),
                                        Color(0xFF8B5CF6),
                                        Color(0xFFA855F7)
                                    )
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign up",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Enhanced Sign in button
                Button(
                    onClick = onSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = Color(0xFF6366F1).copy(alpha = 0.2f)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = "Sign in",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF6366F1)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    StockItTheme {
        OnboardingScreen()
    }
}