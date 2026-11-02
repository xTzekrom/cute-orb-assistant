package com.hookspike.ai

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val overlayPermissionReqCode = 1234
    private lateinit var statusLogView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Built-in crash catcher: Shows exact error on screen instead of closing app
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            runOnUiThread {
                statusLogView.setTextColor(Color.parseColor("#FF5252"))
                statusLogView.text = "CRASH DETECTED:\n${throwable.localizedMessage}\n\nTrace:\n${throwable.stackTraceToString()}"
            }
        }

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0D0F12"))
            isFillViewport = true
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(40, 60, 40, 40)
        }

        val titleView = TextView(this).apply {
            text = "HookSpike AI"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val descView = TextView(this).apply {
            text = "Native Draggable Orb Companion\nRedmi / Android 10 Optimized"
            textSize = 14f
            setTextColor(Color.parseColor("#8E8E93"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        val launchButton = Button(this).apply {
            text = "START FLOATING ORB"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(30, 20, 30, 20)
            setOnClickListener {
                checkPermissionAndLaunch()
            }
        }

        statusLogView = TextView(this).apply {
            text = "Status: App initialized successfully.\nTap button above to show overlay."
            textSize = 13f
            setTextColor(Color.parseColor("#4CAF50"))
            setPadding(20, 40, 20, 20)
            gravity = Gravity.START
        }

        rootLayout.addView(titleView)
        rootLayout.addView(descView)
        rootLayout.addView(launchButton)
        rootLayout.addView(statusLogView)

        scrollView.addView(rootLayout)
        setContentView(scrollView)
    }

    private fun checkPermissionAndLaunch() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    statusLogView.text = "Status: Overlay permission required.\nOpening device settings..."
                    Toast.makeText(this, "Allow 'Display over other apps'", Toast.LENGTH_LONG).show()
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, overlayPermissionReqCode)
                    return
                }
            }
            startOrbServiceSafe()
        } catch (e: Exception) {
            statusLogView.setTextColor(Color.parseColor("#FF5252"))
            statusLogView.text = "Permission Error: ${e.message}"
        }
    }

    private fun startOrbServiceSafe() {
        try {
            val intent = Intent(this, FloatingOrbService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            statusLogView.setTextColor(Color.parseColor("#4CAF50"))
            statusLogView.text = "Status: Floating Orb Service Active!\nCheck your screen for the orb."
            Toast.makeText(this, "HookSpike Orb Running!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            statusLogView.setTextColor(Color.parseColor("#FF5252"))
            statusLogView.text = "Service Launch Error:\n${e.message}"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == overlayPermissionReqCode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startOrbServiceSafe()
            } else {
                statusLogView.setTextColor(Color.parseColor("#FFB74D"))
                statusLogView.text = "Status: Permission not granted. Cannot float orb."
            }
        }
    }
}
