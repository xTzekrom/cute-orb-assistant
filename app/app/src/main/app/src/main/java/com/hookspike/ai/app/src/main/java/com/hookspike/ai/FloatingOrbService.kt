package com.hookspike.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

class FloatingOrbService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var orbContainer: FrameLayout
    private lateinit var removeTargetView: FrameLayout

    private var mediaPlayer: MediaPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val backendUrl = "https://hookspike-core.onrender.com"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            startNotificationSafe()
            setupRemoveTarget()
            setupFloatingOrb()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun startNotificationSafe() {
        val channelId = "hookspike_orb_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "HookSpike Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "HookSpike AI companion is active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("HookSpike AI")
            .setContentText("Orb companion is floating on screen")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1001, notification)
    }

    private fun setupRemoveTarget() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        removeTargetView = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E53935"))
            }
            background = bg

            val crossText = TextView(context).apply {
                text = "✕"
                textSize = 24f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            addView(crossText, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
            visibility = View.GONE
        }

        val targetParams = WindowManager.LayoutParams(
            160, 160,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 120
        }

        windowManager.addView(removeTargetView, targetParams)
    }

    private fun setupFloatingOrb() {
        orbContainer = FrameLayout(this).apply {
            val outerBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#18191E"))
                setStroke(3, Color.parseColor("#2C2D35"))
            }
            background = outerBg
        }

        val innerCoreDot = ImageView(this).apply {
            val innerBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FFFFFF"))
            }
            background = innerBg
        }

        val innerParams = FrameLayout.LayoutParams(38, 38).apply {
            gravity = Gravity.CENTER
        }
        orbContainer.addView(innerCoreDot, innerParams)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val orbParams = WindowManager.LayoutParams(
            135, 135,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 600
            y = 800
        }

        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var touchStartTime = 0L

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        orbContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = orbParams.x
                    initialY = orbParams.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    touchStartTime = System.currentTimeMillis()
                    removeTargetView.visibility = View.VISIBLE
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    orbParams.x = initialX + (event.rawX - touchStartX).toInt()
                    orbParams.y = initialY + (event.rawY - touchStartY).toInt()
                    windowManager.updateViewLayout(orbContainer, orbParams)

                    val targetX = screenWidth / 2
                    val targetY = screenHeight - 160
                    val dist = hypot((event.rawX - targetX).toDouble(), (event.rawY - targetY).toDouble())
                    if (dist < 180) {
                        removeTargetView.alpha = 1.0f
                        removeTargetView.scaleX = 1.25f
                        removeTargetView.scaleY = 1.25f
                    } else {
                        removeTargetView.alpha = 0.65f
                        removeTargetView.scaleX = 1.0f
                        removeTargetView.scaleY = 1.0f
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    removeTargetView.visibility = View.GONE
                    val duration = System.currentTimeMillis() - touchStartTime
                    val diffX = abs(event.rawX - touchStartX)
                    val diffY = abs(event.rawY - touchStartY)

                    val targetX = screenWidth / 2
                    val targetY = screenHeight - 160
                    val dist = hypot((event.rawX - targetX).toDouble(), (event.rawY - targetY).toDouble())
                    if (dist < 180) {
                        stopSelf()
                        return@setOnTouchListener true
                    }

                    if (diffX < 15 && diffY < 15 && duration < 350) {
                        triggerOrbInteraction()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(orbContainer, orbParams)
    }

    private fun triggerOrbInteraction() {
        serviceScope.launch {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource("$backendUrl/api/orb-greeting")
                    prepareAsync()
                    setOnPreparedListener { start() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (::orbContainer.isInitialized) windowManager.removeView(orbContainer)
        if (::removeTargetView.isInitialized) windowManager.removeView(removeTargetView)
        mediaPlayer?.release()
    }
}
