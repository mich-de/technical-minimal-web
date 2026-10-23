package your.app.pkg  // template da LeanBox: adatta package e R

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.VectorDrawable
import java.io.File
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// Design system "Technical Minimal" (Nothing / Teenage Engineering): token di colore,
// tipografia Doto / Geist / Geist Mono, hairline, pattern a punti e tinte OKLCH per sezione
object ThemeManager {

    data class ThemePreset(
        val id: String,
        val name: String,
        val light: Boolean,
        val bg: Int,
        val surface: Int,
        val line: Int,
        val lineStrong: Int,
        val text: Int,
        val muted: Int,
        val accentColor: Int,
        val onAccent: Int,
        val dot: Int
    )

    val PRESETS = listOf(
        ThemePreset(
            id = "tm_dark",
            name = "Technical Scuro (Consigliato)",
            light = false,
            bg = Color.parseColor("#0E0E0E"),
            surface = Color.parseColor("#151515"),
            line = Color.parseColor("#262626"),
            lineStrong = Color.parseColor("#3A3A3A"),
            text = Color.parseColor("#ECEBE8"),
            muted = Color.parseColor("#9A968F"),
            accentColor = Color.parseColor("#FF5A1F"),
            onAccent = Color.parseColor("#0E0E0E"),
            dot = Color.parseColor("#2B2B2B")
        ),
        ThemePreset(
            id = "tm_light",
            name = "Technical Chiaro (Carta)",
            light = true,
            bg = Color.parseColor("#F3F2EF"),
            surface = Color.parseColor("#FBFAF8"),
            line = Color.parseColor("#D9D6D0"),
            lineStrong = Color.parseColor("#BDB9B1"),
            text = Color.parseColor("#141413"),
            muted = Color.parseColor("#5F5B55"),
            accentColor = Color.parseColor("#E5481A"),
            onAccent = Color.WHITE,
            dot = Color.parseColor("#CFCBC4")
        ),
        ThemePreset(
            id = "tm_oled",
            name = "Technical OLED (Nero assoluto)",
            light = false,
            bg = Color.BLACK,
            surface = Color.parseColor("#0A0A0A"),
            line = Color.parseColor("#1F1F1F"),
            lineStrong = Color.parseColor("#333333"),
            text = Color.parseColor("#ECEBE8"),
            muted = Color.parseColor("#8F8B84"),
            accentColor = Color.parseColor("#FF5A1F"),
            onAccent = Color.BLACK,
            dot = Color.parseColor("#1C1C1C")
        )
    )

    const val DEFAULT_PRESET = "tm_dark"
    const val DEFAULT_DAY = "tm_light"
    const val DEFAULT_NIGHT = "tm_dark"
    private const val THEME_VERSION = 2

    // Passaggio una tantum al nuovo tema: i vecchi preset non esistono più, il carattere torna Geist
    fun migrate(context: Context) {
        val prefs = context.getSharedPreferences("leanbox", Context.MODE_PRIVATE)
        if (prefs.getInt("theme_version", 1) >= THEME_VERSION) return
        prefs.edit()
            .putString("theme_preset", DEFAULT_PRESET)
            .putString("theme_day", DEFAULT_DAY)
            .putString("theme_night", DEFAULT_NIGHT)
            .putString("font_family", "geist")
            .putString("card_radius", "soft")
            .putInt("theme_version", THEME_VERSION)
            .apply()
    }

