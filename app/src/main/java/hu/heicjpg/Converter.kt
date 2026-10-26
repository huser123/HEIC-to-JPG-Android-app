package hu.heicjpg

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.IOException
import java.util.Locale

/** All work here is called on the single conversion executor, never on the UI thread. */
class Converter(private val resolver: ContentResolver) {
    companion object {
        const val OUTPUT_FOLDER = "Pictures/HEIC-JPG/"
        const val QUALITY = 95

        fun jpgName(input: String): String {
            val clean = input.substringAfterLast('/').substringAfterLast('\\')
                .replace(Regex("[\\p{Cntrl}:*?\"<>|]"), "_")
            val stem = clean.substringBeforeLast('.', clean).trim().trim('.')
                .take(120).ifBlank { "kep" }
            return "$stem.jpg"
        }
    }

    fun displayName(uri: Uri): String = try {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "kep.heic"
    } catch (_: Exception) { "kep.heic" }

    fun convert(uri: Uri, name: String): Uri {
        // Inspect the actual decoder MIME, rather than trusting a filename or provider.
        val source = ImageDecoder.createSource(resolver, uri)
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val mime = info.mimeType.lowercase(Locale.ROOT)
            if (mime != "image/heif" && mime != "image/heic") {
                throw IOException("Ez a fájl nem HEIC/HEIF kép.")
            }
            // ImageDecoder applies EXIF rotation/mirroring. Never rotate the result again.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
            decoder.setOnPartialImageListener { false }
        }
        var jpegBitmap = decoded
        try {
            // Standard JPEG has no alpha. Flatten transparent pictures onto white.
            if (decoded.hasAlpha() || decoded.config != Bitmap.Config.ARGB_8888) {
                jpegBitmap = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888)
                Canvas(jpegBitmap).apply {
                    drawColor(Color.WHITE)
                    drawBitmap(decoded, 0f, 0f, null)
                }
            }
            return saveJpeg(jpegBitmap, jpgName(name))
        } finally {
            if (jpegBitmap !== decoded) jpegBitmap.recycle()
            decoded.recycle()
        }
    }

    private fun saveJpeg(bitmap: Bitmap, requestedName: String): Uri {
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        // MediaProvider also resolves filesystem name collisions, including files from other apps.
        val base = requestedName.removeSuffix(".jpg")
        var name = requestedName
        var suffix = 1
        while (exists(collection, name)) name = "${base}_${suffix++}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/HEIC-JPG/")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val output = resolver.insert(collection, values) ?: throw IOException("Nem sikerült létrehozni a JPG fájlt.")
        try {
            resolver.openOutputStream(output, "w")?.use {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it)) {
                    throw IOException("Nem sikerült a JPG kódolása.")
                }
                it.flush()
            } ?: throw IOException("Nem sikerült megnyitni a célfájlt.")
            val published = resolver.update(output, ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }, null, null)
            if (published != 1) throw IOException("Nem sikerült véglegesíteni a JPG fájlt.")
            return output
        } catch (error: Throwable) {
            // Never publish a truncated image; preserve the original exception.
            try { resolver.delete(output, null, null) } catch (_: Exception) { }
            throw error
        }
    }

    private fun exists(collection: Uri, name: String): Boolean =
        resolver.query(collection, arrayOf(MediaStore.Images.Media._ID),
            "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} = ?",
            arrayOf(name, OUTPUT_FOLDER), null)?.use { it.moveToFirst() } ?: false
}
