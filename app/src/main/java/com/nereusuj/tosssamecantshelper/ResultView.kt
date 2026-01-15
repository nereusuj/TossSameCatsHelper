package com.nereusuj.tosssamecantshelper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ResultView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val results = mutableListOf<CardResult>()
    
    private val boxPaint = Paint().apply {
        color = Color.LTGRAY // Light Grey Border
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    
    private val brightColors = listOf(
        Color.parseColor("#FFFF80"), // Light Yellow
        Color.parseColor("#80FFFF"), // Light Cyan
        Color.parseColor("#FF80FF"), // Light Magenta
        Color.parseColor("#80FF80"), // Light Green
        Color.parseColor("#FFB366"), // Light Orange
        Color.parseColor("#FF99CC"), // Light Pink
        Color.parseColor("#99CCFF"), // Light Blue
        Color.parseColor("#CCFF99")  // Light Lime
    )

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 100f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    
    // Background for text visibility
    private val textBgPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun setResults(newResults: List<CardResult>) {
        results.clear()
        results.addAll(newResults)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        for (item in results) {
            val rect = item.rect
            
            // Draw Box
            canvas.drawRect(rect, boxPaint)
            
            // Determine Color
            val colorIndex = item.groupId % brightColors.size
            textBgPaint.color = brightColors[colorIndex]
            
            // Draw Indicator at Top-Left
            val radius = 60f
            val cx = rect.left + radius
            val cy = rect.top + radius
            
            // Draw circle background for text
            canvas.drawCircle(cx, cy, radius, textBgPaint)
            
            // Draw Number
            val text = item.groupId.toString()
            val yPos = cy - ((textPaint.descent() + textPaint.ascent()) / 2)
            canvas.drawText(text, cx, yPos, textPaint)
        }
    }
}
