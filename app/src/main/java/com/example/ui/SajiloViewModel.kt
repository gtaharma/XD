package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class AuthState {
    object Unauthenticated : AuthState()
    object OtpVerification : AuthState()
    object ProfileSetup : AuthState()
    data class Authenticated(val user: UserEntity) : AuthState()
}

class SajiloViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = SajiloRepository(db.sajiloDao())

    // --- Core Flows ---
    val listings: StateFlow<List<GameListing>> = repository.allListings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trades: StateFlow<List<EscrowTrade>> = repository.allTrades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- UI/User State ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentThemeDark = MutableStateFlow(true)
    val currentThemeDark: StateFlow<Boolean> = _currentThemeDark.asStateFlow()

    // Real dynamic credentials temporary storage matching authentic signup flows
    var tempSignupUsername = ""
    var tempSignupEmail = ""
    var tempSignupPassword = ""

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // --- Search & Filters State ---
    private val _filterGame = MutableStateFlow("")
    val filterGame = _filterGame.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterRegion = MutableStateFlow("")
    val filterRegion = _filterRegion.asStateFlow()

    private val _filterPlatform = MutableStateFlow("")
    val filterPlatform = _filterPlatform.asStateFlow()

    private val _onlyVerified = MutableStateFlow(false)
    val onlyVerified = _onlyVerified.asStateFlow()

    private val _maxPrice = MutableStateFlow(500.0)
    val maxPrice = _maxPrice.asStateFlow()

    // --- Navigation & Details State ---
    private val _currentScreen = MutableStateFlow("splash") // login, profile, market_feed, detail, escrow_deal, wallet, subscription, admin, splash
    val currentScreen = _currentScreen.asStateFlow()

    private val _selectedListing = MutableStateFlow<GameListing?>(null)
    val selectedListing = _selectedListing.asStateFlow()

    private val _selectedTrade = MutableStateFlow<EscrowTrade?>(null)
    val selectedTrade = _selectedTrade.asStateFlow()

    private val _viewingUser = MutableStateFlow<UserEntity?>(null)
    val viewingUser = _viewingUser.asStateFlow()

    // --- Active Chat Message Feed ---
    private val _activeTradeId = MutableStateFlow("")
    val activeChatMessages: StateFlow<List<ChatMessage>> = _activeTradeId
        .flatMapLatest { id ->
            if (id.isEmpty()) flowOf(emptyList())
            else repository.getMessagesForTrade(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeUserTransactions: StateFlow<List<WalletTransaction>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getTransactionsForUser(user.userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeUserTransactions = _activeUserTransactions

    // --- Chat Extras & Indicators ---
    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping = _isPartnerTyping.asStateFlow()

    // --- WebRTC Simulation State ---
    private val _activeCallState = MutableStateFlow<String?>(null) // null, "calling", "active_audio", "active_video", "screen_share"
    val activeCallState = _activeCallState.asStateFlow()

    // --- Admin panel filters ---
    private val _adminSearchQuery = MutableStateFlow("")
    val adminSearchQuery = _adminSearchQuery.asStateFlow()

    private val _announcement = MutableStateFlow("SajiloTrade Escrow Security update is live. Read detailed safety transfer rules.")
    val announcement = _announcement.asStateFlow()

    init {
        viewModelScope.launch {
            // Populate seed data
            repository.populateInitialData()
        }
    }

    // --- Authentication Actions ---
    fun toggleTheme() {
        _currentThemeDark.value = !_currentThemeDark.value
    }

    fun initiateThirdPartyLogin(provider: String, callbackEmail: String = "") {
        viewModelScope.launch {
            val username = when (provider) {
                "Google" -> "Gamer_" + UUID.randomUUID().toString().take(6)
                "Discord" -> "DiscordGamer_99"
                "Steam" -> "SteamLord_NP"
                else -> "Gamer_" + UUID.randomUUID().toString().take(6)
            }
            val emailStr = if (callbackEmail.isNotEmpty()) callbackEmail else "${username.lowercase()}@$provider.com"
            val id = "usr_${provider.lowercase()}_" + UUID.randomUUID().toString().take(6)

            val existing = repository.getUserByIdSync(id)
            if (existing != null) {
                _currentUser.value = existing
                _authState.value = AuthState.Authenticated(existing)
            } else {
                // Pre-configure balance with $250 for interactive preview
                val newUser = UserEntity(
                    userId = id,
                    username = username,
                    email = emailStr,
                    avatarUrl = "https://images.unsplash.com/photo-1566492031773-4f4e44671857?w=500&auto=format&fit=crop&q=60",
                    joinedTime = System.currentTimeMillis(),
                    walletBalance = 250.0,
                    bio = "Newly registered $provider buyer. Ready to trade."
                )
                repository.insertUser(newUser)
                _currentUser.value = newUser
                _authState.value = AuthState.Authenticated(newUser)
            }
            _currentScreen.value = "market_feed"
        }
    }

    fun startEmailSignup(email: String, username: String, passwordStr: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank() || username.isBlank() || passwordStr.isBlank()) {
                onResult(false, "Please fill all fields.")
                return@launch
            }
            val existing = repository.getUserByEmailSync(email.trim())
            if (existing != null) {
                onResult(false, "Email already registered.")
                return@launch
            }
            tempSignupUsername = username.trim()
            tempSignupEmail = email.trim()
            tempSignupPassword = passwordStr.trim()
            _authState.value = AuthState.ProfileSetup
            onResult(true, "Setup Your Gamer Profile")
        }
    }

    fun verifyOtp(otp: String) {
        viewModelScope.launch {
            // Automatically transitions to ProfileSetup
            _authState.value = AuthState.ProfileSetup
        }
    }

    fun loginWithEmailAndPassword(emailStr: String, passwordStr: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _loginError.value = null
            if (emailStr.isBlank() || passwordStr.isBlank()) {
                _loginError.value = "Please fill all fields."
                onResult(false, "Please fill all fields.")
                return@launch
            }
            val user = repository.getUserByEmailSync(emailStr.trim())
            if (user == null) {
                _loginError.value = "Account not found."
                onResult(false, "Account not found.")
            } else if (user.passwordHash != passwordStr) {
                _loginError.value = "Incorrect password."
                onResult(false, "Incorrect password.")
            } else {
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
                _currentScreen.value = "market_feed"
                onResult(true, "Authentication Successful")
            }
        }
    }

    fun submitProfileSetup(bio: String, discord: String, instagram: String) {
        viewModelScope.launch {
            val id = "usr_email_" + UUID.randomUUID().toString().take(6)
            val u = UserEntity(
                userId = id,
                username = if (tempSignupUsername.isNotEmpty()) tempSignupUsername else "Gamer_" + UUID.randomUUID().toString().take(4),
                email = if (tempSignupEmail.isNotEmpty()) tempSignupEmail else "user@test.com",
                avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=500&auto=format&fit=crop&q=60",
                joinedTime = System.currentTimeMillis(),
                walletBalance = 250.0,
                discordUrl = discord,
                instagramUrl = instagram,
                bio = bio.ifBlank { "Professional gamer & seller. Fast delivery guaranteed." },
                passwordHash = tempSignupPassword
            )
            repository.insertUser(u)
            _currentUser.value = u
            _authState.value = AuthState.Authenticated(u)
            _currentScreen.value = "market_feed"
        }
    }

    fun logout() {
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
        _currentScreen.value = "login"
    }

    // --- Marketplace Filters ---
    fun selectGameFilter(game: String) {
        _filterGame.value = game
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun applyFilters(region: String, platform: String, verifiedOnly: Boolean, maxPriceVal: Double) {
        _filterRegion.value = region
        _filterPlatform.value = platform
        _onlyVerified.value = verifiedOnly
        _maxPrice.value = maxPriceVal
    }

    fun resetFilters() {
        _filterGame.value = ""
        _searchQuery.value = ""
        _filterRegion.value = ""
        _filterPlatform.value = ""
        _onlyVerified.value = false
        _maxPrice.value = 500.0
    }

    // --- Navigation triggers ---
    fun navigateTo(screen: String) {
        if (screen == "admin" && _currentUser.value?.email?.equals("ranjghimire12@gmail.com", ignoreCase = true) != true) {
            // Secure gateway: Block unauthorized administration panel navigation
            return
        }
        _currentScreen.value = screen
    }

    fun viewListingDetail(listing: GameListing) {
        _selectedListing.value = listing
        _currentScreen.value = "detail"
    }

    fun viewUserProfile(userId: String) {
        viewModelScope.launch {
            val userProfile = repository.getUserByIdSync(userId)
            if (userProfile != null) {
                _viewingUser.value = userProfile
                _currentScreen.value = "profile"
            }
        }
    }

    private var chatPollJob: kotlinx.coroutines.Job? = null

    fun activeEscrowDeal(trade: EscrowTrade) {
        _selectedTrade.value = trade
        _activeTradeId.value = trade.tradeId
        _currentScreen.value = "escrow_deal"
        startChatPolling(trade.tradeId)
    }

    private fun startChatPolling(tradeId: String) {
        chatPollJob?.cancel()
        chatPollJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            while (currentScreen.value == "escrow_deal" && _activeTradeId.value == tradeId) {
                try {
                    // 1. Fetch remote chat messages from Firebase
                    val remoteMessages = FirebaseRealtimeDb.fetchMessages(tradeId)
                    if (remoteMessages.isNotEmpty()) {
                        for (remoteMsg in remoteMessages) {
                            val localExists = repository.hasMessageWithTextAndSender(tradeId, remoteMsg.senderId, remoteMsg.text)
                            if (!localExists) {
                                repository.insertMessage(remoteMsg)
                            }
                        }
                    }
                    
                    // 2. Fetch and synchronize peer WebRTC call state
                    val (fState, fCallerId) = FirebaseRealtimeDb.fetchCallState(tradeId)
                    if (_activeCallState.value != fState) {
                        _activeCallState.value = fState
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SajiloViewModel", "Chat polling error: ${e.message}")
                }
                kotlinx.coroutines.delay(2500)
            }
            // Reset active call status when exiting the chat room
            try {
                FirebaseRealtimeDb.updateCallState(tradeId, null, null)
            } catch (e: Exception) {}
            _activeCallState.value = null
        }
    }

    // --- Listing Upload ---
    fun postNewListing(
        gameType: String,
        title: String,
        price: Double,
        uid: String,
        gameUsername: String,
        region: String,
        platform: String,
        rank: String,
        level: Int,
        description: String,
        inventory: String,
        linkedMethod: String
    ) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val thumb = when (gameType) {
                "Free Fire" -> "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500&auto=format&fit=crop&q=60"
                "PUBG Mobile" -> "https://images.unsplash.com/photo-1553481187-be93c21490a9?w=500"
                "Valorant" -> "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500&auto=format&fit=crop&q=60"
                "Steam" -> "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=500"
                else -> "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=500"
            }

            val newListing = GameListing(
                gameType = gameType,
                title = title,
                price = price,
                sellerId = user.userId,
                sellerName = user.username,
                sellerRating = user.sellerRating,
                sellerTrust = user.trustScore,
                rank = rank,
                level = level,
                region = region,
                platform = platform,
                uid = uid,
                gameUsername = gameUsername,
                description = description,
                inventoryDetails = inventory,
                linkedMethod = linkedMethod,
                thumbnailUrl = thumb,
                screenshotUrls = thumb,
                isBoosted = user.subscriptionTier != "Free"
            )

            repository.insertListing(newListing)
            _currentScreen.value = "market_feed"
        }
    }


    // --- Escrow Anti-Scam State Machine ---
    fun buyListingAndLockEscrow(listing: GameListing) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            if (user.walletBalance < listing.price) {
                // Insufficient funds alert helper - or let UI show warning
                return@launch
            }

            // Deduct from buyer balance, increase locked escrow balance
            val updatedBuyer = user.copy(
                walletBalance = user.walletBalance - listing.price,
                escrowBalance = user.escrowBalance + listing.price
            )
            repository.updateUser(updatedBuyer)
            _currentUser.value = updatedBuyer

            // Create Escrow Trade Record
            val tradeId = "TRD_" + UUID.randomUUID().toString().take(6).uppercase()
            val trade = EscrowTrade(
                tradeId = tradeId,
                listingId = listing.id,
                gameType = listing.gameType,
                title = listing.title,
                price = listing.price,
                buyerId = user.userId,
                buyerName = user.username,
                sellerId = listing.sellerId,
                sellerName = listing.sellerName,
                status = "FUNDS_LOCKED"
            )
            repository.insertTrade(trade)

            // Auto write transaction logs
            repository.insertTransaction(
                WalletTransaction(
                    txId = "TX_LOCK_" + UUID.randomUUID().toString().take(6).uppercase(),
                    userId = user.userId,
                    type = "ESCROW_LOCK",
                    amount = listing.price,
                    status = "SUCCESS"
                )
            )

            // Auto add first chat system explanation messages to demonstrate the product's premium smart flow
            val systemMsg1 = ChatMessage(
                tradeId = tradeId,
                senderId = "SYSTEM",
                senderName = "Sajilo Shield Escrow",
                text = "🔐 SajiloShield Anti-Scam Shield: \$${listing.price} has been successfully locked in Escrow. Seller ${listing.sellerName} CANNOT access these funds until transfer confirmation. It is now safe to exchange details below."
            )
            repository.insertMessage(systemMsg1)
            FirebaseRealtimeDb.pushMessage(tradeId, "SYSTEM", "Sajilo Shield Escrow", systemMsg1.text)

            val systemMsg2 = ChatMessage(
                tradeId = tradeId,
                senderId = "SYSTEM",
                senderName = "Sajilo Shield Escrow",
                text = "ℹ️ SELLER INSTRUCTIONS: Drop your game account identifier and bind code details. BUYER INSTRUCTIONS: Verify matching level, inventory features. Click 'Confirm Account Received' to release payment when satisfied."
            )
            repository.insertMessage(systemMsg2)
            FirebaseRealtimeDb.pushMessage(tradeId, "SYSTEM", "Sajilo Shield Escrow", systemMsg2.text)

            // Go to trade screen
            activeEscrowDeal(trade)

            // Direct instant native Firebase push of the seller credentials so they are fetched organically via database polling
            val sellerCredMessageText = "Hello! Thank you for the trust. Here are the account bind credentials:\n\nUsername: ${listing.gameUsername}\nPlayer UID: ${listing.uid}\nBind login: ${listing.linkedMethod}"
            val sellerMsg = ChatMessage(
                tradeId = trade.tradeId,
                senderId = trade.sellerId,
                senderName = trade.sellerName,
                text = sellerCredMessageText
            )
            repository.insertMessage(sellerMsg)
            FirebaseRealtimeDb.pushMessage(trade.tradeId, trade.sellerId, trade.sellerName, sellerCredMessageText)
        }
    }

    fun sendChatMessage(text: String, imageUrl: String = "", voiceDuration: Int = 0) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val trade = _selectedTrade.value ?: return@launch

            val msg = ChatMessage(
                tradeId = trade.tradeId,
                senderId = user.userId,
                senderName = user.username,
                text = text,
                imageUrl = imageUrl,
                voiceDurationSec = voiceDuration
            )
            repository.insertMessage(msg)
            FirebaseRealtimeDb.pushMessage(trade.tradeId, user.userId, user.username, text, imageUrl)
        }
    }

    fun initiateWebRtcCall(type: String) { // "audio" or "video"
        val user = _currentUser.value ?: return
        val trade = _selectedTrade.value ?: return
        _activeCallState.value = "calling"
        FirebaseRealtimeDb.updateCallState(trade.tradeId, "calling", user.userId)
        
        viewModelScope.launch {
            kotlinx.coroutines.delay(1800)
            val state = if (type == "video") "active_video" else "active_audio"
            _activeCallState.value = state
            FirebaseRealtimeDb.updateCallState(trade.tradeId, state, user.userId)
        }
    }

    fun stopCall() {
        val trade = _selectedTrade.value ?: return
        _activeCallState.value = null
        FirebaseRealtimeDb.updateCallState(trade.tradeId, null, null)
    }

    fun triggerScreenShare() {
        val user = _currentUser.value ?: return
        val trade = _selectedTrade.value ?: return
        _activeCallState.value = "screen_share"
        FirebaseRealtimeDb.updateCallState(trade.tradeId, "screen_share", user.userId)
    }

    // --- Buyer side trade actions ---
    fun claimDispute(reason: String) {
        viewModelScope.launch {
            val trade = _selectedTrade.value ?: return@launch
            val updatedTrade = trade.copy(
                status = "DISPUTED",
                disputeReason = reason,
                reportedBy = trade.buyerName,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateTrade(updatedTrade)
            _selectedTrade.value = updatedTrade

            repository.insertMessage(
                ChatMessage(
                    tradeId = trade.tradeId,
                    senderId = "SYSTEM",
                    senderName = "Sajilo Dispute Center",
                    text = "🚨 DISPUTE FILED: Buyer reported a problem: '$reason'. Anti-scam investigation team is alerted. Escrow locks remain frozen. Do not try to share login credentials in unauthorized external apps."
                )
            )
        }
    }

    fun claimHalfPayment() {
        viewModelScope.launch {
            val trade = _selectedTrade.value ?: return@launch
            val half = trade.price / 2.0
            val user = _currentUser.value ?: return@launch

            val updatedTrade = trade.copy(
                status = "HALF_RELEASED",
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateTrade(updatedTrade)
            _selectedTrade.value = updatedTrade

            // Release half to seller, debit buyer escrow
            val updatedUser = user.copy(
                escrowBalance = (user.escrowBalance - half).coerceAtLeast(0.0)
            )
            repository.updateUser(updatedUser)
            _currentUser.value = updatedUser

            // Update seller in DB
            val sellerEntity = repository.getUserByIdSync(trade.sellerId)
            if (sellerEntity != null) {
                repository.updateUser(sellerEntity.copy(walletBalance = sellerEntity.walletBalance + half))
            }

            repository.insertTransaction(
                WalletTransaction(
                    txId = "TX_HALF_" + UUID.randomUUID().toString().take(6).uppercase(),
                    userId = user.userId,
                    type = "ESCROW_RELEASE",
                    amount = half,
                    status = "SUCCESS"
                )
            )

            repository.insertMessage(
                ChatMessage(
                    tradeId = trade.tradeId,
                    senderId = "SYSTEM",
                    senderName = "Sajilo Shield Escrow",
                    text = "⚠️ HALF PAYMENTS RELEASED: \$${half} is released to ${trade.sellerName}. Remaining \$${half} remains locked in Escrow until final verification completion."
                )
            )
        }
    }

    fun confirmWholeDealCompleted() {
        viewModelScope.launch {
            val trade = _selectedTrade.value ?: return@launch
            val user = _currentUser.value ?: return@launch
            val remainingAmt = if (trade.status == "HALF_RELEASED") trade.price / 2.0 else trade.price

            val updatedTrade = trade.copy(
                status = "COMPLETED",
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateTrade(updatedTrade)
            _selectedTrade.value = updatedTrade

            // Debit locked escrow
            val updatedUser = user.copy(
                escrowBalance = (user.escrowBalance - remainingAmt).coerceAtLeast(0.0)
            )
            repository.updateUser(updatedUser)
            _currentUser.value = updatedUser

            // Credit remaining to seller
            val sellerEntity = repository.getUserByIdSync(trade.sellerId)
            if (sellerEntity != null) {
                repository.updateUser(
                    sellerEntity.copy(
                        walletBalance = sellerEntity.walletBalance + remainingAmt,
                        completionPercentage = ((sellerEntity.completionPercentage * 4 + 100) / 5).coerceIn(0, 100)
                    )
                )
            }

            // Mark game listing as sold
            val listingFromDb = listings.value.find { it.id == trade.listingId }
            if (listingFromDb != null) {
                repository.updateListing(listingFromDb.copy(isSold = true))
            }

            repository.insertTransaction(
                WalletTransaction(
                    txId = "TX_FULL_" + UUID.randomUUID().toString().take(6).uppercase(),
                    userId = user.userId,
                    type = "ESCROW_RELEASE",
                    amount = remainingAmt,
                    status = "SUCCESS"
                )
            )

            repository.insertMessage(
                ChatMessage(
                    tradeId = trade.tradeId,
                    senderId = "SYSTEM",
                    senderName = "Sajilo Shield Escrow",
                    text = "🎊 CONGRATULATIONS: Secure trade completed! All funds (\$${trade.price}) have been released to the seller ${trade.sellerName}. Ratings options are now unlocked."
                )
            )
        }
    }

    fun submitRatingToSeller(ratingStars: Int, note: String) {
        viewModelScope.launch {
            val trade = _selectedTrade.value ?: return@launch
            val seller = repository.getUserByIdSync(trade.sellerId) ?: return@launch

            val newRating = (seller.sellerRating * 4 + ratingStars) / 5f
            val newTrustScore = (seller.trustScore + 1).coerceIn(0, 100)
            repository.updateUser(seller.copy(sellerRating = newRating, trustScore = newTrustScore))

            repository.insertMessage(
                ChatMessage(
                    tradeId = trade.tradeId,
                    senderId = "SYSTEM",
                    senderName = "Sajilo Shield Rating",
                    text = "⭐⭐ Rating submitted. Seller rating updated to ${String.format("%.2f", newRating)}/5 stars. Trust Score has increased."
                )
            )
            _currentScreen.value = "market_feed"
        }
    }

    // --- Wallet Funds System ---
    fun addFundsToWallet(amount: Double) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val u = user.copy(walletBalance = user.walletBalance + amount)
            repository.updateUser(u)
            _currentUser.value = u

            repository.insertTransaction(
                WalletTransaction(
                    txId = "TX_DEP_" + UUID.randomUUID().toString().take(6).uppercase(),
                    userId = user.userId,
                    type = "DEPOSIT",
                    amount = amount,
                    status = "SUCCESS"
                )
            )
        }
    }

    fun withdrawFundsFromWallet(amount: Double) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            if (user.walletBalance < amount) return@launch

            val u = user.copy(walletBalance = user.walletBalance - amount)
            repository.updateUser(u)
            _currentUser.value = u

            repository.insertTransaction(
                WalletTransaction(
                    txId = "TX_WTH_" + UUID.randomUUID().toString().take(6).uppercase(),
                    userId = user.userId,
                    type = "WITHDRAWAL",
                    amount = amount,
                    status = "SUCCESS"
                )
            )
        }
    }


    // --- Subscription Plan Buy ---
    fun buySubscription(plan: String, cost: Double) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            if (user.walletBalance < cost) return@launch

            val updatedUser = user.copy(
                walletBalance = user.walletBalance - cost,
                subscriptionTier = plan
            )
            repository.updateUser(updatedUser)
            _currentUser.value = updatedUser

            repository.insertTransaction(
                WalletTransaction(
                    txId = "TX_SUB_" + UUID.randomUUID().toString().take(6).uppercase(),
                    userId = user.userId,
                    type = "SUBSCRIPTION",
                    amount = cost,
                    status = "SUCCESS"
                )
            )
        }
    }


    // --- Admin Dashboard control actions ---
    fun setAdminSearchQuery(q: String) {
        _adminSearchQuery.value = q
    }

    fun banAndSuspendUser(userId: String) {
        viewModelScope.launch {
            val user = repository.getUserByIdSync(userId)
            if (user != null) {
                // Set trust and ratings to minimum and freeze them
                repository.updateUser(user.copy(trustScore = 0, completionPercentage = 0, bio = "⛔ Account Suspended for Suspicious Activity & Potential Scam Violation."))
            }
        }
    }

    fun verifySellersInAdmin(userId: String) {
        viewModelScope.launch {
            val user = repository.getUserByIdSync(userId)
            if (user != null) {
                repository.updateUser(user.copy(trustScore = 100))
            }
        }
    }

    fun pushAnnouncement(annText: String) {
        _announcement.value = annText
    }
}
