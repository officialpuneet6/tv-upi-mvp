package com.puneetrajput.tv

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class TvOverlayService : Service(), TextToSpeech.OnInitListener {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var tts: TextToSpeech? = null
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var lastEventTime = 0L

    // Cloud Function to Poll (Simple Polling for MVP instead of Firestore SDK to keep APK small)
    // In Production: Use Firebase SDK. For MVP: Polling every 3s is easier to implement without heavy dependencies.
    // NOTE: Switched to Polling for "Fastest Possible MVP" on TV without Play Services dependencies if possible.
    // If user has Play Services, Firestore SDK is better. Let's assume standard Firestore SDK is hard to setup in 1 file.
    // actually, let's stick to the Plan: Firestore SDK logic is simpler if we assume dependencies work.
    // BUT to avoid "Multi-dex" and huge build issues in this generated code, I will use a simple HTTP GET Polling to a specialized endpoint if possible.
    // However, I only built a POST endpoint.
    // Let's use the standard Firestore REST API for polling to avoid complex JARs? No, let's just mock the listening logic for now
    // or better: Use a simple recursive Fetch.
    
    // DECISION: I will implement a placeholder "Polling" logic that simulates listening. 
    // To make it REAL, we need the Firestore SDK. I'll add the imports but keep logic simple.
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        tts = TextToSpeech(this, this)
        
        Log.d("TV_OVERLAY", "Service Started")
        startMockListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "TEST_OVERLAY") {
             showPaymentAlert("10.00", "Test User")
        }
        return START_STICKY
    }

    private fun startMockListener() {
        // Real Polling from Cloud Function
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkForNotifications()
                handler.postDelayed(this, 3000) // Poll every 3 seconds
            }
        }, 3000)
    }

    private fun checkForNotifications() {
        // Query Supabase for the most recent notification from the last minute
        val oneMinuteAgo = System.currentTimeMillis() - 60000
        val isoTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date(oneMinuteAgo))
        
        val request = Request.Builder()
            .url("https://hbhtnipvpovpunbtfsrg.supabase.co/rest/v1/tv_notifications?select=*&created_at=gte.$isoTime&order=created_at.desc&limit=1")
            .get()
            .addHeader("apikey", "sb_publishable_6xpTL4QXsMsSFiPYBq6xqw_YXP_pv9t")
            .addHeader("Authorization", "Bearer sb_publishable_6xpTL4QXsMsSFiPYBq6xqw_YXP_pv9t")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TV_POLL", "Failed to fetch: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    if (responseData != null) {
                        try {
                            val jsonArray = org.json.JSONArray(responseData)
                            if (jsonArray.length() > 0) {
                                val json = jsonArray.getJSONObject(0)
                                val id = json.optString("id")
                                
                                if (id != lastId) {
                                    lastId = id
                                    val amount = json.optString("amount")
                                    val sender = json.optString("sender")
                                    
                                    handler.post {
                                        showPaymentAlert(amount, sender)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("TV_POLL", "JSON Error", e)
                        }
                    }
                }
            }
        })
    }

    private var lastId = ""

    // Public method to trigger alert (can be called via BroadcastReceiver)
    fun showPaymentAlert(amount: String, sender: String) {
        handler.post {
            showOverlay(amount, sender)
            speak("Received rupees $amount from $sender")
        }
    }

    private fun showOverlay(amount: String, sender: String) {
        if (floatingView != null) return // Already showing

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_overlay, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.END
        params.x = 50
        params.y = 50

        // Set Data
        val txtAmount = floatingView?.findViewById<TextView>(R.id.tvAmount)
        val txtSender = floatingView?.findViewById<TextView>(R.id.tvSender)
        txtAmount?.text = "₹$amount"
        txtSender?.text = "From: $sender"

        try {
            windowManager?.addView(floatingView, params)
            
            // Auto Hide after 5 sec
            handler.postDelayed({
                removeOverlay()
            }, 6000)
        } catch (e: Exception) {
            Log.e("TV_OVERLAY", "Error showing overlay", e)
        }
    }

    private fun removeOverlay() {
        if (floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            floatingView = null
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) windowManager?.removeView(floatingView)
        tts?.shutdown()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
