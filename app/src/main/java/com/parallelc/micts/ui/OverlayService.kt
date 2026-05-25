package com.parallelc.micts.ui

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.parallelc.micts.R
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.ui.activity.triggerCircleToSearch

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var leftView: View? = null
    private var rightView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isLongPressing = false

    private val longPressRunnable = Runnable {
        if (isLongPressing) {
            triggerCTS()
            isLongPressing = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundService()
        updateOverlays()
    }

    private fun startForegroundService() {
        val channelId = "overlay_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Corner Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Corner Overlay is active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "UPDATE_CONFIG") {
            updateOverlays()
        }
        return START_STICKY
    }

    private fun updateOverlays() {
        removeOverlays()

        val prefs = getSharedPreferences(AppConfig.CONFIG_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(AppConfig.KEY_OVERLAY_ENABLED, false)
        if (!isEnabled) {
            stopSelf()
            return
        }

        val corners = prefs.getInt(AppConfig.KEY_OVERLAY_CORNERS, 2)
        val widthDp = prefs.getInt(AppConfig.KEY_OVERLAY_WIDTH, 50)
        val heightDp = prefs.getInt(AppConfig.KEY_OVERLAY_HEIGHT, 50)
        val opacity = prefs.getInt(AppConfig.KEY_OVERLAY_OPACITY, 50)

        val density = resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt()
        val heightPx = (heightDp * density).toInt()

        if (corners == 0 || corners == 2) {
            leftView = createOverlayView(widthPx, heightPx, opacity, Gravity.BOTTOM or Gravity.LEFT)
        }
        if (corners == 1 || corners == 2) {
            rightView = createOverlayView(widthPx, heightPx, opacity, Gravity.BOTTOM or Gravity.RIGHT)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView(width: Int, height: Int, opacity: Int, gravity: Int): View {
        val view = View(this)
        // Background color logic: black with variable alpha
        val alpha = (opacity / 100f * 255).toInt()
        view.setBackgroundColor(Color.argb(alpha, 0, 0, 0))

        val params = WindowManager.LayoutParams(
            width,
            height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = gravity

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isLongPressing = true
                    val prefs = getSharedPreferences(AppConfig.CONFIG_NAME, Context.MODE_PRIVATE)
                    val delay = prefs.getLong(AppConfig.KEY_OVERLAY_DELAY, 500L)
                    handler.postDelayed(longPressRunnable, delay)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isLongPressing = false
                    handler.removeCallbacks(longPressRunnable)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, params)
        return view
    }

    private fun triggerCTS() {
        val intent = Intent(this, com.parallelc.micts.ui.activity.TriggerActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
    }

    private fun removeOverlays() {
        leftView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        rightView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        leftView = null
        rightView = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Recreate the overlays to handle orientation changes natively
        updateOverlays()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlays()
    }
}
