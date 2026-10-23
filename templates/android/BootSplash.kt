package your.app.pkg  // template da LeanBox: adatta package e R

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.sin

// Splash di avvio a matrice di punti 5x7: i punti si accendono a cascata con jitter deterministico,
// poi la barra segmentata (una tinta per sezione) si riempie e il LED passa da lampeggiante a fisso
class BootSplash(
    context: Context,
    private val hues: List<Int>,
    private val caption: String,
    private val onDone: () -> Unit
) : View(context) {

    private companion object {
        const val WORD = "LEANBOX."
        const val TOTAL_MS = 2100f
        const val DOT_FADE_MS = 160f
        const val BAR_START_MS = 950f
        const val BAR_MS = 650f
        const val READY_MS = BAR_START_MS + BAR_MS

        val GLYPHS = mapOf(
            'L' to listOf("10000", "10000", "10000", "10000", "10000", "10000", "11111"),
            'E' to listOf("11111", "10000", "10000", "11110", "10000", "10000", "11111"),
            'A' to listOf("01110", "10001", "10001", "11111", "10001", "10001", "10001"),
            'N' to listOf("10001", "11001", "10101", "10011", "10001", "10001", "10001"),
            'B' to listOf("11110", "10001", "10001", "11110", "10001", "10001", "11110"),
            'O' to listOf("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
            'X' to listOf("10001", "10001", "01010", "00100", "01010", "10001", "10001"),
            '.' to listOf("0", "0", "0", "0", "0", "0", "1")
        )
    }

    private class Dot(val col: Int, val row: Int, val lit: Boolean, val accent: Boolean, val delay: Float)

    private val theme = ThemeManager.getActivePreset(context)
    private val dp = resources.displayMetrics.density
    private val pitch = 14 * dp
    private val radius = 4.5f * dp
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = ThemeManager.getMonoTypeface(context, medium = true)
        textSize = 12 * resources.displayMetrics.scaledDensity
        letterSpacing = 0.08f
        color = theme.muted
    }
    private val dots: List<Dot>
    private val columns: Int
    private var elapsed = 0f
    private var dismissed = false
    private val animator = ValueAnimator.ofFloat(0f, TOTAL_MS).apply {
        duration = TOTAL_MS.toLong()
        interpolator = LinearInterpolator()
        addUpdateListener {
            elapsed = it.animatedValue as Float
            invalidate()
            if (elapsed >= TOTAL_MS) dismiss()
        }
    }

    init {
        background = ThemeManager.createPatternBackground(context)
        isClickable = true
        setOnTouchListener { _, _ -> dismiss(); true }

        val list = ArrayList<Dot>()
        var x = 0
        for (ch in WORD) {
            val glyph = GLYPHS.getValue(ch)
            for (r in glyph.indices) for (c in glyph[r].indices) {
                val gx = x + c
                // Cascata da sinistra con jitter pseudo-casuale stabile per punto
                val jitter = abs(sin(gx * 12.9898 + r * 78.233) * 43758.5453) % 1.0
                list.add(Dot(gx, r, glyph[r][c] == '1', ch == '.', gx * 14f + (jitter * 140).toFloat()))
            }
            x += glyph[0].length + 1
        }
        dots = list
        columns = x - 1
    }

    fun start() = animator.start()

    fun dismiss() {
        if (dismissed) return
        dismissed = true
        animator.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRenderEffect(RenderEffect.createBlurEffect(12 * dp, 12 * dp, Shader.TileMode.CLAMP))
        }
        animate().alpha(0f).scaleX(1.04f).scaleY(1.04f).setDuration(320L).withEndAction {
            (parent as? ViewGroup)?.removeView(this)
            onDone()
        }.start()
    }

    override fun onDraw(canvas: Canvas) {
        val matrixW = (columns - 1) * pitch
        val left = (width - matrixW) / 2f
        val top = height * 0.40f - 3 * pitch

        for (d in dots) {
            val cx = left + d.col * pitch
            val cy = top + d.row * pitch
            paint.color = theme.lineStrong
            if (d.lit) {
                val a = ((elapsed - d.delay) / DOT_FADE_MS).coerceIn(0f, 1f)
                paint.color = ThemeManager.mix(if (d.accent) theme.accentColor else theme.text, theme.lineStrong, a)
            }
            canvas.drawCircle(cx, cy, radius, paint)
        }

        // Barra segmentata: un segmento per sezione, ciascuno nella propria tinta
        val barTop = top + 6 * pitch + 44 * dp
        val barH = 4 * dp
        val gap = 4 * dp
        val n = hues.size.coerceAtLeast(1)
        val segW = (matrixW + 2 * radius - gap * (n - 1)) / n
        for (i in 0 until n) {
            val sx = left - radius + i * (segW + gap)
            paint.color = theme.line
            canvas.drawRect(sx, barTop, sx + segW, barTop + barH, paint)
            val start = BAR_START_MS + i * BAR_MS / n
            val f = ((elapsed - start) / (BAR_MS / n)).coerceIn(0f, 1f)
            if (f > 0f) {
                paint.color = hues.getOrElse(i) { theme.accentColor }
                canvas.drawRect(sx, barTop, sx + segW * f, barTop + barH, paint)
            }
        }

        // LED lampeggiante durante il boot, fisso (verde) a READY
        val ready = elapsed >= READY_MS
        val ledY = barTop + barH + 26 * dp
        val ledX = left - radius + 4 * dp
        paint.color = if (ready) ThemeManager.hue(context, 145f) else theme.accentColor
        paint.alpha = if (ready || (elapsed / 180f).toInt() % 2 == 0) 255 else 60
        canvas.drawCircle(ledX, ledY, 4 * dp, paint)
        paint.alpha = 255

        val baseline = ledY - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(if (ready) "READY" else "BOOT", ledX + 14 * dp, baseline, textPaint)
        canvas.drawText(caption, left + matrixW + radius - textPaint.measureText(caption), baseline, textPaint)
    }
}
