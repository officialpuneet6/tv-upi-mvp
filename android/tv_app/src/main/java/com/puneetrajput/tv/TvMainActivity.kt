package com.puneetrajput.tv

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TvMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Simple UI
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.gravity = android.view.Gravity.CENTER
        layout.setPadding(50, 50, 50, 50)

        val title = TextView(this)
        title.text = "UPI TV Receiver (Overlay Mode)"
        title.textSize = 24f
        layout.addView(title)

        val btnPerm = Button(this)
        btnPerm.text = "Grant Overlay Permission"
        layout.addView(btnPerm)

        val btnStart = Button(this)
        btnStart.text = "Start Background Service"
        layout.addView(btnStart)
        
        val btnTest = Button(this)
        btnTest.text = "Test Overlay"
        layout.addView(btnTest)

        setContentView(layout)

        // Actions
        btnPerm.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, 101)
            } else {
                Toast.makeText(this, "Permission already granted!", Toast.LENGTH_SHORT).show()
            }
        }

        btnStart.setOnClickListener {
            val intent = Intent(this, TvOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent) // Ideally needs notification
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show()
        }
        
        btnTest.setOnClickListener {
             // Visual Feedback
             Toast.makeText(this, "Simulating Alert...", Toast.LENGTH_SHORT).show()
             
             // Trigger via Intent (Service handles onStartCommand usually, but we haven't overridden it yet)
             // Let's rely on the Real Flow (Polling) we just built.
             // OR, to really force it:
             val intent = Intent(this, TvOverlayService::class.java)
             intent.action = "TEST_OVERLAY"
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 startForegroundService(intent)
             } else {
                 startService(intent)
             }
        }
    }
}
