package your.app.pkg  // template da LeanBox: adatta package e R

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlin.math.max
import kotlin.math.sqrt

// Icone di dimensione visiva uniforme (stesso principio di IconNormalizer di AOSP Launcher3):
// misura l'area realmente opaca dell'icona e la riscala perché tutte occupino la stessa area,
// entro un ingombro massimo. Così un'icona tonda, una quadrata e una con molto bordo vuoto
// sembrano grandi uguali sulle card. Le icone degli strumenti di LeanBox non passano di qui.
object IconNormalizer {

    private const val SIZE = 192            // risoluzione di lavoro (px), basta per l'anteprima grande
    private const val TARGET_AREA = 0.58f   // frazione del quadrato occupata dai pixel visibili
    private const val MAX_EXTENT = 0.94f    // lato massimo del riquadro visibile
    private const val ALPHA_MIN = 40

    private val cache = HashMap<String, Bitmap>()

    fun clear() = cache.clear()

    fun normalized(resources: Resources, key: String, icon: Drawable): Drawable {
        val bmp = cache.getOrPut(key) { normalize(icon) }
        return BitmapDrawable(resources, bmp)
    }

    // Icone di un icon pack: già uniformi, solo convertite in bitmap (i vettori di un altro pacchetto
    // non devono essere tinti come quelli di LeanBox)
    fun rasterized(resources: Resources, key: String, icon: Drawable): Drawable {
        val bmp = cache.getOrPut("raw:$key") { render(icon) }
        return BitmapDrawable(resources, bmp)
    }

    private fun render(icon: Drawable): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        (icon.constantState?.newDrawable()?.mutate() ?: icon).apply {
            setBounds(0, 0, SIZE, SIZE)
            draw(Canvas(out))
        }
        return out
    }

    private fun normalize(icon: Drawable): Bitmap {
        val src = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val copy = icon.constantState?.newDrawable()?.mutate() ?: icon
        copy.setBounds(0, 0, SIZE, SIZE)
        copy.draw(Canvas(src))

        val pixels = IntArray(SIZE * SIZE)
        src.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
        var minX = SIZE; var minY = SIZE; var maxX = -1; var maxY = -1
        var opaque = 0
        for (y in 0 until SIZE) for (x in 0 until SIZE) {
            if ((pixels[y * SIZE + x] ushr 24) < ALPHA_MIN) continue
            opaque++
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        if (opaque == 0) return src

        val bounds = Rect(minX, minY, maxX + 1, maxY + 1)
        // Scala per area visibile costante, limitata dall'ingombro massimo del riquadro
        val byArea = sqrt(TARGET_AREA * SIZE * SIZE / opaque)
        val byExtent = MAX_EXTENT * SIZE / max(bounds.width(), bounds.height())
        val scale = minOf(byArea, byExtent)

        val w = bounds.width() * scale
        val h = bounds.height() * scale
        val dst = RectF((SIZE - w) / 2f, (SIZE - h) / 2f, (SIZE + w) / 2f, (SIZE + h) / 2f)
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, bounds, dst, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
        src.recycle()
        return out
    }
}
