package hu.heicjpg

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val model get() = application as ConverterApplication
    private val teal = Color.rgb(8, 127, 115)
    private val ink = Color.rgb(24, 45, 41)
    private lateinit var pick: Button
    private lateinit var convert: Button
    private lateinit var status: TextView
    private lateinit var details: TextView
    private lateinit var progress: ProgressBar
    private lateinit var list: LinearLayout
    private var renderedItems: List<Item>? = null
    private val observer: () -> Unit = { render() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        // Content URI permissions are rechecked when each input is opened.
        if (model.items.isEmpty() && !model.loading) {
            savedInstanceState?.getStringArrayList("selection")?.let { saved ->
                if (saved.isNotEmpty()) model.select(saved.map(Uri::parse))
            }
        }
    }

    override fun onStart() { super.onStart(); model.observe(observer) }
    override fun onStop() { model.unobserve(observer); super.onStop() }
    override fun onSaveInstanceState(outState: Bundle) {
        // Do not restore a completed batch as new work after process death.
        if (!model.finished && !model.busy) {
            outState.putStringArrayList("selection", ArrayList(model.items.map { it.uri.toString() }))
        }
        super.onSaveInstanceState(outState)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int = 18) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun text(value: String, size: Float, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
        setLineSpacing(dp(3).toFloat(), 1f)
    }
    private fun add(parent: LinearLayout, child: View, top: Int = 0) {
        parent.addView(child, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) })
    }
    private fun button(label: String, primary: Boolean, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 16f; minHeight = dp(54)
        setTextColor(if (primary) Color.WHITE else teal)
        backgroundTintList = android.content.res.ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(if (primary) teal else Color.rgb(220, 240, 233), Color.rgb(218, 225, 222)))
        setOnClickListener { action() }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 248, 247))
        }
        root.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            insets
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(30), dp(24), dp(28))
        }
        add(body, text("KÉPÁTALAKÍTÓ", 12f, teal, true))
        add(body, text("HEIC → JPG", 34f, bold = true), 10)
        add(body, text("Válaszd ki a képeket. A többit elintézzük helyben, a telefonodon.", 16f,
            Color.rgb(85, 105, 99)), 10)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(20))
            background = shape(Color.WHITE)
        }
        add(card, text("Eredeti méret · Kiváló minőség", 16f, bold = true))
        add(card, text("JPG, 95-ös minőségi beállítás. Az eredeti képek megmaradnak.", 14f,
            Color.rgb(85, 105, 99)), 8)
        pick = button("Képek kiválasztása", false) { chooseImages() }
        add(card, pick, 18)
        convert = button("Konvertálás", true) { model.start() }
        add(card, convert, 6)
        add(body, card, 26)

        status = text("Még nincs kiválasztott kép", 18f, bold = true)
        status.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        add(body, status, 24)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            progressTintList = android.content.res.ColorStateList.valueOf(teal)
        }
        add(body, progress, 12)
        details = text("", 14f, Color.rgb(85, 105, 99))
        add(body, details, 10)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        add(body, list, 12)
        add(body, text("MENTÉS HELYE", 11f, teal, true), 28)
        add(body, text("Pictures/HEIC-JPG", 15f, bold = true), 5)
        add(body, text("A kész JPG-k a galériában is megjelennek.", 13f, Color.rgb(85, 105, 99)), 6)
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, -1))
        setContentView(root)
        root.requestApplyInsets()
    }

    private fun chooseImages() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            // image/* intentionally lets mislabelled HEIC files appear in Xiaomi's picker.
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try { startActivityForResult(intent, 10) }
        catch (_: android.content.ActivityNotFoundException) {
            AlertDialog.Builder(this).setMessage("Nem található rendszer-fájlválasztó ezen a telefonon.")
                .setPositiveButton("Rendben", null).show()
        }
    }

    @Deprecated("Uses platform API to avoid additional runtime dependencies")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 10 || resultCode != RESULT_OK || data == null) return
        val uris = mutableListOf<Uri>()
        data.clipData?.let { clips -> repeat(clips.itemCount) { uris.add(clips.getItemAt(it).uri) } }
            ?: data.data?.let { uris.add(it) }
        if (uris.isEmpty()) return
        // Keep only the current selection's persisted grants; do not accumulate access forever.
        val selected = uris.distinct()
        contentResolver.persistedUriPermissions.filter { it.uri !in selected }.forEach {
            try { contentResolver.releasePersistableUriPermission(it.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: SecurityException) { }
        }
        selected.forEach {
            try { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: SecurityException) { /* transient permission still allows immediate conversion */ }
        }
        model.select(selected)
    }

    private fun render() {
        val locked = model.busy || model.loading
        pick.isEnabled = !locked
        pick.text = if (model.items.isEmpty()) "Képek kiválasztása" else "Más képek kiválasztása"
        convert.isEnabled = !locked && model.items.isNotEmpty() && !model.finished
        convert.text = if (model.busy) "Konvertálás folyamatban…" else "Konvertálás"
        progress.visibility = if (model.busy || model.loading) View.VISIBLE else View.GONE
        progress.isIndeterminate = model.loading
        progress.max = model.items.size.coerceAtLeast(1)
        progress.progress = model.completed
        status.text = when {
            model.loading -> "Képek beolvasása…"
            model.busy -> "Átalakítás: ${model.completed} / ${model.items.size}"
            model.finished -> "Kész: ${model.success} / ${model.items.size} sikeres"
            model.items.isNotEmpty() -> "${model.items.size} kép kiválasztva"
            else -> "Még nincs kiválasztott kép"
        }
        details.text = when {
            model.busy -> "Hagyd nyitva az alkalmazást a folyamat végéig."
            model.finished && model.success < model.items.size -> "${model.items.size - model.success} kép nem sikerült. A részletek a listában láthatók."
            model.finished -> "A JPG-ket elmentettük a Pictures/HEIC-JPG mappába."
            model.items.isNotEmpty() -> "A kezdéshez koppints a Konvertálás gombra."
            else -> "Egy vagy több HEIC/HEIF képet is kijelölhetsz."
        }
        if (model.busy) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Reuse rows on progress updates, instead of repeatedly rebuilding a long batch list.
        if (renderedItems?.map { it.uri } != model.items.map { it.uri }) {
            list.removeAllViews()
            model.items.forEach { item ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12))
                    background = shape(Color.WHITE, 12)
                }
                add(row, text(item.name, 14f, bold = true))
                add(row, text(item.status, 13f), 4)
                add(list, row, 8)
            }
        }
        model.items.forEachIndexed { index, item ->
            val row = list.getChildAt(index) as LinearLayout
            (row.getChildAt(1) as TextView).apply {
                text = item.status
                setTextColor(if (item.failed) Color.rgb(166, 49, 40) else teal)
            }
        }
        renderedItems = model.items
    }
}
