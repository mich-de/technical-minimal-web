package your.app.pkg  // template da LeanBox: adatta package e R

import android.view.KeyCharacterMap
import android.view.KeyEvent

// Telecomando della TV via HDMI-CEC (es. LG Magic Remote). Dal log CEC del box:
// - passano solo frecce, OK, Indietro (Exit), i 4 tasti colorati e i tasti media; Home, numeri,
//   Impostazioni e volume restano alla TV;
// - OK tenuto premuto non arriva come pressione lunga ma come raffica di pressioni brevi (~200ms).
// - sono eventi iniettati da HdmiControlService (dispositivo virtuale): il filtro tasti dei servizi di
//   accessibilità non li vede, quindi si possono gestire solo dentro LeanBox, non nelle altre app.
// Qui: raffica = pressione lunga, clic eseguito solo a raffica finita, azioni dei tasti colorati.
object RemoteKeys {

    // Oltre questo intervallo tra due OK la pressione è nuova; sotto, è la stessa tenuta premuta.
    // Misurato su LG Magic Remote: prima ripetizione dopo 385-505ms, poi ogni 160-311ms
    const val BURST_GAP_MS = 650L

    val CONFIRM = setOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_BUTTON_A)

    val COLORS = linkedMapOf(
        KeyEvent.KEYCODE_PROG_RED to "Rosso",
        KeyEvent.KEYCODE_PROG_GREEN to "Verde",
        KeyEvent.KEYCODE_PROG_YELLOW to "Giallo",
        KeyEvent.KEYCODE_PROG_BLUE to "Blu"
    )

    // Gli eventi del servizio HDMI-CEC arrivano dal dispositivo virtuale, ogni pressione è DOWN+UP separati
    fun isCec(event: KeyEvent) = event.deviceId == KeyCharacterMap.VIRTUAL_KEYBOARD

    private val lastDown = HashMap<Int, Long>()

    // Da chiamare una volta per ogni pressione (ACTION_DOWN): true se continua una raffica dello stesso
    // tasto, cioè il tasto è tenuto premuto sul telecomando TV. Tutti gli OK contano come un solo tasto
    fun continuesBurst(event: KeyEvent): Boolean {
        val key = if (event.keyCode in CONFIRM) KeyEvent.KEYCODE_DPAD_CENTER else event.keyCode
        val gap = event.eventTime - (lastDown[key] ?: 0L)
        lastDown[key] = event.eventTime
        return gap < BURST_GAP_MS
    }

    // Tasti colorati nella home di LeanBox
    val HOME_ACTIONS = linkedMapOf(
        KeyEvent.KEYCODE_PROG_RED to "Opzioni app",
        KeyEvent.KEYCODE_PROG_GREEN to "Preferito sì/no",
        KeyEvent.KEYCODE_PROG_YELLOW to "Disinstalla app",
        KeyEvent.KEYCODE_PROG_BLUE to "Impostazioni LeanBox"
    )
}
