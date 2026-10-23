package com.michde.leanbox.icons

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import java.io.File

// Esporta le icone originali delle app installate (massima densità, 432 px) in
// Android/data/<pack>/files/export, con components.tsv (componente, etichetta). Serve a
// tools/style_icons.py per disegnare le icone del pack partendo da quelle vere.
// Avvio: adb shell am start -n com.michde.leanbox.icons/.ExportActivity
class ExportActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread {
            val out = File(getExternalFilesDir(null), "export").apply {
                deleteRecursively()
                mkdirs()
            }
            val seen = HashSet<String>()
            val rows = ArrayList<String>()
            for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
                val query = Intent(Intent.ACTION_MAIN).addCategory(category)
                for (info in packageManager.queryIntentActivities(query, 0)) {
                    val component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                    if (component.packageName == packageName || !seen.add(component.flattenToString())) continue
                    val icon = highResIcon(info.activityInfo.iconResource.takeIf { it != 0 } ?: info.activityInfo.applicationInfo.icon,
                        info.activityInfo.packageName) ?: info.activityInfo.loadIcon(packageManager)
                    save(icon, File(out, "${component.packageName}__${component.className}.png"))
                    rows.add("${component.flattenToString()}\t${info.loadLabel(packageManager)}")
                }
            }
            File(out, "components.tsv").writeText(rows.joinToString("\n"))
            runOnUiThread { finish() }
        }.start()
    }

    private fun highResIcon(id: Int, pkg: String): Drawable? = try {
        packageManager.getResourcesForApplication(pkg).getDrawableForDensity(id, DisplayMetrics.DENSITY_XXXHIGH, null)
    } catch (_: Exception) {
        null
    }

    private fun save(icon: Drawable, file: File) {
        val size = 432
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, size, size)
        icon.draw(Canvas(bitmap))
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
