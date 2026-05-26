package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.example.data.ChatMessage
import com.example.data.EscrowTrade
import com.example.data.GameListing
import com.example.data.UserEntity
import com.example.ui.AuthState
import com.example.ui.SajiloViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainMarketplaceContainer(viewModel: SajiloViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val isDark by viewModel.currentThemeDark.collectAsState()

    MyApplicationTheme(darkTheme = isDark) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    bottomBar = {
                        if (currentUser != null && currentScreen != "login" && currentScreen != "splash") {
                            SajiloBottomNavigation(
                                currentScreen = currentScreen,
                                onSelectScreen = { viewModel.navigateTo(it) },
                                userTier = currentUser?.subscriptionTier ?: "Free",
                                userEmail = currentUser?.email ?: ""
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        DeepObsidian,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    ) {
                        val activeScreen = if (currentScreen == "splash") {
                            "splash"
                        } else if (currentUser == null) {
                            "login"
                        } else {
                            currentScreen
                        }
                        AnimatedContent(
                            targetState = activeScreen,
                            transitionSpec = {
                                slideInVertically(initialOffsetY = { 600 }) + fadeIn() with
                                        slideOutVertically(targetOffsetY = { -600 }) + fadeOut()
                            },
                            label = "screenTransition"
                        ) { screen ->
                            when (screen) {
                                "splash" -> SplashScreen(viewModel)
                                "login" -> LoginRegisterScreen(viewModel)
                                "market_feed" -> MarketplaceFeedScreen(viewModel)
                                "detail" -> AccountDetailScreen(viewModel)
                                "escrow_deal" -> EscrowDealScreen(viewModel)
                                "wallet" -> WalletDashboardScreen(viewModel)
                                "subscription" -> SubscriptionPlanScreen(viewModel)
                                "profile" -> UserProfileScreen(viewModel)
                                "admin" -> AdminControlDashboard(viewModel)
                                else -> MarketplaceFeedScreen(viewModel)
                            }
                        }
                    }
                }

                // WebRTC Calling overlay simulation
                val callState by viewModel.activeCallState.collectAsState()
                val activeCall = callState
                if (activeCall != null) {
                    WebRtcCallOverlay(callState = activeCall, onHangUp = { viewModel.stopCall() }, onScreenShare = { viewModel.triggerScreenShare() })
                }
            }
        }
    }
}

// --- Dynamic Bottom Nav matching Material 3 Guidelines with Notch/Padding Handling ---
@Composable
fun SajiloBottomNavigation(
    currentScreen: String,
    onSelectScreen: (String) -> Unit,
    userTier: String,
    userEmail: String
) {
    NavigationBar(
        containerColor = LightSlate,
        tonalElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding()
    ) {
        val items = remember(userEmail) {
            val list = mutableListOf(
                Triple("market_feed", Icons.Default.Home, "Market"),
                Triple("wallet", Icons.Default.Wallet, "Wallet"),
                Triple("subscription", Icons.Default.Star, "Pro Boost"),
                Triple("profile", Icons.Default.Person, "Profile")
            )
            if (userEmail.equals("ranjghimire12@gmail.com", ignoreCase = true)) {
                list.add(Triple("admin", Icons.Default.Security, "Mod Area"))
            }
            list
        }

        items.forEach { (route, icon, title) ->
            val isSelected = currentScreen == route || (route == "market_feed" && currentScreen == "detail") || (route == "profile" && currentScreen == "escrow_deal")
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectScreen(route) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (route == "subscription" && userTier != "Free") {
                                Badge(containerColor = GamerGreen) { Text("PRO", fontSize = 9.sp, color = DeepObsidian) }
                            }
                        }
                    ) {
                        Icon(imageVector = icon, contentDescription = title)
                    }
                },
                label = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyberCyan,
                    selectedTextColor = CyberCyan,
                    indicatorColor = CyberCyan.copy(alpha = 0.15f),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag("nav_item_$route")
            )
        }
    }
}

