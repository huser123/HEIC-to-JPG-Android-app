package hu.heicjpg

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

data class Item(val uri: Uri, val name: String, val status: String = "Kiválasztva", val failed: Boolean = false)

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
                main.post { update(index, "Átalakítás…", false) }
                var ok = false
                val status = try {
                    converter.convert(item.uri, item.name)
                    ok = true
                    "Elmentve JPG-ként"
                } catch (_: OutOfMemoryError) {
                    "Nincs elég szabad memória ehhez a képhez."
                } catch (_: SecurityException) {
                    "A fájl már nem érhető el. Válaszd ki újra."
                } catch (error: Exception) {
                    when {
                        error.message == "Ez a fájl nem HEIC/HEIF kép." -> error.message!!
                        else -> "Nem sikerült: sérült/nem támogatott kép, vagy mentési hiba."
                    }
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

    private fun update(index: Int, status: String, failed: Boolean) {
        items = items.mapIndexed { i, item -> if (i == index) item.copy(status = status, failed = failed) else item }
        notifyChanged()
    }
}
