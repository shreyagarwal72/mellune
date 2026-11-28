package app.mellune.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import app.mellune.preferences2.PreferenceManager2
import app.mellune.preferences2.firstCached
import com.android.launcher3.icons.DotRenderer
import kotlin.math.max

/**
 * Renders Nova-inspired notification badge styles (`numeric`, `dynamic`) on launcher icons,
 * delegating back to AOSP's dot renderer when the default `dots` style is selected.
 */
object MelluneBadgeRenderer {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val textBounds = Rect()
    private val pillRect = RectF()

    fun drawCustomBadge(
        canvas: Canvas,
        context: Context,
        params: DotRenderer.DrawParams,
        notificationCount: Int,
    ): Boolean {
        val style = PreferenceManager2.getInstance(context).notificationBadgeStyle.firstCached()
        if (style != "numeric" && style != "dynamic") {
            return false
        }
        val scale = params.scale
        if (scale <= 0f) {
            return true
        }

        val density = context.resources.displayMetrics.density
        val bounds = params.iconBounds
        val cx = if (params.leftAlign) bounds.left + 8f * density else bounds.right - 6f * density
        val cy = bounds.top + 6f * density

        val label = when {
            notificationCount > 99 -> "99+"
            notificationCount > 0 -> notificationCount.toString()
            else -> ""
        }

        val baseRadius = 9f * density * scale
        textPaint.textSize = 10.5f * density * scale

        val pillWidth = if (label.isEmpty()) {
            baseRadius * 2f
        } else {
            textPaint.getTextBounds(label, 0, label.length, textBounds)
            max(baseRadius * 2f, textBounds.width() + 10f * density * scale)
        }
        val pillHeight = baseRadius * 2f

        pillRect.set(
            cx - pillWidth / 2f,
            cy - pillHeight / 2f,
            cx + pillWidth / 2f,
            cy + pillHeight / 2f,
        )

        bgPaint.color = params.dotColor
        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, bgPaint)

        if (style == "dynamic") {
            ringPaint.strokeWidth = 1.5f * density * scale
            ringPaint.color = ColorUtils.setAlphaComponent(Color.WHITE, 190)
            canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, ringPaint)
        }

        if (label.isNotEmpty()) {
            val luminance = ColorUtils.calculateLuminance(params.dotColor)
            textPaint.color = if (luminance > 0.55) Color.BLACK else Color.WHITE
            val textY = pillRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label, pillRect.centerX(), textY, textPaint)
        }
        return true
    }
}