// --- 1. Login with interactive cyberpunk game background animations ---
@Composable
fun LoginRegisterScreen(viewModel: SajiloViewModel) {
    val authState by viewModel.authState.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    var uiFeedbackText by remember { mutableStateOf<String?>(null) }
    
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(true) } // Default to signup to create accounts easily

    // Pulsing cyber glow state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowInten by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        // Large Cyberpunk Logo Design using Canvas Brush Background
        Box(
            modifier = Modifier
                .size(100.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF0D64).copy(alpha = 0.35f * glowInten),
                                Color.Transparent
                            )
                        ),
                        radius = size.width * 1.3f
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            SajiloLogo(
                modifier = Modifier.size(90.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SAJILO TRADE",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = CyberCyan,
            letterSpacing = 2.sp
        )

        Text(
            text = "PREMIUM GAMING MARKETPLACE & ESCROW",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = ElectroViolet,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (authState) {
            is AuthState.Unauthenticated -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SteelBlue),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, LightSlate)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (isSignUpMode) "Create Free Account" else "Safe Portal Authentication",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Dynamic validation or backend error messages
                        if (loginError != null || uiFeedbackText != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color(0xFFFF0D64).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFFF0D64).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = loginError ?: uiFeedbackText ?: "",
                                    color = Color(0xFFFF5E5E),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                label = { Text("Desired Username") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("username_input"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Game Registered Email") },
                            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Shield Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                uiFeedbackText = null
                                if (isSignUpMode) {
                                    if (usernameInput.isBlank() || emailInput.isBlank() || passwordInput.isBlank()) {
                                        uiFeedbackText = "Please fill all fields."
                                    } else {
                                        viewModel.startEmailSignup(emailInput, usernameInput, passwordInput) { success, msg ->
                                            if (!success) {
                                                uiFeedbackText = msg
                                            }
                                        }
                                    }
                                } else {
                                    viewModel.loginWithEmailAndPassword(emailInput, passwordInput) { success, msg ->
                                        if (!success) {
                                            uiFeedbackText = msg
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("primary_auth_submit"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isSignUpMode) "CONTINUE TO PROFILE SETUP" else "ENTER ESCROW PORTAL",
                                color = DeepObsidian,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = { 
                                isSignUpMode = !isSignUpMode 
                                uiFeedbackText = null
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = if (isSignUpMode) "Already verified? Sign In Here" else "New trader? Create Account Securely",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Social 1-tap fast connection (Google, Steam, Discord)
                Text("RESTRICTED THIRD-PARTY LOGIN PROVIDERS", fontSize = 10.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SocialLargeButton(
                        name = "Google",
                        icon = Icons.Default.Stream,
                        color = Color(0xFFEA4335),
                        onClick = { viewModel.initiateThirdPartyLogin("Google") }
                    )
                    SocialLargeButton(
                        name = "Steam",
                        icon = Icons.Default.Gamepad,
                        color = Color(0xFF171A21),
                        onClick = { viewModel.initiateThirdPartyLogin("Steam") }
                    )
                    SocialLargeButton(
                        name = "Discord",
                        icon = Icons.Default.Forum,
                        color = Color(0xFF5865F2),
                        onClick = { viewModel.initiateThirdPartyLogin("Discord") }
                    )
                }
            }

            is AuthState.OtpVerification -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SteelBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "One-Time OTP Code Authentication",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(
                            "We have sent a six digit code to your mailbox coordinates. Check spam box if missing.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { if (it.length <= 6) otpInput = it },
                            label = { Text("6-Digit Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.verifyOtp(otpInput) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                        ) {
                            Text("VALIDATE CODE", color = DeepObsidian, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is AuthState.ProfileSetup -> {
                var bioStr by remember { mutableStateOf("") }
                var discStr by remember { mutableStateOf("") }
                var instStr by remember { mutableStateOf("") }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SteelBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Sajilo Professional Profile Setup",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = bioStr,
                            onValueChange = { bioStr = it },
                            label = { Text("Trader Public Biography (Bio)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = discStr,
                            onValueChange = { discStr = it },
                            label = { Text("Discord Username Tag") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = instStr,
                            onValueChange = { instStr = it },
                            label = { Text("Instagram Account URL") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.submitProfileSetup(bioStr, discStr, instStr) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                        ) {
                            Text("FINALIZE REGISTRATION", color = DeepObsidian, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is AuthState.Authenticated -> {
                // If authenticated but screen shows login, route them
                LaunchedEffect(Unit) {
                    viewModel.navigateTo("market_feed")
                }
            }
        }
    }
}

@Composable
fun SocialLargeButton(name: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = SteelBlue),
        border = BorderStroke(1.dp, color),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(96.dp)
            .height(55.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(imageVector = icon, contentDescription = name, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(name, fontSize = 8.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

// --- 2. Live Dynamic Marketplace & Filtering Dashboard ---
@Composable
fun MarketplaceFeedScreen(viewModel: SajiloViewModel) {
    val listings by viewModel.listings.collectAsState()
    val searchQ by viewModel.searchQuery.collectAsState()
    val selectedGame by viewModel.filterGame.collectAsState()
    val announcement by viewModel.announcement.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showPostDialog by remember { mutableStateOf(false) }

    // Filtered lists computation locally to avoid blocking
    val filteredListings = remember(listings, searchQ, selectedGame) {
        listings.filter { listing ->
            val matchesGame = selectedGame.isEmpty() || listing.gameType.equals(selectedGame, ignoreCase = true)
            val matchesSearch = searchQ.isEmpty() ||
                    listing.title.contains(searchQ, ignoreCase = true) ||
                    listing.rank.contains(searchQ, ignoreCase = true) ||
                    listing.description.contains(searchQ, ignoreCase = true)
            matchesGame && matchesSearch && !listing.isSold
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Premium Brand App Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // S Logo Emblem with custom high-fidelity drawn icon matching uploaded design
                    SajiloLogo(
                        modifier = Modifier.size(40.dp)
                    )

                    Column {
                        Text(
                            text = "SajiloTrade",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "PREMIUM MARKETPLACE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = CyberCyan
                        )
                    }
                }

                // Balance display & avatar button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val balance = currentUser?.walletBalance ?: 2450.0
                    Box(
                        modifier = Modifier
                            .background(SteelBlue, RoundedCornerShape(99.dp))
                            .border(1.dp, LightSlate, RoundedCornerShape(99.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(GamerGreen, CircleShape)
                            )
                            Text(
                                text = String.format("$%.2f", balance),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    // Avatar placeholder
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SteelBlue, CircleShape)
                            .border(1.dp, LightSlate, CircleShape)
                            .clickable { viewModel.navigateTo("profile") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // News Announcement Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SteelBlue.copy(alpha = 0.8f)),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = announcement,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Live trust volume indicators table
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsMiniCard("SECURE ESCROW", "\$490K+", "100% Locked", GamerGreen, modifier = Modifier.weight(1f))
                AnalyticsMiniCard("SERVERS MONITOR", "ACTIVE", "0% Fault Rate", CyberCyan, modifier = Modifier.weight(1f))
            }
        }

        // Game selector chips header
        item {
            val games = listOf("All", "Valorant", "Free Fire", "PUBG Mobile", "Steam", "Fortnite", "Clash of Clans")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(games) { game ->
                    val isSelected = (game == "All" && selectedGame.isEmpty()) || game.equals(selectedGame, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectGameFilter(if (game == "All") "" else game) },
                        label = { Text(game, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan,
                            selectedLabelColor = DeepObsidian,
                            containerColor = SteelBlue,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = LightSlate,
                            selectedBorderColor = CyberCyan
                        )
                    )
                }
            }
        }

        // Search Bar & Filters Switch
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQ,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search account names/weapons/skins...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SteelBlue,
                        unfocusedContainerColor = SteelBlue,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = LightSlate
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = { showFilterSheet = !showFilterSheet },
                    modifier = Modifier.size(52.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = SteelBlue)
                ) {
                    Icon(
                        imageVector = if (showFilterSheet) Icons.Default.Close else Icons.Default.FilterList,
                        contentDescription = "Search Config Filters",
                        tint = CyberCyan
                    )
                }
            }
        }

        // Advanced filter options inline dropdown
        if (showFilterSheet) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SteelBlue),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Search Filter Metrics", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.applyFilters("Asia-Pacific", "", true, 300.0) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = LightSlate),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Verified + S.Asia", fontSize = 10.sp, color = TextPrimary)
                            }

                            Button(
                                onClick = { viewModel.applyFilters("", "PC", false, 150.0) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = LightSlate),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Under \$150 PC only", fontSize = 10.sp, color = TextPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { viewModel.resetFilters() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Reset Search Filters", color = HotPink, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Boosted Seller Slider
        val boostedList = filteredListings.filter { it.isBoosted }
        if (boostedList.isNotEmpty() && searchQ.isEmpty()) {
            item {
                Text("🔥 PREMIUM BOOSTED LISTINGS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = HotPink)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(boostedList) { bListing ->
                        BoostedListingCard(bListing, onDetail = { viewModel.viewListingDetail(bListing) })
                    }
                }
            }
        }

        // Grid feed items Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE LIVE CORRIDOR FEED (${filteredListings.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Text("Escrow Active", color = GamerGreen, fontSize = 10.sp)
            }
        }

        if (filteredListings.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(64.dp), tint = TextMuted)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Game Passcodes listed in this coordinate range.", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            // Main dynamic listing items
            items(filteredListings) { listing ->
                MarketplaceRowCard(
                    listing = listing,
                    onDetail = { viewModel.viewListingDetail(listing) },
                    onSellerProfile = { viewModel.viewUserProfile(listing.sellerId) }
                )
            }
        }

        // Add account float button helper panel representation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ElectroViolet.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, ElectroViolet.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPostDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("List Your Game Account Pass-code", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                        Text("Instant sell. Sajilo Escrow handles the money holding safety.", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }

    if (showPostDialog) {
        PostListingDialog(viewModel = viewModel, onDismiss = { showPostDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostListingDialog(viewModel: SajiloViewModel, onDismiss: () -> Unit) {
    var gameType by remember { mutableStateOf("Steam") }
    var title by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var uid by remember { mutableStateOf("") }
    var gameUsername by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("Asia-Pacific") }
    var platform by remember { mutableStateOf("PC") }
    var rank by remember { mutableStateOf("No Rank") }
    var levelStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var inventoryDetails by remember { mutableStateOf("") }
    var linkedMethod by remember { mutableStateOf("Username & Password") }
    var errorMessage by remember { mutableStateOf("") }

    val gamesList = listOf("Valorant", "Free Fire", "PUBG Mobile", "Steam", "Fortnite", "Clash of Clans")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Register New Game Listing",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
            )
        },
        containerColor = DeepObsidian,
        textContentColor = TextPrimary,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select Target Game Universe:", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gamesList.forEach { g ->
                        val selected = gameType == g
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selected) CyberCyan else SteelBlue,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (selected) CyberCyan else LightSlate,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { gameType = g }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = g,
                                color = if (selected) DeepObsidian else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = Color(0xFFFF5E5E), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("List Title (e.g. Steam account with Cyberpunk)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price (USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                    )
                    OutlinedTextField(
                        value = levelStr,
                        onValueChange = { levelStr = it },
                        label = { Text("Level") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gameUsername,
                        onValueChange = { gameUsername = it },
                        label = { Text("Game Username") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                    )
                    OutlinedTextField(
                        value = uid,
                        onValueChange = { uid = it },
                        label = { Text("Player UID") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = region,
                        onValueChange = { region = it },
                        label = { Text("Region/Server") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                    )
                    OutlinedTextField(
                        value = rank,
                        onValueChange = { rank = it },
                        label = { Text("Rank status") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = platform,
                        onValueChange = { platform = it },
                        label = { Text("Platform (PC/Mobile)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                    )
                    OutlinedTextField(
                        value = linkedMethod,
                        onValueChange = { linkedMethod = it },
                        label = { Text("Login bind path") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Rare Skins") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                )

                OutlinedTextField(
                    value = inventoryDetails,
                    onValueChange = { inventoryDetails = it },
                    label = { Text("Inventory details") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceVal = priceStr.toDoubleOrNull() ?: 0.0
                    val lvlVal = levelStr.toIntOrNull() ?: 1
                    if (title.isBlank() || priceVal <= 0.0 || gameUsername.isBlank()) {
                        errorMessage = "Please enter valid Title, Price, and Username."
                    } else {
                        viewModel.postNewListing(
                            gameType = gameType,
                            title = title,
                            price = priceVal,
                            uid = uid,
                            gameUsername = gameUsername,
                            region = region,
                            platform = platform,
                            rank = rank,
                            level = lvlVal,
                            description = description,
                            inventory = inventoryDetails,
                            linkedMethod = linkedMethod
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
            ) {
                Text("POST LISTING NOW", color = DeepObsidian, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AnalyticsMiniCard(title: String, valStr: String, desc: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SteelBlue),
        border = BorderStroke(1.dp, LightSlate),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Text(valStr, fontSize = 18.sp, fontWeight = FontWeight.Black, color = accent)
            Text(desc, fontSize = 9.sp, color = TextMuted)
        }
    }
}

@Composable
fun BoostedListingCard(listing: GameListing, onDetail: () -> Unit) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable { onDetail() },
        colors = CardDefaults.cardColors(containerColor = SteelBlue),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, HotPink.copy(alpha = 0.5f))
    ) {
        Column {
            Box(modifier = Modifier.height(110.dp)) {
                AsyncImage(
                    model = listing.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(HotPink, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Text("BOOSTED PRO", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(DeepObsidian.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Text(listing.gameType, color = CyberCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = listing.title,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RANK: ${listing.rank}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "\$${listing.price}",
                        fontSize = 18.sp,
                        color = GamerGreen,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun MarketplaceRowCard(listing: GameListing, onDetail: () -> Unit, onSellerProfile: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetail() },
        colors = CardDefaults.cardColors(containerColor = SteelBlue),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, LightSlate)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = listing.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(listing.gameType, color = CyberCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("lvl ${listing.level}", color = TextSecondary, fontSize = 9.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listing.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Seller: ",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Text(
                        text = listing.sellerName,
                        fontSize = 10.sp,
                        color = CyberCyan,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onSellerProfile() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Star, contentDescription = null, tint = AmberGold, modifier = Modifier.size(10.dp))
                    Text(text = "${listing.sellerRating}", fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(start = 2.dp))
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "\$${listing.price}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = GamerGreen
                )
                Text(
                    text = listing.region,
                    fontSize = 9.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

// --- 3. Gaming UID & Inventory Buyer Checking Screen ---
@Composable
fun AccountDetailScreen(viewModel: SajiloViewModel) {
    val listing by viewModel.selectedListing.collectAsState()
    val activeUser by viewModel.currentUser.collectAsState()

    var showStatsChecklist by remember { mutableStateOf(false) }
    var verifyingStatsSec by remember { mutableStateOf(false) }

    if (listing == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Large detail hero image
        Box(modifier = Modifier.height(200.dp)) {
            AsyncImage(
                model = listing?.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = { viewModel.navigateTo("market_feed") },
                modifier = Modifier
                    .padding(16.dp)
                    .background(DeepObsidian.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .background(DeepObsidian.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .align(Alignment.BottomStart)
            ) {
                Text("${listing?.gameType} Escrow Item", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = listing?.title ?: "",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing Tag Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SteelBlue),
                border = BorderStroke(1.dp, LightSlate)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Escrow Protection Purchase Price", fontSize = 11.sp, color = TextSecondary)
                        Text("\$${listing?.price}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = CyberCyan)
                    }

                    Button(
                        onClick = {
                            if (listing != null) {
                                viewModel.buyListingAndLockEscrow(listing!!)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("buy_and_lock_button")
                    ) {
                        Text("🔒 BUY & LOCK FUNDS", color = DeepObsidian, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Inform the buyer if wallet balance is low
            if (activeUser != null && (activeUser?.walletBalance ?: 0.0) < (listing?.price ?: 0.0)) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = CoralRed.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, CoralRed)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CoralRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Insufficient Wallet Escrow Funds", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                            Text("Your Escrow Balance is \$${activeUser?.walletBalance}. Head to Wallet screen to inject funds first.", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time stats verifier
            Text("SAJILO VERIF-SHIELD: INSTANT ACCOUNT AUDIT", fontSize = 11.sp, fontWeight = FontWeight.Black, color = GamerGreen)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SteelBlue),
                border = BorderStroke(1.dp, GamerGreen.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Interactive audit queries direct game network links to verify active levels, rank frame, KD ratios and custom weapon skins before making payment.", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (verifyingStatsSec) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally), color = CyberCyan)
                    } else {
                        Button(
                            onClick = {
                                verifyingStatsSec = true
                                showStatsChecklist = false
                                // Simulate API delays
                                showStatsChecklist = true
                                verifyingStatsSec = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LightSlate),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("INSPECT REAL-TIME ACCOUNT STATS", color = GamerGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    if (showStatsChecklist) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = LightSlate)
                        Spacer(modifier = Modifier.height(8.dp))
                        StatVerifyResultRow("Gamer Level Status: ${listing?.level}", true)
                        StatVerifyResultRow("Lobby Region Servers: ${listing?.region}", true)
                        StatVerifyResultRow("Match Rank Tier: ${listing?.rank}", true)
                        StatVerifyResultRow("Clean ID Verified (No reports): Clean", true)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Game Account Parameters Grid
            Text("PUBLIC GENERAL SPECS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpecMiniBlock(label = "Platform", value = listing?.platform ?: "", modifier = Modifier.weight(1f))
                SpecMiniBlock(label = "Rank Frame", value = listing?.rank ?: "", modifier = Modifier.weight(1f))
                SpecMiniBlock(label = "Device Level", value = "${listing?.level ?: 0}", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpecMiniBlock(label = "Bind Linked", value = listing?.linkedMethod ?: "", modifier = Modifier.weight(1f))
                SpecMiniBlock(label = "Player Lobby UID", value = listing?.uid ?: "", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Detailed Description
            Text("DETAILED SELLER INFORMATION", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = listing?.description ?: "",
                fontSize = 13.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("INVENTORY AND SKIN ITEMS DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = listing?.inventoryDetails ?: "",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun StatVerifyResultRow(label: String, verified: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (verified) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (verified) GamerGreen else CoralRed,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 11.sp, color = TextPrimary)
    }
}

@Composable
fun SpecMiniBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SteelBlue),
        border = BorderStroke(1.dp, LightSlate)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 11.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}


// --- 4. Secure Anti-Scam Escrow Protection Deal Control Panel ---
@Composable
fun EscrowDealScreen(viewModel: SajiloViewModel) {
    val trade by viewModel.selectedTrade.collectAsState()
    val chatMessages by viewModel.activeChatMessages.collectAsState()
    val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()
    val activeUser by viewModel.currentUser.collectAsState()

    var chatTextInput by remember { mutableStateOf("") }
    var disputeText by remember { mutableStateOf("") }
    var showDisputeSheet by remember { mutableStateOf(false) }

    if (trade == null) return

    val currentTrade = trade!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian)
    ) {
        // Core Escrow header info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SteelBlue),
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(1.dp, LightSlate)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateTo("market_feed") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = CyberCyan)
                    }
                    Text("Secure Escrow Pipeline Room: ${currentTrade.tradeId}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Phase indicators tracker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EscrowStepBubble("Locked", currentTrade.status != "AWAITING_DEPOSIT", GamerGreen)
                    EscrowStepBubble("Verify stats", currentTrade.status == "TRANSFERRING" || currentTrade.status == "HALF_RELEASED" || currentTrade.status == "COMPLETED", CyberCyan)
                    EscrowStepBubble("Completing", currentTrade.status == "COMPLETED", HotPink)
                }
            }
        }

        // Easy Gamer ID Trade Guideline Card
        var showEscrowGuide by remember { mutableStateOf(true) }
        if (showEscrowGuide) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCyan.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Text(
                                "How Escrow Trading Works",
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showEscrowGuide = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text("1. Funds Locked in Escrow: Your payment is safely held by SajiloGuard.", color = TextPrimary, fontSize = 11.sp, lineHeight = 14.sp)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("2. Verify Account Login: The seller will send credentials in chat. Log in and inspect the game account status.", color = TextPrimary, fontSize = 11.sp, lineHeight = 14.sp)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("3. Confirm and Release: If everything checks out, tap 'CONFIRM RECEIVED' to release the funds. In case of issues, click 'DISPUTE'.", color = TextPrimary, fontSize = 11.sp, lineHeight = 14.sp)
                }
            }
        }

        // Action controls for buyer only
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                "BUYER SHIELD INTERACTIVE CONTROLS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.confirmWholeDealCompleted() },
                    colors = ButtonDefaults.buttonColors(containerColor = GamerGreen),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("confirm_deal_completed"),
                    shape = RoundedCornerShape(8.dp),
                    enabled = currentTrade.status != "COMPLETED" && currentTrade.status != "DISPUTED"
                ) {
                    Text("CONFIRM RECEIVED", color = DeepObsidian, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }

                Button(
                    onClick = { viewModel.claimHalfPayment() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("release_half_payment"),
                    shape = RoundedCornerShape(8.dp),
                    enabled = currentTrade.status == "FUNDS_LOCKED"
                ) {
                    Text("RELEASE HALF", color = DeepObsidian, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showDisputeSheet = !showDisputeSheet },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("dispute_payment"),
                    shape = RoundedCornerShape(8.dp),
                    enabled = currentTrade.status != "COMPLETED" && currentTrade.status != "DISPUTED"
                ) {
                    Text("DISPUTE ESCROW", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (showDisputeSheet) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = SteelBlue),
                    border = BorderStroke(1.dp, CoralRed)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Initiate Security Fraud Dispute", color = CoralRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = disputeText,
                            onValueChange = { disputeText = it },
                            placeholder = { Text("Describe password mismatch/scams...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CoralRed)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.claimDispute(disputeText)
                                showDisputeSheet = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                        ) {
                            Text("ALERT SAJILO RECTIFIER FRAUD SHIELD", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // High contrast scrolling realtime chats
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, LightSlate, RoundedCornerShape(12.dp))
                .background(SteelBlue.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of Chat detailing connected players and calling triggers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LightSlate)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(GamerGreen, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trading Chat Space: ${if (activeUser?.userId == currentTrade.buyerId) currentTrade.sellerName else currentTrade.buyerName}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Simulated audio/video calls
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { viewModel.initiateWebRtcCall("audio") }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = "Audio Ring", tint = CyberCyan, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { viewModel.initiateWebRtcCall("video") }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.VideoCall, contentDescription = "Video Ring", tint = CyberCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Chat Messages Feed Box
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatMessages) { msg ->
                        ChatMsgBubble(msg, currentUserId = activeUser?.userId ?: "")
                    }

                    if (isPartnerTyping) {
                        item {
                            Text("Seller is typing (लेख्दै हुनुहुन्छ)...", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                // Bottom Input with simulated screenshot/proof attachment
                HorizontalDivider(color = LightSlate)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            // Mock gallery photo upload
                            viewModel.sendChatMessage("Uploaded proof attachment", imageUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500")
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = CyberCyan)
                    }

                    OutlinedTextField(
                        value = chatTextInput,
                        onValueChange = { chatTextInput = it },
                        placeholder = { Text("Send a message...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text"),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DeepObsidian,
                            unfocusedContainerColor = DeepObsidian,
                            focusedBorderColor = CyberCyan
                        ),
                        maxLines = 2,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (chatTextInput.isNotEmpty()) {
                                viewModel.sendChatMessage(chatTextInput)
                                chatTextInput = ""
                            }
                        },
                        modifier = Modifier
                            .background(CyberCyan, CircleShape)
                            .size(36.dp)
                            .testTag("chat_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = DeepObsidian, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun EscrowStepBubble(title: String, active: Boolean, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (active) color else TextMuted, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, fontSize = 10.sp, color = if (active) TextPrimary else TextMuted, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ChatMsgBubble(msg: ChatMessage, currentUserId: String) {
    val isMe = msg.senderId == currentUserId
    val isSys = msg.senderId == "SYSTEM"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isSys) Alignment.CenterHorizontally else if (isMe) Alignment.End else Alignment.Start
    ) {
        if (isSys) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ElectroViolet.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, ElectroViolet.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(msg.senderName, fontSize = 10.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                    Text(msg.text, fontSize = 11.sp, color = TextPrimary, lineHeight = 15.sp)
                }
            }
        } else {
            Text(msg.senderName, fontSize = 8.sp, color = TextMuted, modifier = Modifier.padding(horizontal = 4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isMe) CyberCyan else LightSlate),
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isMe) 12.dp else 0.dp,
                    bottomEnd = if (isMe) 0.dp else 12.dp
                ),
                modifier = Modifier.widthIn(max = 260.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (msg.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = msg.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .height(120.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        msg.text,
                        fontSize = 12.sp,
                        color = if (isMe) DeepObsidian else TextPrimary
                    )
                }
            }
        }
    }
}


// --- 5. Interactive Digital Wallet Dash Screen ---
@Composable
fun WalletDashboardScreen(viewModel: SajiloViewModel) {
    val user by viewModel.currentUser.collectAsState()
    val transactions by viewModel.activeUserTransactions.collectAsState()

    var fundAmtStr by remember { mutableStateOf("") }
    var actionMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("MY DIGITAL SECURE ESCROW WALLET", fontSize = 11.sp, fontWeight = FontWeight.Black, color = CyberCyan)

        // Visa / Mastercard cybercard mockup
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(containerColor = SteelBlue)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                ElectroViolet.copy(alpha = 0.8f),
                                CyberCyan.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SajiloShield Shield Wallet", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Card(colors = CardDefaults.cardColors(containerColor = GamerGreen.copy(alpha = 0.2f))) {
                            Text("AUDITED ACTIVE", color = GamerGreen, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Column {
                        Text("COMPREHENSIVE LIQUID BALANCE", color = TextSecondary, fontSize = 10.sp)
                        Text("\$${user?.walletBalance ?: 0.0}", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(user?.username ?: "", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("Escrow Locked: \$${user?.escrowBalance ?: 0.0}", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Fund Control Action Block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SteelBlue),
            border = BorderStroke(1.dp, LightSlate)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Escrow Injector / Withdrawal Port", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = fundAmtStr,
                    onValueChange = { fundAmtStr = it },
                    label = { Text("Transfer Cash Amount (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val amt = fundAmtStr.toDoubleOrNull()
                            if (amt != null && amt > 0) {
                                viewModel.addFundsToWallet(amt)
                                actionMessage = "Successfully deposited \$${amt} via Google Pay secure token gateway."
                                fundAmtStr = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("DEPOSIT", color = DeepObsidian, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val amt = fundAmtStr.toDoubleOrNull()
                            if (amt != null && amt > 0 && amt <= (user?.walletBalance ?: 0.0)) {
                                viewModel.withdrawFundsFromWallet(amt)
                                actionMessage = "Successfully withdrew \$${amt} to your linked bank account."
                                fundAmtStr = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LightSlate),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("WITHDRAW", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                if (actionMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(actionMessage, fontSize = 10.sp, color = GamerGreen, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Wallet History logs
        Text("TRANSACTIONS CHRONOLOGY LEDGER", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextMuted)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SteelBlue),
            border = BorderStroke(1.dp, LightSlate)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (transactions.isEmpty()) {
                    Text("No transactions logged in this wallet session yet.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                } else {
                    transactions.forEach { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(tx.type, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("ID: ${tx.txId}", color = TextMuted, fontSize = 9.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (tx.type == "DEPOSIT" || tx.type == "ESCROW_REFUND" || tx.type == "ESCROW_RELEASE" && user?.userId != tx.userId) "+\$${tx.amount}" else "-\$${tx.amount}",
                                    color = if (tx.type == "DEPOSIT" || tx.type == "ESCROW_REFUND") GamerGreen else HotPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(tx.status, color = GamerGreen, fontSize = 8.sp)
                            }
                        }
                        HorizontalDivider(color = LightSlate)
                    }
                }
            }
        }
    }
}


// --- 6. Pro Priority Subscription Center Screen ---
@Composable
fun SubscriptionPlanScreen(viewModel: SajiloViewModel) {
    val activeUser by viewModel.currentUser.collectAsState()
    var purchaseConfirmationMessage by remember { mutableStateOf("") }

    val tiersList = listOf(
        Triple("Bronze Pass", 9.99, "Feed visibility priority boost • Direct uploads verification"),
        Triple("Silver Pass", 19.99, "Homepage slider boost banner • Fast 10-minutes listing approval"),
        Triple("Gold Pass", 29.99, "Dual badge tracker • Maximum server visibility push • More photo spots"),
        Triple("Verified Pro", 49.99, "Verified Platinum Shield Trust badge • 0% trading transaction commission fee • Highlight rank frame")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SAJILO VIP EXPANSION CORRIDOR", fontSize = 11.sp, fontWeight = FontWeight.Black, color = CyberCyan)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ElectroViolet.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, ElectroViolet.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Gain Ultimate Placement Boosts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Subscribers enjoy instant auto-approval, feed optimization, Verified Pro badges, and are listed higher on search results index pages.", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your Active Level: ", fontSize = 11.sp, color = TextMuted)
                    Text(activeUser?.subscriptionTier ?: "Free Base Pack", color = HotPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (purchaseConfirmationMessage.isNotEmpty()) {
            Text(purchaseConfirmationMessage, color = GamerGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        // List Tiers
        tiersList.forEach { (name, price, benefits) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SteelBlue),
                border = BorderStroke(1.dp, if (activeUser?.subscriptionTier == name) CyberCyan else LightSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                if (activeUser?.subscriptionTier == name) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Card(colors = CardDefaults.cardColors(containerColor = CyberCyan)) {
                                        Text("CURRENT", color = DeepObsidian, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(benefits, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                        }

                        Button(
                            onClick = {
                                if ((activeUser?.walletBalance ?: 0.0) >= price) {
                                    viewModel.buySubscription(name, price)
                                    purchaseConfirmationMessage = "Sajilo Protocol upgraded to $name successfully! Premium visibility loaded."
                                } else {
                                    purchaseConfirmationMessage = "Upgrade failed. Deposit \$${price} to lock this tier."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            enabled = activeUser?.subscriptionTier != name,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("\$${price}/mo", color = DeepObsidian, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}


// --- 7. Public Seller Profile Screen ---
@Composable
fun UserProfileScreen(viewModel: SajiloViewModel) {
    val activeUser by viewModel.currentUser.collectAsState()
    val viewingUser by viewModel.viewingUser.collectAsState()
    val myTrades by viewModel.trades.collectAsState()

    val profile = viewingUser ?: activeUser ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (viewingUser != null) {
                IconButton(onClick = { viewModel.navigateTo("market_feed") }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = CyberCyan)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text("PUBLIC GAME TRADER DOSSIER", fontSize = 11.sp, fontWeight = FontWeight.Black, color = CyberCyan)
        }

        // Profile Avatar, Username rank badge
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SteelBlue),
            border = BorderStroke(1.dp, LightSlate)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(2.dp, CyberCyan, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(GamerGreen, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.username, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (profile.subscriptionTier != "Free") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Verified, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                    }
                }

                Text(profile.email, fontSize = 11.sp, color = TextMuted)

                Spacer(modifier = Modifier.height(8.dp))

                Card(colors = CardDefaults.cardColors(containerColor = LightSlate)) {
                    Text(
                        text = profile.subscriptionTier + " Priority Tier",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = profile.bio,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Ratings metrics & Trust factor dashboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SteelBlue),
                border = BorderStroke(1.dp, LightSlate),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TRUST RATIO", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text("${profile.trustScore}%", fontSize = 24.sp, fontWeight = FontWeight.Black, color = GamerGreen)
                    Text("Sajilo Audit Certified", fontSize = 8.sp, color = TextSecondary)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SteelBlue),
                border = BorderStroke(1.dp, LightSlate),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("COMPLETED TRADES", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text("${profile.completionPercentage}%", fontSize = 24.sp, fontWeight = FontWeight.Black, color = CyberCyan)
                    Text("Escrow Compliant", fontSize = 8.sp, color = TextSecondary)
                }
            }
        }

        // Social Connections details links
        Text("SOCIAL BINDINGS (ESCROW ACCREDITED)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextMuted)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SteelBlue),
            border = BorderStroke(1.dp, LightSlate)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SocialBindingParam("Discord Connection", "active_discord_user_0", GamerGreen)
                SocialBindingParam("Steam community ID", "community_link_steam", CyberCyan)
                SocialBindingParam("Instagram Gamer Handle", "@sajilo_trader", HotPink)
            }
        }

        // Logout bypass button
        if (viewingUser == null) {
            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = HotPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("logout_button")
            ) {
                Text("LOGOUT PORTAL", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SocialBindingParam(label: String, valStr: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text(valStr, fontSize = 11.sp, color = accent, fontWeight = FontWeight.Bold)
    }
}


// --- 8. Moderator Admin Controls Dashboard Screen ---
@Composable
fun AdminControlDashboard(viewModel: SajiloViewModel) {
    val activeUser by viewModel.currentUser.collectAsState()
    if (activeUser?.email?.equals("ranjghimire12@gmail.com", ignoreCase = true) != true) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "No Access",
                    tint = Color(0xFFFF5E5E),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "ACCESS DENIED",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5E5E),
                    fontSize = 18.sp
                )
                Text(
                    text = "Only the master administrator (ranjghimire12@gmail.com) can access Sajilo Trade's moderation area.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val usersList by viewModel.users.collectAsState()
    val tradesList by viewModel.trades.collectAsState()
    val adminSearchQuery by viewModel.adminSearchQuery.collectAsState()

    var modAlertInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SAJILO PORTAL MODERATION ZONE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = HotPink)

        // Custom push announcement editor
        Card(
            colors = CardDefaults.cardColors(containerColor = SteelBlue),
            border = BorderStroke(1.dp, HotPink.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Live Global Alert Banner Editor", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = modAlertInput,
                    onValueChange = { modAlertInput = it },
                    placeholder = { Text("E.g. Free Fire database sync is complete...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (modAlertInput.isNotEmpty()) {
                            viewModel.pushAnnouncement(modAlertInput)
                            modAlertInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotPink)
                ) {
                    Text("PUSH UPDATE GLOBAL BANNER", color = Color.White)
                }
            }
        }

        // Active Users freeze options list
        Text("MALICIOUS USER SCANNER (BAN SCAMMERS)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextMuted)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SteelBlue),
            border = BorderStroke(1.dp, LightSlate)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                usersList.forEach { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(user.username, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (user.trustScore == 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("FROZEN", color = CoralRed, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Text(user.email, color = TextMuted, fontSize = 9.sp)
                        }

                        Button(
                            onClick = { viewModel.banAndSuspendUser(user.userId) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (user.trustScore == 0) LightSlate else CoralRed),
                            shape = RoundedCornerShape(4.dp),
                            enabled = user.trustScore > 0
                        ) {
                            Text("BAN PORTAL", fontSize = 9.sp, color = Color.White)
                        }
                    }
                    HorizontalDivider(color = LightSlate)
                }
            }
        }

        // Global Escrow Trade logs
        Text("COMPREHENSIVE GLOBAL ESCROW ORDERS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextMuted)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SteelBlue),
            border = BorderStroke(1.dp, LightSlate)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (tradesList.isEmpty()) {
                    Text("No live escrow transfers registered in databases yet.", color = TextSecondary, fontSize = 11.sp)
                } else {
                    tradesList.forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Order: ${t.tradeId}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${t.gameType} | \$${t.price} USD", color = TextSecondary, fontSize = 10.sp)
                            }
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when (t.status) {
                                        "COMPLETED" -> GamerGreen.copy(alpha = 0.2f)
                                        "FUNDS_LOCKED" -> CyberCyan.copy(alpha = 0.2f)
                                        "DISPUTED" -> CoralRed.copy(alpha = 0.2f)
                                        else -> LightSlate
                                    }
                                )
                            ) {
                                Text(
                                    t.status,
                                    fontSize = 8.sp,
                                    color = when (t.status) {
                                        "COMPLETED" -> GamerGreen
                                        "FUNDS_LOCKED" -> CyberCyan
                                        "DISPUTED" -> CoralRed
                                        else -> TextPrimary
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        HorizontalDivider(color = LightSlate)
                    }
                }
            }
        }
    }
}


// --- Simulated interactive WebRTC video overlay calling module ---
@Composable
fun WebRtcCallOverlay(callState: String, onHangUp: () -> Unit, onScreenShare: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clickable { /* prevent bubble clicks fallback */ },
        colors = CardDefaults.cardColors(containerColor = DeepObsidian.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, CyberCyan)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SAJILOSHIELD WEBRTC SECURE ROOM", color = CyberCyan, fontWeight = FontWeight.Black, fontSize = 12.sp)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(SteelBlue, CircleShape)
                        .border(2.dp, ElectroViolet, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (callState == "active_video" || callState == "screen_share") Icons.Default.Videocam else Icons.Default.Mic,
                        contentDescription = null,
                        tint = ElectroViolet,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (callState) {
                        "calling" -> "Ringing recipient security details..."
                        "active_audio" -> "Escrow Protection Voice Stream"
                        "active_video" -> "Verified Visual Live Cam Stream (1080p)"
                        "screen_share" -> "Screen-Sharing active. Verifying UI details..."
                        else -> "Synchronizing parameters"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text("Encrypted via Sajilo VoIP Anti-Fraud protocols", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
            }

            // WebRTC calling controller layout row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onScreenShare,
                    modifier = Modifier
                        .background(LightSlate, CircleShape)
                        .size(54.dp)
                ) {
                    Icon(Icons.Default.ScreenShare, contentDescription = null, tint = CyberCyan)
                }

                IconButton(
                    onClick = onHangUp,
                    modifier = Modifier
                        .background(CoralRed, CircleShape)
                        .size(64.dp)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White)
                }

                var mutedState by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { mutedState = !mutedState },
                    modifier = Modifier
                        .background(LightSlate, CircleShape)
                        .size(54.dp)
                ) {
                    Icon(if (mutedState) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = null, tint = if (mutedState) CoralRed else Color.White)
                }
            }
        }
    }
}

// Custom High-Fidelity SajiloLogo (Triangle-S Emblem) Vector Composable
@Composable
fun SajiloLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val logoBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF0D64), // Crimson/Hot pink
                Color(0xFFFF2A55), // Pinkish Red
                Color(0xFFFF5200)  // Coral Red/Orange
            )
        )

        // Outer rounded triangle (Thick outer rim path)
        val outerPath = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            cubicTo(w * 0.55f, h * 0.12f, w * 0.58f, h * 0.17f, w * 0.61f, h * 0.22f)
            lineTo(w * 0.85f, h * 0.63f)
            cubicTo(w * 0.89f, h * 0.70f, w * 0.85f, h * 0.78f, w * 0.77f, h * 0.78f)
            lineTo(w * 0.61f, h * 0.78f)
            cubicTo(w * 0.60f, h * 0.72f, w * 0.64f, h * 0.68f, w * 0.69f, h * 0.68f)
            lineTo(w * 0.73f, h * 0.68f)
            cubicTo(w * 0.77f, h * 0.68f, w * 0.79f, h * 0.64f, w * 0.77f, h * 0.60f)
            lineTo(w * 0.56f, h * 0.25f)
            cubicTo(w * 0.53f, h * 0.20f, w * 0.47f, h * 0.20f, w * 0.44f, h * 0.25f)
            lineTo(w * 0.23f, h * 0.60f)
            cubicTo(w * 0.21f, h * 0.64f, w * 0.23f, h * 0.68f, w * 0.27f, h * 0.68f)
            lineTo(w * 0.46f, h * 0.68f)
            cubicTo(w * 0.52f, h * 0.68f, w * 0.52f, h * 0.78f, w * 0.46f, h * 0.78f)
            lineTo(w * 0.23f, h * 0.78f)
            cubicTo(w * 0.15f, h * 0.78f, w * 0.11f, h * 0.70f, w * 0.15f, h * 0.63f)
            lineTo(w * 0.39f, h * 0.22f)
            cubicTo(w * 0.42f, h * 0.17f, w * 0.45f, h * 0.12f, w * 0.5f, h * 0.12f)
            close()
        }

        drawPath(path = outerPath, brush = logoBrush)

        // Custom flowing S path located elegantly in the center and bottom section
        val innerSPath = Path().apply {
            moveTo(w * 0.5f, h * 0.38f)
            cubicTo(w * 0.58f, h * 0.38f, w * 0.67f, h * 0.42f, w * 0.67f, h * 0.52f)
            cubicTo(w * 0.67f, h * 0.62f, w * 0.56f, h * 0.64f, w * 0.53f, h * 0.68f)
            cubicTo(w * 0.50f, h * 0.72f, w * 0.55f, h * 0.75f, w * 0.59f, h * 0.75f)
            cubicTo(w * 0.56f, h * 0.79f, w * 0.48f, h * 0.79f, w * 0.46f, h * 0.74f)
            cubicTo(w * 0.43f, h * 0.68f, w * 0.48f, h * 0.64f, w * 0.52f, h * 0.60f)
            cubicTo(w * 0.58f, h * 0.55f, w * 0.58f, h * 0.50f, w * 0.54f, h * 0.47f)
            cubicTo(w * 0.51f, h * 0.44f, w * 0.45f, h * 0.45f, w * 0.42f, h * 0.48f)
            cubicTo(w * 0.40f, h * 0.44f, w * 0.44f, h * 0.38f, w * 0.5f, h * 0.38f)
            close()
        }
        drawPath(path = innerSPath, brush = logoBrush)
    }
}

@Composable
fun SplashScreen(viewModel: SajiloViewModel) {
    val context = LocalContext.current

    // Check permission state dynamically
    var cameraGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var audioGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher Contract
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: cameraGranted
        audioGranted = permissions[android.Manifest.permission.RECORD_AUDIO] ?: audioGranted
    }

    val needsPermissions = !cameraGranted || !audioGranted
    var bypassPermissions by remember { mutableStateOf(false) }

    // Navigation delay transition
    LaunchedEffect(needsPermissions, bypassPermissions) {
        if (!needsPermissions || bypassPermissions) {
            kotlinx.coroutines.delay(2200)
            viewModel.navigateTo("login")
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val r = java.util.Random(42)
                    for (i in 0..30) {
                        val x = r.nextFloat() * size.width
                        val y = r.nextFloat() * size.height
                        val sizeStar = r.nextFloat() * 4f + 2f
                        drawCircle(
                            color = CyberCyan.copy(alpha = 0.35f),
                            radius = sizeStar,
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    }
                }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer(
                        scaleX = scaleAnim,
                        scaleY = scaleAnim,
                        alpha = alphaAnim
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(ElectroViolet.copy(alpha = 0.25f), Color.Transparent)
                            )
                        )
                )

                SajiloLogo(modifier = Modifier.size(110.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SAJILO TRADE",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 4.sp
            )

            Text(
                text = "ULTRA SECURE GAMING ASSET ESCROW • 100% SECURE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (needsPermissions && !bypassPermissions) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .border(1.dp, ElectroViolet.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = LightSlate.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "DEVICE CAPABILITY CONSENT",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "SajiloTrade uses system standard protocols for anti-fraud security and WebRTC multimedia trading calls. Please authorize the following permissions:",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        HorizontalDivider(color = LightSlate, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Camera Icon",
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Camera & Microphone Access", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Powers face ID matching and real-time live trading video/audio support.", fontSize = 10.sp, color = TextSecondary)
                            }
                        }

                        Button(
                            onClick = {
                                launcher.launch(
                                    arrayOf(
                                        android.Manifest.permission.CAMERA,
                                        android.Manifest.permission.RECORD_AUDIO
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ALLOW CORE PERMISSIONS",
                                color = DeepObsidian,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = { bypassPermissions = true }
                        ) {
                            Text(
                                text = "Continue with limited capabilities",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        color = CyberCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Synchronizing encrypted decentral nodes...",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
