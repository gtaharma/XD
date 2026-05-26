package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SajiloRepository(private val dao: SajiloDao) {

    val allListings: Flow<List<GameListing>> = dao.getAllListings()
    val allTrades: Flow<List<EscrowTrade>> = dao.getAllTrades()
    val allTransactions: Flow<List<WalletTransaction>> = dao.getAllTransactions()
    val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()

    fun getUserById(userId: String): Flow<UserEntity?> = dao.getUserById(userId)
    suspend fun getUserByIdSync(userId: String): UserEntity? = dao.getUserByIdSync(userId)
    suspend fun getUserByEmailSync(email: String): UserEntity? = dao.getUserByEmailSync(email)
    fun getListingById(id: Long): Flow<GameListing?> = dao.getListingById(id)
    fun getMessagesForTrade(tradeId: String): Flow<List<ChatMessage>> = dao.getMessagesForTrade(tradeId)
    fun getTransactionsForUser(userId: String): Flow<List<WalletTransaction>> = dao.getTransactionsForUser(userId)

    suspend fun insertUser(user: UserEntity) = dao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = dao.updateUser(user)

    suspend fun insertListing(listing: GameListing): Long = dao.insertListing(listing)
    suspend fun updateListing(listing: GameListing) = dao.updateListing(listing)
    suspend fun deleteListing(listing: GameListing) = dao.deleteListing(listing)

    suspend fun insertTrade(trade: EscrowTrade) = dao.insertTrade(trade)
    suspend fun updateTrade(trade: EscrowTrade) = dao.updateTrade(trade)

    suspend fun insertMessage(message: ChatMessage) = dao.insertMessage(message)
    suspend fun hasMessageWithTextAndSender(tradeId: String, senderId: String, text: String): Boolean = dao.hasMessage(tradeId, senderId, text)
    suspend fun insertTransaction(tx: WalletTransaction) = dao.insertTransaction(tx)

    suspend fun populateInitialData() {
        // Mockup seed listings and users are completely deleted to fit the 'No Mockup data any more' requirement.
        // Users will register real accounts and upload raw listings via database/Firebase.
    }
}
