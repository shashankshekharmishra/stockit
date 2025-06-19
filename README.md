# StockIt - Advanced Stock Trading Mobile Application

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)
![Min SDK](https://img.shields.io/badge/Min%20SDK-28-yellow.svg)
![Target SDK](https://img.shields.io/badge/Target%20SDK-35-green.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

</div>

A modern, feature-rich stock trading application built with Jetpack Compose, offering real-time market data, portfolio management, and seamless trading capabilities with a stunning dark-themed UI.

## 🌟 Features

### 📱 Modern UI/UX
- **Dark Theme Design**: Elegant dark mode interface with glassmorphism effects
- **Smooth Animations**: Spring-based animations and transitions throughout the app
- **Responsive Layout**: Adaptive design that works across different screen sizes
- **Accessibility First**: Full accessibility support with semantic descriptions and screen reader compatibility

### 📊 Real-Time Market Data
- **Live Stock Prices**: Real-time price updates with WebSocket connections
- **Interactive Charts**: Multiple timeframe support (1W, 1M, 3M, 6M, 1Y)
- **Trending Stocks**: Curated list of trending stocks with live market data
- **Advanced Analytics**: Technical indicators and market statistics

### 💼 Portfolio Management
- **Portfolio Overview**: Real-time portfolio value and P&L tracking
- **Holdings Management**: Detailed view of stock holdings with performance metrics
- **Transaction History**: Complete trading history with filtering options
- **Performance Analytics**: Profit/loss tracking with percentage calculations

### 📈 Trading Capabilities
- **Buy/Sell Orders**: Seamless stock trading with real-time affordability checks
- **Market Orders**: Instant execution at current market prices
- **Price Validation**: Real-time price verification before order placement
- **Order Confirmation**: Multi-step confirmation process for secure trading

### 👀 Watchlist Management
- **Personal Watchlist**: Add/remove stocks to track favorites
- **Price Alerts**: Monitor stock price movements
- **Quick Actions**: Easy access to stock details and trading from watchlist
- **Synchronized Data**: Cloud-based watchlist sync across devices

### 🔐 Authentication & Security
- **Secure Login/Signup**: JWT-based authentication with token management
- **Session Management**: Automatic token refresh and secure storage
- **Biometric Support**: Optional fingerprint/face unlock (planned)
- **Data Encryption**: End-to-end encryption for sensitive user data

## 🏗️ Architecture

This application follows **MVVM + Clean Architecture** principles for maintainable and testable code.

### Technology Stack

#### Frontend
- **Jetpack Compose**: Modern declarative UI toolkit
- **Material 3**: Latest Material Design components
- **Compose Navigation**: Type-safe navigation
- **Hilt**: Dependency injection framework
- **StateFlow/Flow**: Reactive programming

#### Networking
- **Retrofit**: Type-safe HTTP client
- **OkHttp**: HTTP/HTTP2 client with interceptors
- **Gson**: JSON serialization/deserialization
- **Coroutines**: Asynchronous programming

#### Data Management
- **SharedPreferences**: Local data storage
- **Repository Pattern**: Data layer abstraction
- **Flow**: Reactive data streams

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog | 2023.1.1 or newer
- Android SDK API level 28 or higher
- JDK 11 or higher
- Gradle 8.0+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/StockIt.git
   cd StockIt
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Configure API Endpoint**
   - Update API base URL in `app/src/main/java/com/yourpackage/utils/Constants.kt`

4. **Build and Run**
   - Sync project with Gradle files
   - Run on device or emulator (API 28+)

## 📱 Screenshots

### 🎯 Onboarding & Authentication
| Onboarding 1 | Onboarding 2 | Sign In | Sign Up |
|--------------|--------------|---------|---------|
| ![Onboarding 1](./screenshots/onboarding%201.jpg) | ![Onboarding 2](./screenshots/onboarding%202.jpg) | ![Sign In](./screenshots/sign%20in.jpg) | ![Sign Up](./screenshots/sign%20up.jpg) |

### 📊 Main Application
| Home Page | Stock Details 1 | Stock Details 2 | Buy Stocks |
|-----------|-----------------|-----------------|------------|
| ![Home Page](./screenshots/home%20page.jpg) | ![Stock Details 1](./screenshots/stock%20details%201.jpg) | ![Stock Details 2](./screenshots/stock%20details%202.jpg) | ![Buy Stocks](./screenshots/buy%20stocks.jpg) |

### 👤 Profile & Success
| Profile Page 0 | Profile Page | Success Page |
|----------------|--------------|--------------|
| ![Profile Page 0](./screenshots/profile%20page%200.jpg) | ![Profile Page](./screenshots/profile%20page.jpg) | ![Success Page](./screenshots/success%20page.jpg) |

## 🎨 Design System

### Color Palette
- **Primary**: Modern dark theme with high contrast
- **Accent**: Strategic use of colors for data visualization
- **Background**: Multi-layered gradients for depth

### Typography
- **Headlines**: Bold, high contrast for readability
- **Body Text**: Optimized for dark backgrounds
- **Accent Text**: Color-coded for different data types

### Visual Effects
- **Glassmorphism**: Translucent cards with blur effects
- **Gradient Backgrounds**: Multi-layer gradients for depth
- **Smooth Animations**: Spring-based transitions
- **Micro-interactions**: Subtle feedback for user actions

## 🔌 API Integration

The app integrates with a comprehensive trading API providing:

- **Authentication**: JWT-based secure login
- **Market Data**: Real-time stock prices and charts
- **Trading**: Buy/sell order execution
- **Portfolio**: Holdings and transaction management
- **Watchlist**: Personal stock tracking

## 🔒 Security Features

### Data Protection
- **Encrypted Storage**: Sensitive data encryption at rest
- **Secure Communication**: HTTPS/TLS for all API calls
- **Token Management**: Automatic token refresh and validation
- **Session Security**: Secure session handling

### Authentication Flow
1. User Registration/Login with secure credential validation
2. JWT Token issued by server
3. Encrypted local token storage
4. Automatic token renewal
5. Complete session cleanup on logout

## ♿ Accessibility Features

- **Screen Reader Support**: Complete VoiceOver/TalkBack compatibility
- **High Contrast**: WCAG compliant color schemes
- **Large Text Support**: Dynamic type scaling
- **Touch Targets**: Minimum 44dp touch areas
- **Semantic Labels**: Descriptive content descriptions

## 🎯 Performance Optimizations

### Memory Management
- Efficient Composables with optimized recomposition
- Smart bitmap caching and management
- Lifecycle-aware background operations
- Proactive memory leak prevention

### Network Optimization
- Intelligent request caching
- Automatic failure recovery with retry logic
- Efficient connection pooling
- Optimized data transfer with compression

## 🔧 Build Configuration

### Build Variants
- **Debug**: Full logging and debugging features
- **Staging**: Production-like testing environment
- **Release**: Optimized production build

## 🚀 Roadmap

### Upcoming Features
- 📊 Advanced technical analysis tools
- 🔔 Push notifications for price alerts
- 🌐 Multi-language support
- 📱 Optimized tablet layouts
- 🔐 Biometric authentication
- 📈 Paper trading mode
- 💬 Community features
- 🤖 AI-powered insights

### Technical Improvements
- Offline support with local caching
- WebSocket integration for real-time data
- Feature-based modular architecture
- Comprehensive testing coverage
- CI/CD pipeline implementation

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Standards
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use ktlint for code formatting
- Write comprehensive unit tests
- Document public APIs

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

<div align="center">

**StockIt** - Empowering the next generation of investors with cutting-edge mobile technology 📈📱

Built with ❤️ using Jetpack Compose

</div>