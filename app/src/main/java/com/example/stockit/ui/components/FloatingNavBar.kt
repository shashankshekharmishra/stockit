package com.example.stockit.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescription: String
)

@Composable
fun FloatingNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        NavItem(
            route = "home",
            label = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            contentDescription = "Navigate to Home screen"
        ),
        NavItem(
            route = "watchlist",
            label = "Watchlist",
            selectedIcon = Icons.Filled.Visibility,
            unselectedIcon = Icons.Outlined.Visibility,
            contentDescription = "Navigate to Watchlist screen"
        ),
        NavItem(
            route = "profile",
            label = "Profile",
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
            contentDescription = "Navigate to Profile screen"
        )
    )

    val haptic = LocalHapticFeedback.current

    // Animation for the entire navbar
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val navBarOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navbar_offset"
    )

    val navBarScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navbar_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .offset(y = navBarOffset.dp)
            .graphicsLayer(
                scaleX = navBarScale,
                scaleY = navBarScale
            )
            .semantics {
                contentDescription = "Main navigation bar with ${navItems.size} options"
            },
        contentAlignment = Alignment.Center
    ) {
        // Enhanced glass morphism background with constrained width
        Box(
            modifier = Modifier
                .wrapContentSize()
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = Color.Black.copy(alpha = 0.1f),
                    spotColor = Color.Black.copy(alpha = 0.2f)
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E293B).copy(alpha = 0.95f),
                            Color(0xFF334155).copy(alpha = 0.9f)
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .clip(RoundedCornerShape(32.dp))
        ) {
            // Subtle glass effect overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.02f)
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(500f, 500f)
                        )
                    )
            )

            // Navigation items
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    NavBarItem(
                        item = item,
                        isSelected = currentRoute == item.route,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigate(item.route)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NavBarItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Smooth animations for selection state
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "background_alpha"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "icon_scale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF94A3B8),
        animationSpec = tween(durationMillis = 200),
        label = "content_color"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .semantics {
                contentDescription = item.contentDescription
                if (isSelected) {
                    stateDescription = "Selected"
                }
                role = Role.Tab
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Selection background indicator
        Box(
            modifier = Modifier
                .size(40.dp) // Slightly smaller for better proportion
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = backgroundAlpha),
                            Color(0xFF8B5CF6).copy(alpha = backgroundAlpha),
                            Color(0xFFA855F7).copy(alpha = backgroundAlpha)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null, // Handled by parent semantics
                tint = contentColor,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer(
                        scaleX = iconScale,
                        scaleY = iconScale
                    )
            )
        }

        // Animated label - only show for selected item
        AnimatedVisibility(
            visible = isSelected,
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(
                animationSpec = tween(300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it / 2 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(
                animationSpec = tween(200)
            )
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}