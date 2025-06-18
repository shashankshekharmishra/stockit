package com.example.stockit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.stockit.ui.screens.splash.SplashScreen
import com.example.stockit.ui.screens.onboarding.OnboardingScreen
import com.example.stockit.ui.screens.auth.SignInScreen
import com.example.stockit.ui.screens.auth.SignUpScreen
import com.example.stockit.ui.screens.auth.SignInSuccessScreen
import com.example.stockit.ui.screens.home.HomeScreen
import com.example.stockit.ui.screens.stock.StockDetailScreen
import com.example.stockit.ui.screens.profile.ProfileScreen
import com.example.stockit.ui.screens.watchlist.WatchlistScreen
import com.example.stockit.ui.components.MainScreenWrapper

@Composable
fun Navigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Define which routes should show the floating nav bar
    val routesWithNavBar = setOf("home", "watchlist", "profile")
    val shouldShowNavBar = currentRoute in routesWithNavBar

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { 
            SplashScreen(
                onNavigateToOnboarding = { 
                    navController.navigate("onboarding") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToHome = { 
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            ) 
        }
        composable("onboarding") { 
            OnboardingScreen(
                onSkip = { 
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onSignIn = { navController.navigate("sign_in") },
                onSignUp = { navController.navigate("sign_up") }
            ) 
        }
        composable("sign_in") { 
            SignInScreen(
                onSignIn = { navController.navigate("sign_in_success") },
                onNavigateToSignUp = { navController.navigate("sign_up") }
            ) 
        }
        composable("sign_up") { 
            SignUpScreen(
                onSignUp = { navController.navigate("sign_in_success") },
                onNavigateToSignIn = { navController.navigate("sign_in") }
            ) 
        }
        composable("sign_in_success") { 
            SignInSuccessScreen(
                onContinue = { 
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            ) 
        }
        composable("home") { 
            if (shouldShowNavBar) {
                MainScreenWrapper(
                    currentRoute = "home",
                    onNavigate = { route ->
                        if (route != "home") {
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                ) {
                    HomeScreen(
                        onStockClick = { stockSymbol -> 
                            navController.navigate("stock_detail/$stockSymbol")
                        },
                        onNavigateToOnboarding = {
                            navController.navigate("onboarding")
                        }
                    )
                }
            } else {
                HomeScreen(
                    onStockClick = { stockSymbol -> 
                        navController.navigate("stock_detail/$stockSymbol")
                    },
                    onNavigateToOnboarding = {
                        navController.navigate("onboarding")
                    }
                )
            }
        }
        composable("watchlist") { 
            if (shouldShowNavBar) {
                MainScreenWrapper(
                    currentRoute = "watchlist",
                    onNavigate = { route ->
                        if (route != "watchlist") {
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                ) {
                    WatchlistScreen(
                        onStockClick = { stockSymbol -> 
                            navController.navigate("stock_detail/$stockSymbol")
                        }
                    )
                }
            } else {
                WatchlistScreen(
                    onStockClick = { stockSymbol -> 
                        navController.navigate("stock_detail/$stockSymbol")
                    }
                )
            }
        }
        composable(
            "stock_detail/{stockSymbol}",
            arguments = listOf(navArgument("stockSymbol") { type = NavType.StringType })
        ) { backStackEntry ->
            val stockSymbol = backStackEntry.arguments?.getString("stockSymbol") ?: ""
            StockDetailScreen(
                stockSymbol = stockSymbol,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("profile") { 
            if (shouldShowNavBar) {
                MainScreenWrapper(
                    currentRoute = "profile",
                    onNavigate = { route ->
                        if (route != "profile") {
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                ) {
                    ProfileScreen(
                        onStockClick = { stockSymbol -> 
                            navController.navigate("stock_detail/$stockSymbol")
                        },
                        onLogout = {
                            navController.navigate("home") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            } else {
                ProfileScreen(
                    onStockClick = { stockSymbol -> 
                        navController.navigate("stock_detail/$stockSymbol")
                    },
                    onLogout = {
                        navController.navigate("home") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}