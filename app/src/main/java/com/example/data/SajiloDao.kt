package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SajiloDao {

    // --- User Queries ---
    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserByIdSync(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmailSync(email: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)


    // --- Game Listing Queries ---
    @Query("SELECT * FROM game_listings ORDER BY id DESC")
    fun getAllListings(): Flow<List<GameListing>>

    @Query("SELECT * FROM game_listings WHERE id = :id")
    fun getListingById(id: Long): Flow<GameListing?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: GameListing): Long

    @Update
    suspend fun updateListing(listing: GameListing)

    @Delete
    suspend fun deleteListing(listing: GameListing)


    // --- Escrow Trade Queries ---
    @Query("SELECT * FROM escrow_trades ORDER BY lastUpdated DESC")
    fun getAllTrades(): Flow<List<EscrowTrade>>

    @Query("SELECT * FROM escrow_trades WHERE tradeId = :tradeId")
    fun getTradeById(tradeId: String): Flow<EscrowTrade?>

    @Query("SELECT * FROM escrow_trades WHERE tradeId = :tradeId")
    suspend fun getTradeByIdSync(tradeId: String): EscrowTrade?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: EscrowTrade)

    @Update
    suspend fun updateTrade(trade: EscrowTrade)


    // --- Chat Message Queries ---
    @Query("SELECT * FROM chat_messages WHERE tradeId = :tradeId ORDER BY timestamp ASC")
    fun getMessagesForTrade(tradeId: String): Flow<List<ChatMessage>>

    @Query("SELECT EXISTS(SELECT 1 FROM chat_messages WHERE tradeId = :tradeId AND senderId = :senderId AND text = :text)")
    suspend fun hasMessage(tradeId: String, senderId: String, text: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)


    // --- Wallet Transactions Queries ---
    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsForUser(userId: String): Flow<List<WalletTransaction>>

    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<WalletTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: WalletTransaction)
}
