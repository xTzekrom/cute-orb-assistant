package com.hookspike.ai

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
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val overlayPermissionReqCode = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Programmatic UI without needing layout XML
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#0D0F12"))
            setPadding(40, 40, 40, 40)
        }

        val titleView = TextView(this).apply {
            text = "HookSpike AI"
            textSize = 28f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        val descView = TextView(this).apply {
            text = "Tap below to launch or restore the background floating orb."
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#8E8E93"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 60)
        }

        val toggleOrbButton = Button(this).apply {
            text = "Launch / Restore Floating Orb"
            setBackgroundColor(android.graphics.Color.parseColor("#2563EB"))
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
            setPadding(30, 20, 30, 20)
            setOnClickListener {
                checkPermissionAndStartService()
            }
        }

        rootLayout.addView(titleView)
        rootLayout.addView(descView)
        rootLayout.addView(toggleOrbButton)

        setContentView(rootLayout)

        checkPermissionAndStartService()
    }

    private fun checkPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant overlay permission for HookSpike", Toast.LENGTH_LONG).show()
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
        val intent = Intent(this, FloatingOrbService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "HookSpike Orb Active!", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == overlayPermissionReqCode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startOrbService()
                } else {
                    Toast.makeText(this, "Permission denied. Overlay cannot be shown.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
