package your.app.pkg  // template da LeanBox: adatta package e R

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.PathInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

// Pannello laterale a destra (stile impostazioni Android TV): Indietro torna al pannello padre
class SidePanel(
    private val activity: Activity,
    title: String,
    subtitle: String? = null,
    icon: Drawable? = null,
    private val onBack: (() -> Unit)? = null
) {
    private val dp = activity.resources.displayMetrics.density
    private val theme = ThemeManager.getActivePreset(activity)
    private val accent = theme.accentColor
    private val hairline = dp.toInt().coerceAtLeast(1)
    private val dialog = Dialog(activity)
    private val list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private val panel: LinearLayout
    private var firstItem: View? = null
    private var headerCount = 0
    private var keyTap: ((KeyEvent) -> Boolean)? = null
    private var pendingClick: Runnable? = null

    init {
        panel = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((28 * dp).toInt(), (36 * dp).toInt(), (28 * dp).toInt(), (24 * dp).toInt())
            // Superficie piena con sola hairline sul bordo sinistro
            background = LayerDrawable(arrayOf(
                ColorDrawable(theme.line),
                ColorDrawable(ThemeManager.withAlpha(theme.surface, 0xF5))
            )).apply { setLayerInset(1, hairline, 0, 0, 0) }
        }

        val head = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, (18 * dp).toInt())
        }
        if (icon != null) {
            head.addView(ImageView(activity).apply {
                setImageDrawable(copyOf(icon))
            }, LinearLayout.LayoutParams((40 * dp).toInt(), (40 * dp).toInt()).apply {
                marginEnd = (14 * dp).toInt()
            })
        }
        val titles = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(TextView(activity).apply {
            text = title
            textSize = 22f
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(theme.text)
            typeface = ThemeManager.getFontTypeface(activity, isBold = true)
        })
        if (!subtitle.isNullOrEmpty()) {
            titles.addView(TextView(activity).apply {
                text = subtitle
                textSize = 12f
                setTextColor(theme.muted)
                typeface = ThemeManager.getMonoTypeface(activity)
                setPadding(0, (4 * dp).toInt(), 0, 0)
            })
        }
        head.addView(titles)
        panel.addView(head)
        panel.addView(View(activity).apply { setBackgroundColor(theme.line) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hairline).apply {
                bottomMargin = (8 * dp).toInt()
            })

        val scroll = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            isVerticalFadingEdgeEnabled = true
            setFadingEdgeLength((20 * dp).toInt())
            addView(list)
        }
        panel.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(panel)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.END)
            setLayout((400 * dp).toInt(), WindowManager.LayoutParams.MATCH_PARENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(if (theme.light) 0.25f else 0.5f)
        }
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyTap?.invoke(event) == true) return@setOnKeyListener true
            if (keyCode in RemoteKeys.CONFIRM && RemoteKeys.isCec(event)) return@setOnKeyListener handleCecConfirm(event)
            if (keyCode != KeyEvent.KEYCODE_BACK || onBack == null) return@setOnKeyListener false
            if (event.action == KeyEvent.ACTION_UP) {
                dialog.dismiss()
                onBack.invoke()
            }
            true
        }
    }

    // OK dal telecomando TV (HDMI-CEC): tenuto premuto arriva come raffica di OK brevi. La voce a fuoco
    // si attiva una sola volta, a raffica finita, così la raffica non prosegue nel pannello o dialog
    // successivo (es. conferma di disinstallazione). Una raffica iniziata prima di questo pannello
    // (OK tenuto sulla card che lo ha aperto) viene ignorata
    private fun handleCecConfirm(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return true
        val burst = RemoteKeys.continuesBurst(event)
        val pending = pendingClick
        if (burst && pending == null) return true
        pending?.let { panel.removeCallbacks(it) }
        val target = dialog.currentFocus ?: return true
        pendingClick = Runnable {
            pendingClick = null
            target.performClick()
        }.also { panel.postDelayed(it, RemoteKeys.BURST_GAP_MS) }
        return true
    }

    fun focused(): View? = dialog.currentFocus

    fun dismiss() = dialog.dismiss()

    // Intercetta i tasti prima del pannello (true = consumato); usato dal test del telecomando
    fun onKey(tap: (KeyEvent) -> Boolean) {
        keyTap = tap
    }

    // Testo monospazio aggiornabile (es. registro dei tasti premuti)
    fun mono(text: String): TextView = TextView(activity).apply {
        this.text = text
        textSize = 14f
        setLineSpacing(0f, 1.25f)
        setTextColor(theme.text)
        typeface = ThemeManager.getMonoTypeface(activity)
        setPadding((14 * dp).toInt(), (8 * dp).toInt(), (14 * dp).toInt(), (8 * dp).toInt())
        list.addView(this)
    }

    private fun copyOf(icon: Drawable): Drawable = ThemeManager.tintIfVector(
        icon.constantState?.newDrawable(activity.resources)?.mutate() ?: icon, theme.text)

    // Intestazione tecnica: "01 // SEZIONE" (o solo il testo, es. le lettere di Tutte le app)
    fun header(text: String, numbered: Boolean = true) {
        headerCount++
        list.addView(TextView(activity).apply {
            this.text = if (numbered) "%02d // %s".format(headerCount, text.uppercase()) else text.uppercase()
            textSize = 11f
            letterSpacing = 0.08f
            setTextColor(accent)
            typeface = ThemeManager.getMonoTypeface(activity, medium = true)
            setPadding((14 * dp).toInt(), (16 * dp).toInt(), 0, (6 * dp).toInt())
        })
    }

    fun note(text: String) {
        list.addView(TextView(activity).apply {
            this.text = text
            textSize = 14f
            setTextColor(theme.muted)
            typeface = ThemeManager.getFontTypeface(activity, isBold = false)
            setPadding((14 * dp).toInt(), (8 * dp).toInt(), (14 * dp).toInt(), (8 * dp).toInt())
        })
    }

    // Di default il click chiude il pannello prima dell'azione (che spesso apre un altro pannello)
    fun item(
        title: String,
        value: String? = null,
        icon: Drawable? = null,
        checked: Boolean = false,
        danger: Boolean = false,
        onClick: () -> Unit
    ): View {
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            isClickable = true
            setPadding((14 * dp).toInt(), (11 * dp).toInt(), (14 * dp).toInt(), (11 * dp).toInt())
            attachKineticBackground(this)
            setOnClickListener {
                dialog.dismiss()
                onClick()
            }
        }
        if (icon != null) {
            row.addView(ImageView(activity).apply {
                setImageDrawable(copyOf(icon))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, LinearLayout.LayoutParams((26 * dp).toInt(), (26 * dp).toInt()).apply {
                marginEnd = (14 * dp).toInt()
            })
        }
        val texts = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(TextView(activity).apply {
            text = title
            textSize = 16f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(if (danger) ThemeManager.hue(activity, 25f) else theme.text)
            typeface = ThemeManager.getFontTypeface(activity, isBold = false)
        })
        if (!value.isNullOrEmpty()) {
            texts.addView(TextView(activity).apply {
                text = value
                textSize = 12f
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(theme.muted)
                typeface = ThemeManager.getMonoTypeface(activity)
                setPadding(0, (3 * dp).toInt(), 0, 0)
            })
        }
        row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (checked) {
            // Spia LED accesa al posto del segno di spunta
            row.addView(View(activity).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(accent)
                }
            }, LinearLayout.LayoutParams((8 * dp).toInt(), (8 * dp).toInt()).apply {
                marginStart = (12 * dp).toInt()
            })
        }
        list.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        list.addView(View(activity).apply { setBackgroundColor(theme.line) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hairline).apply {
                marginStart = (14 * dp).toInt()
            })
        if (firstItem == null || checked) firstItem = row
        return row
    }

    // Riga di sola lettura (etichetta a sinistra, valore a destra); focalizzabile per poter scorrere col telecomando
    fun info(title: String, value: String?) {
        if (value.isNullOrEmpty()) return
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            setPadding((14 * dp).toInt(), (9 * dp).toInt(), (14 * dp).toInt(), (9 * dp).toInt())
            attachKineticBackground(this)
        }
        row.addView(TextView(activity).apply {
            text = title
            textSize = 13f
            setTextColor(theme.muted)
            typeface = ThemeManager.getMonoTypeface(activity)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.42f))
        row.addView(TextView(activity).apply {
            text = value
            textSize = 15f
            gravity = Gravity.END
            maxLines = 12
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(theme.text)
            typeface = ThemeManager.getFontTypeface(activity, isBold = false)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.58f))
        list.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        // I pannelli informativi partono dall'alto
        if (firstItem == null) firstItem = row
    }

    // Griglia di scelte brevi (es. icone emoji); la scelta attuale è evidenziata e riceve il fuoco
    fun grid(choices: List<String>, selected: String?, onPick: (String) -> Unit) {
        for (chunk in choices.chunked(6)) {
            val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
            for (choice in chunk) {
                val isSelected = choice == selected
                val cell = TextView(activity).apply {
                    text = choice
                    textSize = 26f
                    gravity = Gravity.CENTER
                    isFocusable = true
                    isClickable = true
                    background = cellBackground(false, isSelected)
                    onFocusChangeListener = View.OnFocusChangeListener { v, focused -> v.background = cellBackground(focused, isSelected) }
                    setOnClickListener {
                        dialog.dismiss()
                        onPick(choice)
                    }
                }
                row.addView(cell, LinearLayout.LayoutParams(0, (56 * dp).toInt(), 1f).apply {
                    val m = (3 * dp).toInt()
                    setMargins(m, m, m, m)
                })
                if (firstItem == null || isSelected) firstItem = cell
            }
            // Celle vuote per mantenere la larghezza nell'ultima riga
            repeat(6 - chunk.size) { row.addView(View(activity), LinearLayout.LayoutParams(0, 0, 1f)) }
            list.addView(row)
        }
    }

    private fun cellBackground(focused: Boolean, selected: Boolean) = GradientDrawable().apply {
        cornerRadius = 6 * dp
        when {
            focused -> {
                setColor(ThemeManager.mix(accent, theme.surface, 0.12f))
                setStroke((2 * dp).toInt(), accent)
            }
            selected -> setStroke(hairline, theme.lineStrong)
            else -> setColor(Color.TRANSPARENT)
        }
    }

    // Riga "cinetica": a fuoco il fondo tinto si espande da sinistra, con barretta d'accento
    private fun attachKineticBackground(row: View) {
        val fill = ClipDrawable(ColorDrawable(ThemeManager.mix(accent, theme.surface, 0.10f)), Gravity.START, ClipDrawable.HORIZONTAL)
        val bar = ClipDrawable(ColorDrawable(accent), Gravity.START, ClipDrawable.HORIZONTAL)
        row.background = LayerDrawable(arrayOf(fill, bar)).apply {
            setLayerWidth(1, (3 * dp).toInt())
            setLayerGravity(1, Gravity.START or Gravity.FILL_VERTICAL)
        }
        val ease = PathInterpolator(0.16f, 1f, 0.3f, 1f)
        row.onFocusChangeListener = View.OnFocusChangeListener { _, focused ->
            bar.level = if (focused) 10000 else 0
            ObjectAnimator.ofInt(fill, "level", fill.level, if (focused) 10000 else 0).apply {
                duration = if (focused) 450L else 160L
                interpolator = ease
                start()
            }
        }
    }

    // Il fuoco parte dalla voce selezionata (LED acceso) se presente
    fun show() {
        dialog.show()
        firstItem?.requestFocus()
        panel.translationX = 48 * dp
        panel.alpha = 0f
        panel.animate().translationX(0f).alpha(1f).setDuration(220L)
            .setInterpolator(PathInterpolator(0.16f, 1f, 0.3f, 1f)).start()
    }
}
