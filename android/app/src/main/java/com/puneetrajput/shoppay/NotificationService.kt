package com.puneetrajput.shoppay

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class NotificationService : NotificationListenerService() {

    private val TAG = "UPI_LISTENER"
    // TODO: Replace with your actual Cloud Function URL
    private val BACKEND_URL = "https://us-central1-tv-upi-mvp.cloudfunctions.net/api"
    private val client = OkHttpClient()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getString(Notification.EXTRA_TEXT)

        Log.d(TAG, "Notification from $packageName: $title - $text")

        // Filter for UPI Apps (PhonePe, GPay, Paytm)
        // PhonePe often uses "com.phonepe.app"
        // GPay uses "com.google.android.apps.nbu.paisa.user"
        // Paytm uses "net.one97.paytm"
        
        if (text != null && isPaymentNotification(text)) {
            val (amount, sender) = parsePaymentText(text)
            if (amount != null && sender != null) {
                sendToBackend(amount, sender)
            }
        }
    }

    private fun isPaymentNotification(text: String): Boolean {
        // Broad check for payment keywords
        return text.contains("Received", ignoreCase = true) || 
               text.contains("Sent", ignoreCase = true) || // Optional: handle sent too? User said "Received"
               text.contains("Paid", ignoreCase = true)
    }

    private fun parsePaymentText(text: String): Pair<String?, String?> {
        // Example: "Received Rs. 850 from Rahul Sharma"
        // Regex is tricky across apps, so using a robust MVP approach
        
        try {
            // Find Amount (Rs. 100 or ₹100)
            val amountRegex = Regex("[₹|Rs\\.?]\\s*(\\d+(\\.\\d{2})?)")
            val amountMatch = amountRegex.find(text)
            val amount = amountMatch?.groupValues?.get(1)

            // Find Sender (Strategy: "from X" or "to X")
            // For received: "from <Name>"
            val fromRegex = Regex("from\\s+([a-zA-Z\\s]+)")
            val fromMatch = fromRegex.find(text)
            var sender = fromMatch?.groupValues?.get(1)?.trim()

            // Cleanup sender if it captured too much
            if (sender != null && sender.contains(" on ")) {
                sender = sender.split(" on ")[0]
            }

            return Pair(amount, sender)
        } catch (e: Exception) {
            Log.e(TAG, "Parsing error: ${e.message}")
            return Pair(null, null)
        }
    }

    private fun sendToBackend(amount: String, sender: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject()
                json.put("amount", amount)
                json.put("sender", sender)

                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("https://hbhtnipvpovpunbtfsrg.supabase.co/rest/v1/tv_notifications")
                    .post(body)
                    .addHeader("apikey", "sb_publishable_6xpTL4QXsMsSFiPYBq6xqw_YXP_pv9t")
                    .addHeader("Authorization", "Bearer sb_publishable_6xpTL4QXsMsSFiPYBq6xqw_YXP_pv9t")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Supabase error: ${response.code}")
                    } else {
                        Log.i(TAG, "Event sent to Supabase successfully!")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error: ${e.message}")
            }
        }
    }
}
