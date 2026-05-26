package com.example.data

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object FirebaseRealtimeDb {
    private const val TAG = "FirebaseRealtimeDb"
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // Push message to ronak-1a7f2 Realtime Database under: /chats/{tradeId}/messages
    fun pushMessage(
        tradeId: String,
        senderId: String,
        senderName: String,
        text: String,
        imageUrl: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        val sanitizedTradeId = tradeId.replace(".", "_").replace("#", "_").replace("$", "_")
        val url = "${FirebaseConfig.DATABASE_URL}/chats/$sanitizedTradeId/messages.json"
        
        val jsonPayload = JSONObject().apply {
            put("tradeId", tradeId)
            put("senderId", senderId)
            put("senderName", senderName)
            put("text", text)
            put("imageUrl", imageUrl)
            put("videoUrl", "")
            put("voiceDurationSec", 0)
            put("timestamp", timestamp)
            put("isSeen", false)
        }.toString()

        val body = jsonPayload.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Failed to push message to Firebase: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful) {
                    Log.e(TAG, "Error matching Firebase Response Code: ${response.code}")
                } else {
                    Log.d(TAG, "Successfully pushed message to Firebase: ${response.body?.string()}")
                }
                response.close()
            }
        })
    }

    // Fetch messages synchronously on IO Dispatcher for /chats/{tradeId}/messages
    fun fetchMessages(tradeId: String): List<ChatMessage> {
        val sanitizedTradeId = tradeId.replace(".", "_").replace("#", "_").replace("$", "_")
        val url = "${FirebaseConfig.DATABASE_URL}/chats/$sanitizedTradeId/messages.json"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val list = mutableListOf<ChatMessage>()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString != "null" && bodyString.isNotEmpty()) {
                        val jsonObject = JSONObject(bodyString)
                        val keys = jsonObject.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val msgObj = jsonObject.getJSONObject(key)
                            val mId = timestampToLongId(msgObj.optLong("timestamp"))
                            list.add(
                                ChatMessage(
                                    messageId = mId,
                                    tradeId = msgObj.optString("tradeId", tradeId),
                                    senderId = msgObj.optString("senderId", ""),
                                    senderName = msgObj.optString("senderName", ""),
                                    text = msgObj.optString("text", ""),
                                    imageUrl = msgObj.optString("imageUrl", ""),
                                    videoUrl = msgObj.optString("videoUrl", ""),
                                    voiceDurationSec = msgObj.optInt("voiceDurationSec", 0),
                                    timestamp = msgObj.optLong("timestamp", System.currentTimeMillis()),
                                    isSeen = msgObj.optBoolean("isSeen", false)
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching messages from Firebase: ${e.message}")
        }
        return list.sortedBy { it.timestamp }
    }

    // Set WebRTC Call State on Firebase endpoint: /chats/{tradeId}/callState.json
    fun updateCallState(tradeId: String, callState: String?, callerId: String?) {
        val sanitizedTradeId = tradeId.replace(".", "_").replace("#", "_").replace("$", "_")
        val url = "${FirebaseConfig.DATABASE_URL}/chats/$sanitizedTradeId/callState.json"

        val jsonPayload = JSONObject().apply {
            put("state", callState ?: JSONObject.NULL)
            put("callerId", callerId ?: JSONObject.NULL)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        val body = jsonPayload.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .put(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "Failed to update call state on Firebase: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.close()
            }
        })
    }

    // Fetch WebRTC Call State returning Pair of status & callerId
    fun fetchCallState(tradeId: String): Pair<String?, String?> {
        val sanitizedTradeId = tradeId.replace(".", "_").replace("#", "_").replace("$", "_")
        val url = "${FirebaseConfig.DATABASE_URL}/chats/$sanitizedTradeId/callState.json"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString != "null" && bodyString.isNotEmpty()) {
                        val obj = JSONObject(bodyString)
                        val state = if (obj.isNull("state")) null else obj.optString("state", null)
                        val callerId = if (obj.isNull("callerId")) null else obj.optString("callerId", null)
                        return state to callerId
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching call state: ${e.message}")
        }
        return null to null
    }

    private fun timestampToLongId(timestamp: Long): Long {
        // Return a stable long ID based on timestamp and simple random bits
        return timestamp + (1..100).random()
    }
}
