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
                onNext = { navController.navigate("sign_in") }
            ) 
        }
        composable("sign_in") { 
            SignInScreen(
                onSignIn = { navController.navigate("home") }
            ) 
        }
        composable("sign_up") { 
            SignUpScreen(
                onSignUp = { navController.navigate("home") }
            ) 
        }
        composable("sign_in_success") { SignInSuccessScreen() }
        composable("home") { HomeScreen() }
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