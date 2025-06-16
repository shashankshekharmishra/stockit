package com.example.stockit.ui.screens.auth

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.stockit.ui.theme.StockItTheme
import com.example.stockit.utils.AuthManager
import com.example.stockit.network.ApiConfig
import com.example.stockit.network.SignUpRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUp: () -> Unit,
    onNavigateToSignIn: () -> Unit = {}
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var startAnimation by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "signup_animations")
    
    val backgroundPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "background_pulse"
    )
    
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .scale(contentScale)
                .alpha(contentAlpha),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Enhanced Title
            Text(
                text = "Create Account",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                letterSpacing = (-1).sp,
                lineHeight = 48.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Sign up to start tracking your investments",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                letterSpacing = 0.5.sp
            )
            
            Spacer(modifier = Modifier.height(40.dp))

            // Enhanced Form Fields
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Enhanced Full Name Field
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { 
                        Text(
                            "Full Name",
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF6366F1)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedLabelColor = Color(0xFF6366F1),
                        unfocusedLabelColor = Color(0xFF64748B)
                    )
                )

                // Enhanced Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { 
                        Text(
                            "Email",
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFF6366F1)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedLabelColor = Color(0xFF6366F1),
                        unfocusedLabelColor = Color(0xFF64748B)
                    )
                )

                // Enhanced Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { 
                        Text(
                            "Password",
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color(0xFF6366F1)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedLabelColor = Color(0xFF6366F1),
                        unfocusedLabelColor = Color(0xFF64748B)
                    )
                )

                // Enhanced Confirm Password Field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { 
                        Text(
                            "Confirm Password",
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                tint = Color(0xFF6366F1)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            focusManager.clearFocus()
                            if (fullName.isNotBlank() && email.isNotBlank() && 
                                password.isNotBlank() && confirmPassword.isNotBlank()) {
                                scope.launch {
                                    signUpUser(
                                        fullName = fullName,
                                        email = email,
                                        password = password,
                                        confirmPassword = confirmPassword,
                                        context = context,
                                        onLoading = { isLoading = it },
                                        onError = { errorMessage = it },
                                        onSuccess = onSignUp
                                    )
                                }
                            }
                        }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedLabelColor = Color(0xFF6366F1),
                        unfocusedLabelColor = Color(0xFF64748B)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enhanced Error Message
            AnimatedVisibility(
                visible = errorMessage.isNotEmpty(),
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color(0xFFEF4444).copy(alpha = 0.1f)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEF2F2)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFDC2626),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Enhanced Sign Up Button
            Button(
                onClick = {
                    errorMessage = ""
                    scope.launch {
                        signUpUser(
                            fullName = fullName,
                            email = email,
                            password = password,
                            confirmPassword = confirmPassword,
                            context = context,
                            onLoading = { isLoading = it },
                            onError = { errorMessage = it },
                            onSuccess = onSignUp
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color(0xFF6366F1).copy(alpha = 0.15f),
                        spotColor = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                    ),
                enabled = !isLoading && fullName.isNotBlank() && email.isNotBlank() && 
                         password.isNotBlank() && confirmPassword.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (isLoading || fullName.isBlank() || email.isBlank() || 
                                      password.isBlank() || confirmPassword.isBlank()) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFCBD5E1),
                                        Color(0xFFA1A1AA)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6366F1),
                                        Color(0xFF8B5CF6),
                                        Color(0xFFA855F7)
                                    )
                                )
                            },
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Sign Up",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enhanced Sign In Link
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    fontSize = 16.sp,
                    color = Color(0xFF64748B)
                )
                TextButton(
                    onClick = onNavigateToSignIn
                ) {
                    Text(
                        text = "Sign In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1)
                    )
                }
            }
        }
    }
}

// Updated signUpUser function using ApiConfig
suspend fun signUpUser(
    fullName: String,
    email: String,
    password: String,
    confirmPassword: String,
    context: Context,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {
    onLoading(true)
    
    try {
        val request = SignUpRequest(fullName, email, password, confirmPassword)
        val response = ApiConfig.authApiService.signUp(request)
        
        if (response.success) {
            val accessToken = response.access_token ?: ""
            val refreshToken = response.refresh_token ?: ""
            val userId = response.user_id ?: ""
            val userEmail = response.user_email ?: ""
            val userFullName = response.user_fullName ?: ""
            
            // Store tokens and user info in SharedPreferences
            val authManager = AuthManager(context)
            authManager.saveUserData(accessToken, refreshToken, userId, userEmail, userFullName)
            
            onSuccess()
        } else {
            onError(response.message ?: "Sign up failed")
        }
        
    } catch (e: HttpException) {
        val errorMessage = when (e.code()) {
            400 -> "Invalid input. Please check your information and try again."
            409 -> "An account with this email already exists. Please sign in instead."
            422 -> "Password must be at least 8 characters long and contain uppercase, lowercase, number, and special character."
            429 -> "Too many attempts. Please try again later."
            500 -> "Server error. Please try again later."
            else -> "Sign up failed. Please try again."
        }
        onError(errorMessage)
    } catch (e: Exception) {
        onError("Network error: ${e.message}")
    } finally {
        onLoading(false)
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    StockItTheme {
        SignUpScreen(
            onSignUp = {},
            onNavigateToSignIn = {}
        )
    }
}