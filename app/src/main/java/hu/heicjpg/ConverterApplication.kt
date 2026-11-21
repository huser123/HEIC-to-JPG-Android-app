package hu.heicjpg

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

data class Item(
    val uri: Uri,
    val name: String,
    val statusResId: Int = R.string.item_selected,
    val failed: Boolean = false,
)

/** Application-scoped job and state survive activity recreation without leaking an Activity. */
class ConverterApplication : Application() {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val observers = mutableSetOf<() -> Unit>()
    var items: List<Item> = emptyList(); private set
    var busy = false; private set
    var loading = false; private set
    var completed = 0; private set
    var success = 0; private set
    var finished = false; private set

    fun observe(listener: () -> Unit) { observers.add(listener); listener() }
    fun unobserve(listener: () -> Unit) { observers.remove(listener) }
    private fun notifyChanged() { observers.toList().forEach { it() } }

    fun select(uris: List<Uri>) {
        if (busy || loading) return
        loading = true
        finished = false
        items = emptyList()
        completed = 0
        success = 0
        notifyChanged()
        worker.execute {
            val converter = Converter(contentResolver)
            val selected = uris.distinct().map { Item(it, converter.displayName(it)) }
            main.post { items = selected; loading = false; notifyChanged() }
        }
    }

    fun start() {
        if (busy || loading || finished || items.isEmpty()) return
        busy = true
        completed = 0
        success = 0
        notifyChanged()
        val batch = items.toList()
        worker.execute {
            val converter = Converter(contentResolver)
            batch.forEachIndexed { index, item ->
                main.post { update(index, R.string.item_converting, false) }
                var ok = false
                val status = try {
                    converter.convert(item.uri, item.name)
                    ok = true
                    R.string.item_saved
                } catch (_: OutOfMemoryError) {
                    R.string.item_out_of_memory
                } catch (_: SecurityException) {
                    R.string.item_file_unavailable
                } catch (_: NotHeicException) {
                    R.string.item_not_heic
                } catch (_: Exception) {
                    R.string.item_conversion_failed
                }
                val succeeded = ok
                main.post {
                    completed++
                    if (succeeded) success++
                    update(index, status, !succeeded)
                }
            }
            main.post { busy = false; finished = true; notifyChanged() }
        }
    }

    private fun update(index: Int, statusResId: Int, failed: Boolean) {
        items = items.mapIndexed { i, item -> if (i == index) item.copy(statusResId = statusResId, failed = failed) else item }
        notifyChanged()
    }
}
