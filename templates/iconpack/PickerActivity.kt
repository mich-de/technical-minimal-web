package com.michde.leanbox.icons

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import org.xmlpull.v1.XmlPullParser

// Griglia di tutte le icone del pack (elenco da res/xml/drawable.xml). Con ACTION_PICK_ICON
// restituisce l'icona scelta come bitmap ("icon" ADW e chiave di LeanBox) e come risorsa
class PickerActivity : Activity() {

    private val names = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val parser = resources.getXml(R.xml.drawable)
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                parser.getAttributeValue(null, "drawable")?.let { names.add(it) }
            }
        }
        val picking = intent.action == "org.adw.launcher.icons.ACTION_PICK_ICON"
        val dp = resources.displayMetrics.density
        val grid = GridView(this).apply {
            numColumns = GridView.AUTO_FIT
            columnWidth = (96 * dp).toInt()
            stretchMode = GridView.STRETCH_SPACING
            val pad = (32 * dp).toInt()
            setPadding(pad, pad, pad, pad)
            verticalSpacing = (16 * dp).toInt()
            clipToPadding = false
            setBackgroundColor(Color.parseColor("#0E0E0E"))
            // Selettore di fuoco visibile col telecomando: bordo arancio attorno all'icona
            selector = GradientDrawable().apply {
                cornerRadius = 20 * dp
                setStroke((3 * dp).toInt(), Color.parseColor("#FF5A1F"))
            }
            setDrawSelectorOnTop(true)
            adapter = IconAdapter()
            setOnItemClickListener { _, _, position, _ -> if (picking) pick(names[position]) }
        }
        setContentView(grid)
    }

    private fun pick(name: String) {
        val id = resources.getIdentifier(name, "drawable", packageName)
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        getDrawable(id)?.apply {
            setBounds(0, 0, size, size)
            draw(Canvas(bitmap))
        }
        setResult(RESULT_OK, Intent().apply {
            putExtra("icon", bitmap)
            putExtra("org.adw.launcher.icons.extra.ICON_BITMAP", bitmap)
            putExtra("org.adw.launcher.icons.extra.ICON_RESOURCE", ComponentName(packageName, name))
            putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this@PickerActivity, id))
        })
        finish()
    }

    private inner class IconAdapter : BaseAdapter() {
        override fun getCount() = names.size
        override fun getItem(position: Int) = names[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val dp = resources.displayMetrics.density
            val view = (convertView as? ImageView) ?: ImageView(this@PickerActivity).apply {
                layoutParams = ViewGroup.LayoutParams((88 * dp).toInt(), (88 * dp).toInt())
                isFocusable = false
                setPadding((6 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt())
            }
            view.setImageResource(resources.getIdentifier(names[position], "drawable", packageName))
            return view
        }
    }
}
