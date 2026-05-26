package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val email: String,
    val avatarUrl: String,
    val joinedTime: Long,
    val trustScore: Int = 100,
    val completionPercentage: Int = 100,
    val sellerRating: Float = 5.0f,
    val buyerRating: Float = 5.0f,
    val bio: String = "Premium Pro gamer & seller. Fast delivery guaranteed.",
    val walletBalance: Double = 250.0, // Pre-funded with $250 for testing
    val escrowBalance: Double = 0.0,
    val subscriptionTier: String = "Free", // Free, Bronze, Silver, Gold, Verified Pro
    val discordUrl: String = "",
    val steamUrl: String = "",
    val instagramUrl: String = "",
    val tiktokUrl: String = "",
    val facebookUrl: String = "",
    val isOnline: Boolean = true,
    val lastActive: String = "Online",
    val passwordHash: String = ""
)

@Entity(tableName = "game_listings")
data class GameListing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameType: String, // Free Fire, PUBG Mobile, Steam, Valorant, Fortnite, Clash of Clans
    val title: String,
    val price: Double,
    val sellerId: String,
    val sellerName: String,
    val sellerRating: Float = 4.8f,
    val sellerTrust: Int = 98,
    val rank: String,
    val level: Int,
    val region: String,
    val platform: String, // Mobile, PC, Console
    val uid: String,
    val gameUsername: String,
    val description: String,
    val inventoryDetails: String,
    val linkedMethod: String, // Gmail, Facebook, Twitter, Steam
    val thumbnailUrl: String,
    val screenshotUrls: String, // Comma separated mock image styles
    val isBoosted: Boolean = false,
    val isSold: Boolean = false,
    val verifiedSeller: Boolean = true
)

@Entity(tableName = "escrow_trades")
data class EscrowTrade(
    @PrimaryKey val tradeId: String,
    val listingId: Long,
    val gameType: String,
    val title: String,
    val price: Double,
    val buyerId: String,
    val buyerName: String,
    val sellerId: String,
    val sellerName: String,
    val status: String, // AWAITING_DEPOSIT, FUNDS_LOCKED, TRANSFERRING, HALF_RELEASED, COMPLETED, DISPUTED, CANCELLED
    val disputeReason: String = "",
    val reportedBy: String = "",
    val depositTime: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val messageId: Long = 0,
    val tradeId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val imageUrl: String = "",
    val videoUrl: String = "",
    val voiceDurationSec: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isSeen: Boolean = false
)

@Entity(tableName = "wallet_transactions")
data class WalletTransaction(
    @PrimaryKey val txId: String,
    val userId: String,
    val type: String, // DEPOSIT, WITHDRAWAL, ESCROW_LOCK, ESCROW_RELEASE, ESCROW_REFUND, SUBSCRIPTION
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String // SUCCESS, PENDING, FAILED
)