    fun getActivePreset(context: Context): ThemePreset {
        val prefs = context.getSharedPreferences("leanbox", Context.MODE_PRIVATE)
        // Tema automatico: preset diverso per giorno e notte (uiMode di sistema)
        if (prefs.getBoolean("theme_auto", false)) {
            val night = (context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
            val id = prefs.getString(if (night) "theme_night" else "theme_day", if (night) DEFAULT_NIGHT else DEFAULT_DAY)
            return PRESETS.firstOrNull { it.id == id } ?: PRESETS[0]
        }
        val id = prefs.getString("theme_preset", DEFAULT_PRESET)
        return PRESETS.firstOrNull { it.id == id } ?: PRESETS[0]
    }

    fun getCardWidthDp(context: Context): Int {
        val prefs = context.getSharedPreferences("leanbox", Context.MODE_PRIVATE)
        return when (prefs.getString("card_size", "standard")) {
            "compact" -> 136
            "large" -> 184
            else -> 156
        }
    }

    fun getCardRadiusDp(context: Context): Float {
        val prefs = context.getSharedPreferences("leanbox", Context.MODE_PRIVATE)
        return when (prefs.getString("card_radius", "soft")) {
            "none" -> 0f
            "soft" -> 8f
            "round" -> 22f
            else -> 14f
        }
    }

    // Testo corrente (titoli, voci): Geist di default, oppure il carattere di sistema scelto
    fun getFontTypeface(context: Context, isBold: Boolean = false): Typeface {
        val prefs = context.getSharedPreferences("leanbox", Context.MODE_PRIVATE)
        val style = if (isBold) Typeface.BOLD else Typeface.NORMAL
        return when (prefs.getString("font_family", "geist")) {
            "sans-serif" -> Typeface.create("sans-serif-medium", style)
            "sans-serif-light" -> Typeface.create("sans-serif-light", style)
            "sans-serif-condensed" -> Typeface.create("sans-serif-condensed", style)
            "monospace" -> Typeface.create(Typeface.MONOSPACE, style)
            "serif" -> Typeface.create(Typeface.SERIF, style)
            else -> context.resources.getFont(if (isBold) R.font.geist_semibold else R.font.geist_regular)
        }
    }

    // Metadati, contatori, indici di sezione, status bar (cifre tabulari)
    fun getMonoTypeface(context: Context, medium: Boolean = false): Typeface =
        context.resources.getFont(if (medium) R.font.geist_mono_medium else R.font.geist_mono_regular)

    // Display a matrice di punti: orologio, wordmark, splash
    fun getDisplayTypeface(context: Context): Typeface = context.resources.getFont(R.font.doto_black)

    // Tinta di sezione: oklch(.48 .14 h) su chiaro, oklch(.76 .12 h) su scuro (contrasto ≥ 4.5:1)
    fun hue(context: Context, h: Float): Int =
        if (getActivePreset(context).light) oklch(0.48f, 0.14f, h) else oklch(0.76f, 0.12f, h)

    // Tonalità distribuite sul cerchio, a partire dall'arancio dell'accento
    fun sectionHue(index: Int, count: Int): Float = (35f + index * 360f / count.coerceAtLeast(1)) % 360f

    // color-mix(in srgb, color pct%, base)
    fun mix(color: Int, base: Int, pct: Float): Int = Color.rgb(
        (Color.red(color) * pct + Color.red(base) * (1 - pct)).roundToInt(),
        (Color.green(color) * pct + Color.green(base) * (1 - pct)).roundToInt(),
        (Color.blue(color) * pct + Color.blue(base) * (1 - pct)).roundToInt()
    )

    fun withAlpha(color: Int, alpha: Int): Int = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    // Le icone vettoriali di LeanBox sono bianche: sul tema chiaro vanno tinte; le icone delle app restano a colori
    fun tintIfVector(drawable: Drawable, color: Int): Drawable {
        if (drawable is VectorDrawable) drawable.setTintList(ColorStateList.valueOf(color))
        return drawable
    }

    fun dimColor(context: Context, percent: Int): Int =
        withAlpha(getActivePreset(context).bg, (percent * 255) / 100)

    private fun oklch(l: Float, c: Float, hDeg: Float): Int {
        val h = Math.toRadians(hDeg.toDouble())
        val a = c * cos(h)
        val b = c * sin(h)
        val l_ = (l + 0.3963377774 * a + 0.2158037573 * b).pow(3.0)
        val m_ = (l - 0.1055613458 * a - 0.0638541728 * b).pow(3.0)
        val s_ = (l - 0.0894841775 * a - 1.2914855480 * b).pow(3.0)
        val r = 4.0767416621 * l_ - 3.3077115913 * m_ + 0.2309699292 * s_
        val g = -1.2684380046 * l_ + 2.6097574011 * m_ - 0.3413193965 * s_
        val bl = -0.0041960863 * l_ - 0.7034186147 * m_ + 1.7076147010 * s_
        fun gamma(x: Double): Int {
            val v = x.coerceIn(0.0, 1.0)
            val e = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1 / 2.4) - 0.055
            return (e * 255).roundToInt()
        }
        return Color.rgb(gamma(r), gamma(g), gamma(bl))
    }

    // Pattern radiale a punti (1.2dp ogni 16dp) come piastrella ripetuta: un solo bitmap minuscolo
    private fun dotGrid(context: Context, preset: ThemePreset): Drawable {
        val dp = context.resources.displayMetrics.density
        val step = (16 * dp).roundToInt()
        val tile = Bitmap.createBitmap(step, step, Bitmap.Config.ARGB_8888)
        Canvas(tile).drawCircle(step / 2f, step / 2f, 1.2f * dp, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = preset.dot })
        return BitmapDrawable(context.resources, tile).apply { setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT) }
    }

    fun createRootBackground(context: Context): Drawable {
        val prefs = context.getSharedPreferences("leanbox", Context.MODE_PRIVATE)
        val wallpaperPath = prefs.getString("custom_wallpaper_path", null)
        val dimming = prefs.getInt("wallpaper_dimming", 35)

        if (!wallpaperPath.isNullOrEmpty()) {
            // Percorso di file o content:// da SAF (scelto da Impostazioni › Sfondo)
            val bitmap = try {
                if (wallpaperPath.startsWith("content://")) {
                    context.contentResolver.openInputStream(android.net.Uri.parse(wallpaperPath))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                } else {
                    val file = File(wallpaperPath)
                    if (file.exists() && file.canRead()) BitmapFactory.decodeFile(file.absolutePath) else null
                }
            } catch (_: Exception) { null }
            if (bitmap != null) {
                val bmpDrawable = BitmapDrawable(context.resources, bitmap)
                val dimDrawable = ColorDrawable(dimColor(context, dimming))
                return LayerDrawable(arrayOf(bmpDrawable, dimDrawable))
            }
        }

        return createPatternBackground(context)
    }

    fun createPatternBackground(context: Context): Drawable {
        val preset = getActivePreset(context)
        return LayerDrawable(arrayOf(ColorDrawable(preset.bg), dotGrid(context, preset)))
    }

    // Superficie con hairline; a fuoco bordo accento
    fun createTileBackground(context: Context, focused: Boolean): GradientDrawable {
        val preset = getActivePreset(context)
        val dp = context.resources.displayMetrics.density
        val bg = GradientDrawable()
        bg.cornerRadius = getCardRadiusDp(context) * dp
        if (focused) {
            bg.setColor(mix(preset.accentColor, preset.surface, 0.12f))
            bg.setStroke((2 * dp).roundToInt(), preset.accentColor)
        } else {
            bg.setColor(preset.surface)
            bg.setStroke(dp.roundToInt().coerceAtLeast(1), preset.line)
        }
        return bg
    }
}
