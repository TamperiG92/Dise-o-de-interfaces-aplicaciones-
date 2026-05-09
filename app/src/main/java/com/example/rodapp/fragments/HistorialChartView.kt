package com.example.rodapp.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.example.rodapp.R

class HistorialChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class BarEntry(val label: String, val value: Float)

    private var entries: List<BarEntry> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.button_blue)
    }

    private fun sp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.gris_hint)
        textSize = sp(10f)
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.white)
        textSize = sp(9f)
        textAlign = Paint.Align.CENTER
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.gris_hint)
        textSize = sp(12f)
        textAlign = Paint.Align.CENTER
    }

    private val dp = resources.displayMetrics.density

    fun setData(data: List<BarEntry>) {
        entries = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (entries.isEmpty() || entries.all { it.value == 0f }) {
            canvas.drawText(
                context.getString(R.string.label_sin_datos_mes),
                width / 2f,
                height / 2f - emptyPaint.descent(),
                emptyPaint
            )
            return
        }

        val maxValue = entries.maxOf { it.value }.takeIf { it > 0 } ?: 1f

        val padH = 4 * dp
        val padBottom = 20 * dp
        val padTop = 20 * dp

        val chartWidth = width - padH * 2
        val chartHeight = height - padTop - padBottom

        val count = entries.size
        val gap = 6 * dp
        val barWidth = (chartWidth - gap * (count - 1)) / count

        entries.forEachIndexed { i, entry ->
            val x = padH + i * (barWidth + gap)
            val barH = if (maxValue > 0) (entry.value / maxValue) * chartHeight else 0f
            val minBarH = if (entry.value > 0) 4 * dp else 0f
            val actualBarH = maxOf(barH, minBarH)
            val top = padTop + chartHeight - actualBarH
            val bottom = padTop + chartHeight

            val radius = barWidth * 0.3f
            canvas.drawRoundRect(RectF(x, top, x + barWidth, bottom), radius, radius, barPaint)

            if (entry.value > 0) {
                val txt = if (entry.value >= 1_000_000) "$${(entry.value / 1_000_000).toInt()}M"
                          else if (entry.value >= 1_000) "$${(entry.value / 1_000).toInt()}k"
                          else "$${entry.value.toInt()}"
                canvas.drawText(txt, x + barWidth / 2, top - 4 * dp, valuePaint)
            }

            canvas.drawText(entry.label, x + barWidth / 2, height - 4 * dp, labelPaint)
        }
    }
}
