package your.app.pkg  // template da LeanBox: adatta package e R

import android.view.KeyEvent
import android.view.View
import java.text.Normalizer
import java.util.Locale

// Tutte le app in ordine alfabetico, raggruppate per lettera. Pensato per il telecomando della TV:
// scrivere è lento, quindi ◀ ▶ saltano alla lettera precedente/successiva, ▲ ▼ scorrono le app,
// OK apre, Rosso (o Menu) apre le opzioni dell'app
object AllAppsPanel {

    fun show(activity: MainActivity) {
        val apps = activity.allApps().filter { !activity.isHidden(it) }
        val groups = apps.groupBy { letterOf(it.label) }.toSortedMap(compareBy { if (it == "#") "￿" else it })
        val firstOfLetter = ArrayList<View>()
        val letterIndex = HashMap<View, Int>()

        SidePanel(activity, "Tutte le app", "${apps.size} app · ◀ ▶ lettera · Rosso opzioni").apply {
            groups.entries.forEachIndexed { i, (letter, list) ->
                header(letter, numbered = false)
                list.forEachIndexed { j, app ->
                    val row = item(app.label, activity.categoryTitle(app), activity.appIcon(app)) { activity.launch(app) }
                    row.tag = app
                    letterIndex[row] = i
                    if (j == 0) firstOfLetter.add(row)
                }
            }
            onKey { e ->
                when (e.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (e.action == KeyEvent.ACTION_DOWN && firstOfLetter.isNotEmpty()) {
                            val current = letterIndex[focused()] ?: 0
                            val step = if (e.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1
                            firstOfLetter[(current + step).coerceIn(0, firstOfLetter.lastIndex)].requestFocus()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_PROG_RED, KeyEvent.KEYCODE_MENU -> {
                        val app = focused()?.tag as? MainActivity.App
                        if (e.action == KeyEvent.ACTION_DOWN && !RemoteKeys.continuesBurst(e) && app != null) {
                            dismiss()
                            AppContextMenuHelper.showMenu(activity, app, onBack = { show(activity) })
                        }
                        true
                    }
                    else -> false
                }
            }
        }.show()
    }

    // Iniziale senza accenti (È → E); cifre e simboli finiscono in "#"
    private fun letterOf(label: String): String {
        val c = Normalizer.normalize(label.trim(), Normalizer.Form.NFD).firstOrNull { it.isLetterOrDigit() }
            ?: return "#"
        val up = c.uppercase(Locale.ITALY)
        return if (up[0] in 'A'..'Z') up else "#"
    }
}
