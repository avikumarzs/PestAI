package com.avikshit.PestAI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import java.util.LinkedList
import kotlin.math.max
import com.avikshit.PestAI.R

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    /** When true, draw HUD-style corner brackets instead of full rectangles (Rover mode). */
    var isRoverMode = false

    private var results = listOf<BoundingBox>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        results = listOf()
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results.forEach {
            val left = it.x1 * width
            val top = it.y1 * height
            val right = it.x2 * width
            val bottom = it.y2 * height

            if (isRoverMode) {
                drawCornerBrackets(canvas, left, top, right, bottom)
            } else {
                canvas.drawRect(left, top, right, bottom, boxPaint)
            }

            val drawableText = it.clsName + " " + String.format("%.2f", it.cnf)
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
        }
    }

    /** Draws HUD-style corner brackets (drone targeting) at the four corners of the box. */
    private fun drawCornerBrackets(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        val w = right - left
        val h = bottom - top
        val len = (minOf(w, h) * BRACKET_LENGTH_RATIO).coerceIn(MIN_BRACKET_LEN, MAX_BRACKET_LEN)

        // Top-left: vertical down, horizontal right
        canvas.drawLine(left, top, left, top + len, boxPaint)
        canvas.drawLine(left, top, left + len, top, boxPaint)
        // Top-right: vertical down, horizontal left
        canvas.drawLine(right, top, right, top + len, boxPaint)
        canvas.drawLine(right, top, right - len, top, boxPaint)
        // Bottom-left: vertical up, horizontal right
        canvas.drawLine(left, bottom, left, bottom - len, boxPaint)
        canvas.drawLine(left, bottom, left + len, bottom, boxPaint)
        // Bottom-right: vertical up, horizontal left
        canvas.drawLine(right, bottom, right, bottom - len, boxPaint)
        canvas.drawLine(right, bottom, right - len, bottom, boxPaint)
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
        private const val BRACKET_LENGTH_RATIO = 0.2f
        private const val MIN_BRACKET_LEN = 20f
        private const val MAX_BRACKET_LEN = 80f
    }
}