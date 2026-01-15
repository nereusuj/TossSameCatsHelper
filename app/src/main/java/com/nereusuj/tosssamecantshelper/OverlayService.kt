package com.nereusuj.tosssamecantshelper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var controlsView: View
    private lateinit var resultViewContainer: View
    private lateinit var resultView: ResultView
    
    private lateinit var screenCaptureManager: ScreenCaptureManager
    private val imageAnalyzer = ImageAnalyzer()
    
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private var currentGridId: Int = 0

    private var rules = mapOf(
        R.id.btn_2x2 to Pair(2, 2),
        R.id.btn_3x2 to Pair(3, 2),
        R.id.btn_4x2 to Pair(4, 2),
        R.id.btn_4x3 to Pair(4, 3),
        R.id.btn_4x4 to Pair(4, 4),
        R.id.btn_5x4 to Pair(5, 4),
        R.id.btn_6x4 to Pair(6, 4),
        R.id.btn_6x5 to Pair(6, 5)
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        screenCaptureManager = ScreenCaptureManager(this)

        initControlsView()
        initResultView()
    }

    private fun startForegroundService() {
        val channelId = "OverlayServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "OverlayServiceChannel",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_content))
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)
    }

    private fun initControlsView() {
        controlsView = LayoutInflater.from(this).inflate(R.layout.layout_overlay_controls, null)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 100 // Margin from top

        // Setup Buttons
        rules.forEach { (id, grid) ->
            controlsView.findViewById<Button>(id).setOnClickListener {
                currentGridId = id
                startAnalysis(grid.first, grid.second)
            }
        }

        controlsView.findViewById<Button>(R.id.btn_exit).setOnClickListener {
            stopSelf()
        }

        controlsView.findViewById<Button>(R.id.btn_stop).setOnClickListener {
            stopAnalysis()
        }

        windowManager.addView(controlsView, params)
    }

    private fun initResultView() {
        resultViewContainer = LayoutInflater.from(this).inflate(R.layout.layout_overlay_result, null)
        
        val container = resultViewContainer.findViewById<FrameLayout>(R.id.container_result)
        resultView = ResultView(this)
        container.addView(resultView)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, // Draw over status bar
            PixelFormat.TRANSLUCENT
        )
        // Initially not added or invisible. Better to add and set GONE.
        // Or add only when needed. Lets add only when needed to save resources/touch events.
        
        // ResultView initialized
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val resultCode = intent.getIntExtra("EXTRA_RESULT_CODE", 0)
            val data = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra("EXTRA_DATA", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("EXTRA_DATA")
            }
            
            if (resultCode != 0 && data != null) {
                screenCaptureManager.start(resultCode, data)
            }
        }
        return START_STICKY
    }

    private fun startAnalysis(rows: Int, cols: Int) {
        serviceScope.launch {
            // Hide controls
            controlsView.visibility = View.GONE
            
            // Wait a bit for UI to hide? No, ScreenCapture should grab what's on screen.
            // But if controls are visible, they might cover the game. 
            // Better to hide controls, wait 100ms, then capture.
            withContext(Dispatchers.IO) {
                Thread.sleep(200) 
            }

            val bitmap = screenCaptureManager.capture()
            if (bitmap != null) {
                val results = withContext(Dispatchers.Default) {
                    imageAnalyzer.analyze(bitmap, rows, cols)
                }
                
                showResults(results)
            } else {
                Toast.makeText(this@OverlayService, getString(R.string.error_capture_failed), Toast.LENGTH_SHORT).show()
                controlsView.visibility = View.VISIBLE
            }
        }
    }

    private fun showResults(results: List<CardResult>) {
        resultView.setResults(results)
        addResultWindow()
        
        controlsView.findViewById<View>(R.id.scroll_grids).visibility = View.GONE
        controlsView.findViewById<View>(R.id.btn_stop).visibility = View.VISIBLE
        controlsView.visibility = View.VISIBLE
    }

    private fun addResultWindow() {
         val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, // Allow drawing over system bars
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Ensure resultView is removed from any parent before adding
        if (resultView.parent != null) {
            (resultView.parent as android.view.ViewGroup).removeView(resultView)
        }
        windowManager.addView(resultView, params)
    }
    
    private fun stopAnalysis() {
        // Remove Result Window
        try {
            windowManager.removeView(resultView)
        } catch (e: Exception) {}
        
        controlsView.findViewById<View>(R.id.scroll_grids).visibility = View.VISIBLE
        controlsView.findViewById<View>(R.id.btn_stop).visibility = View.GONE
        controlsView.visibility = View.VISIBLE

        // Auto-scroll to next button
        if (currentGridId != 0) {
            val keys = rules.keys.toList()
            val currentIndex = keys.indexOf(currentGridId)
            if (currentIndex != -1 && currentIndex < keys.size - 1) {
                val nextGridId = keys[currentIndex + 1]
                val nextButton = controlsView.findViewById<Button>(nextGridId)
                val scrollView = controlsView.findViewById<android.widget.HorizontalScrollView>(R.id.scroll_grids)
                
                if (nextButton != null && scrollView != null) {
                    scrollView.post {
                        val scrollX = nextButton.left - (scrollView.width / 2) + (nextButton.width / 2)
                        // Or just scroll to the button's left if we want it at the start, 
                        // but centering or ensuring it's visible is better. 
                        // The user request says "move to next step", so let's try to make sure it's visible.
                        // Simple approach: scroll to the next button's left position.
                        scrollView.smoothScrollTo(nextButton.left, 0)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            windowManager.removeView(controlsView)
        } catch (e: Exception) {}
        try {
            windowManager.removeView(resultView) // if attached
        } catch (e: Exception) {}
        screenCaptureManager.stop()
    }
}
