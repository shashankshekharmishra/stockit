package com.example.stockit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.stockit.ui.screens.splash.SplashScreen
import com.example.stockit.ui.screens.onboarding.OnboardingScreen
import com.example.stockit.ui.screens.auth.SignInScreen
import com.example.stockit.ui.screens.auth.SignUpScreen
import com.example.stockit.ui.screens.auth.SignInSuccessScreen
import com.example.stockit.ui.screens.home.HomeScreen
import com.example.stockit.ui.screens.portfolio.PortfolioScreen
import com.example.stockit.ui.screens.stock.StockDetailScreen
import com.example.stockit.ui.screens.profile.ProfileScreen
import com.example.stockit.data.model.Stock
import com.example.stockit.utils.AuthManager

@Composable
fun Navigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { 
            SplashScreen(
                onNavigateToNext = { navController.navigate("onboarding") }
            ) 
        }
        composable("onboarding") { 
            OnboardingScreen(
                onSkip = { navController.navigate("home") },
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
                onContinue = { navController.navigate("home") }
            ) 
        }
        composable("home") { 
            HomeScreen(
                onSearchClick = { /* TODO: Navigate to search */ },
                onStockClick = { stockSymbol -> 
                    navController.navigate("stock_detail/$stockSymbol")
                }
            ) 
        }
        composable("portfolio") { PortfolioScreen() }
        composable("stock_detail/{stockId}") { backStackEntry ->
            val stockId = backStackEntry.arguments?.getString("stockId")
            if (stockId != null) {
                val defaultStock = Stock(
                    id = stockId,
                    name = "Loading...",
                    symbol = "...",
                    price = 0.0,
                    change = 0.0,
                    marketCap = 0L,
                    description = "Loading stock details..."
                )
                StockDetailScreen(stock = defaultStock)
            }
        }
        composable("profile") { ProfileScreen() }
    }
}