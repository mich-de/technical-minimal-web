package your.app.pkg  // template da LeanBox (WeatherHelper.kt): icona meteo disegnata in codice

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import kotlin.math.cos
import kotlin.math.sin

// enum Kind { SUN, PARTLY, CLOUD, FOG, RAIN, SNOW, STORM } in WeatherHelper

// Icona meteo monocromatica disegnata su griglia 24x24 (stile hairline); il sole usa il colore d'accento
class WeatherIcon(private val kind: WeatherHelper.Kind, private val ink: Int, private val accent: Int) : Drawable() {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 1.8f
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(bounds.width() / 24f, bounds.height() / 24f)
        when (kind) {
            WeatherHelper.Kind.SUN -> sun(canvas, 12f, 12f, 4.2f, 3.2f)
            WeatherHelper.Kind.PARTLY -> {
                sun(canvas, 8.5f, 8f, 3f, 2.2f)
                cloud(canvas, 2f, 3f)
            }
            WeatherHelper.Kind.CLOUD -> cloud(canvas, 0f, 0f)
            WeatherHelper.Kind.FOG -> {
                stroke.color = ink
                for ((y, x0, x1) in listOf(Triple(8f, 4f, 20f), Triple(12f, 2.5f, 21.5f), Triple(16f, 5f, 19f))) {
                    canvas.drawLine(x0, y, x1, y, stroke)
                }
            }
            WeatherHelper.Kind.RAIN -> {
                cloud(canvas, 0f, -3.5f)
                stroke.color = ink
                for (x in listOf(8f, 12.5f, 17f)) canvas.drawLine(x, 18f, x - 1.5f, 22f, stroke)
            }
            WeatherHelper.Kind.SNOW -> {
                cloud(canvas, 0f, -3.5f)
                fill.color = ink
                for (x in listOf(8f, 12.5f, 17f)) canvas.drawCircle(x - 0.7f, 20.2f, 1.2f, fill)
            }
            WeatherHelper.Kind.STORM -> {
                cloud(canvas, 0f, -3.5f)
                fill.color = accent
                canvas.drawPath(Path().apply {
                    moveTo(13.5f, 15.5f); lineTo(9.5f, 20.5f); lineTo(12.2f, 20.5f)
                    lineTo(10.8f, 23.5f); lineTo(15.2f, 18.5f); lineTo(12.5f, 18.5f); close()
                }, fill)
            }
        }
        canvas.restore()
    }

    private fun sun(canvas: Canvas, cx: Float, cy: Float, r: Float, ray: Float) {
        stroke.color = accent
        canvas.drawCircle(cx, cy, r, stroke)
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val c = cos(a).toFloat()
            val s = sin(a).toFloat()
            canvas.drawLine(cx + c * (r + 1.6f), cy + s * (r + 1.6f), cx + c * (r + 1.6f + ray * 0.6f), cy + s * (r + 1.6f + ray * 0.6f), stroke)
        }
    }

    // Nuvola piena: unione di cerchi e base arrotondata
    private fun cloud(canvas: Canvas, dx: Float, dy: Float) {
        fill.color = ink
        canvas.drawRoundRect(RectF(4.5f + dx, 13f + dy, 22f + dx, 19f + dy), 3f, 3f, fill)
        canvas.drawCircle(9.5f + dx, 13.2f + dy, 3.8f, fill)
        canvas.drawCircle(14.5f + dx, 10.8f + dy, 5f, fill)
        canvas.drawCircle(18.6f + dx, 14f + dy, 3.2f, fill)
    }

    override fun setAlpha(alpha: Int) {
        fill.alpha = alpha
        stroke.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fill.colorFilter = colorFilter
        stroke.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
