package com.example.JoystickAGV

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var hatRadius = 0f

    private var posX = 0f
    private var posY = 0f

    private var listener: ((Int, Int) -> Unit)? = null

    private val basePaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.FILL
    }

    private val hatPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    fun setOnMoveListener(l: (x: Int, y: Int) -> Unit) {
        listener = l
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = min(w, h) / 2.5f
        hatRadius = min(w, h) / 8f
        posX = centerX
        posY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        canvas.drawCircle(posX, posY, hatRadius, hatPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val dx = event.x - centerX
        val dy = event.y - centerY
        val distance = sqrt(dx * dx + dy * dy)

        if (event.action != MotionEvent.ACTION_UP) {
            if (distance < baseRadius) {
                posX = event.x
                posY = event.y
            } else {
                val ratio = baseRadius / distance
                posX = centerX + dx * ratio
                posY = centerY + dy * ratio
            }

            val xPercent = ((posX - centerX) / baseRadius * 100).toInt()
            val yPercent = ((centerY - posY) / baseRadius * 100).toInt()

            listener?.invoke(xPercent, yPercent)

        } else {
            posX = centerX
            posY = centerY
            listener?.invoke(0, 0)
        }

        invalidate()
        return true
    }
}