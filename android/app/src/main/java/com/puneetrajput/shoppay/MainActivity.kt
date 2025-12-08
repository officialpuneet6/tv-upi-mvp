package com.puneetrajput.shoppay

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    private val ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Simple programmatic UI to avoid XML for MVP speed, using layout params if needed, 
        // but for simplicity in this generated code, I'll assume a basic layout or just set content view to a programmatic layout.
        // To be safe and dependency-free (no XML needed), I'll use a simple linear layout programmatically.
        
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.gravity = android.view.Gravity.CENTER
        layout.setPadding(50, 50, 50, 50)
        
        val title = TextView(this)
        title.text = "UPI TV Alert"
        title.textSize = 24f
        layout.addView(title)
        
        val status = TextView(this)
        status.text = "Status: Checking..."
        status.textSize = 18f
        status.setPadding(0, 20, 0, 50)
        layout.addView(status)

        val btnPermission = Button(this)
        btnPermission.text = "Grant Notification Permission"
        layout.addView(btnPermission)

        val btnTest = Button(this)
        btnTest.text = "Send Test Alert"
        layout.addView(btnTest)

        setContentView(layout)

        // Logic
        if (!isNotificationServiceEnabled()) {
            status.text = "Status: Permission Missing ❌"
            buildNotificationServiceAlertDialog().show()
        } else {
            status.text = "Status: Ready ✅"
        }

        btnPermission.setOnClickListener {
            startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnTest.setOnClickListener {
            Toast.makeText(this, "Wait for a real UPI notification!", Toast.LENGTH_SHORT).show()
            // In a real app, we might force a broadcast, but for MVP, just user feedback.
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, ENABLED_NOTIFICATION_LISTENERS)
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                    return true
                }
            }
        }
        return false
    }

    private fun buildNotificationServiceAlertDialog(): AlertDialog.Builder {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Notification Listener Service")
        alertDialogBuilder.setMessage("For the app to work, please enable the Notification Listener Service.")
        alertDialogBuilder.setPositiveButton("Settings") { _, _ ->
            startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        alertDialogBuilder.setNegativeButton("Cancel") { _, _ ->
            // Do nothing
        }
        return alertDialogBuilder
    }
}
