package com.hookspike.ai

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

// Standard Activity avoids all AppCompat theme crashes
class MainActivity : Activity() {

    private val overlayPermissionReqCode = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#0D0F12"))
            setPadding(50, 50, 50, 50)
        }

        val titleView = TextView(this).apply {
            text = "HookSpike AI"
            textSize = 28f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        val descView = TextView(this).apply {
            text = "Floating Companion Orb\nTap below to activate or manage overlay permission."
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#8E8E93"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }

        val toggleOrbButton = Button(this).apply {
            text = "Start Floating Orb"
            setBackgroundColor(android.graphics.Color.parseColor("#2563EB"))
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
            setPadding(40, 25, 40, 25)
            setOnClickListener {
                checkAndStart()
            }
        }

        rootLayout.addView(titleView)
        rootLayout.addView(descView)
        rootLayout.addView(toggleOrbButton)

        setContentView(rootLayout)
    }

    private fun checkAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Enable 'Display over other apps' for HookSpike", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, overlayPermissionReqCode)
                return
            }
        }
        startOrbService()
    }

    private fun startOrbService() {
        try {
            val intent = Intent(this, FloatingOrbService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "HookSpike Orb Activated!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Service start error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == overlayPermissionReqCode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startOrbService()
            } else {
                Toast.makeText(this, "Permission required to show orb", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
