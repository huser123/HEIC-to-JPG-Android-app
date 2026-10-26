package hu.heicjpg

import android.content.ContentUris
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.test.InstrumentationTestCase
import java.io.File
import java.util.UUID

/** On-device tests exercise Android's real HEIF codec and MediaStore, with no mocks. */
@Suppress("DEPRECATION")
class ConverterTest : InstrumentationTestCase() {
    private val created = mutableListOf<Uri>()
    private val inputs = mutableListOf<File>()
    private val prefix = "heicjpg_test_${UUID.randomUUID()}"
    private val target get() = instrumentation.targetContext

    private fun input(asset: String, label: String = asset): Uri {
        val file = File(target.cacheDir, "${prefix}_$label")
        instrumentation.context.assets.open(asset).use { source -> file.outputStream().use { source.copyTo(it) } }
        inputs.add(file)
        return Uri.fromFile(file)
    }

    private fun convert(uri: Uri, name: String): Uri =
        Converter(target.contentResolver).convert(uri, "${prefix}_$name").also { created.add(it) }

    fun testHeicToJpegPreservesDimensionsAndOriginal() {
        val uri = input("normal.heic")
        val original = File(uri.path!!).readBytes()
        val output = convert(uri, "normal.heic")
        val bytes = target.contentResolver.openInputStream(output)!!.use { it.readBytes() }
        assertEquals(0xff, bytes[0].toInt() and 255)
        assertEquals(0xd8, bytes[1].toInt() and 255)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertNotNull(bitmap)
        assertEquals(96, bitmap.width)
        assertEquals(64, bitmap.height)
        assertTrue(android.graphics.Color.red(bitmap.getPixel(20, 32)) > 220)
        assertTrue(android.graphics.Color.blue(bitmap.getPixel(75, 32)) > 220)
        bitmap.recycle()
        assertTrue(original.contentEquals(File(uri.path!!).readBytes()))
        target.contentResolver.query(output,
            arrayOf(MediaStore.Images.Media.IS_PENDING, MediaStore.Images.Media.RELATIVE_PATH,
                MediaStore.Images.Media.MIME_TYPE), null, null, null)!!.use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
            assertEquals(Converter.OUTPUT_FOLDER, it.getString(1))
            assertEquals("image/jpeg", it.getString(2))
        }
    }

    fun testOrientationAppliedExactlyOnce() {
        val output = convert(input("rotated.heic"), "rotated.heic")
        val bitmap = target.contentResolver.openInputStream(output)!!.use { BitmapFactory.decodeStream(it) }
        assertEquals(64, bitmap.width)
        assertEquals(96, bitmap.height)
        // Original red left half must become the top half after 90 degree clockwise rotation.
        assertTrue(android.graphics.Color.red(bitmap.getPixel(32, 20)) > 220)
        assertTrue(android.graphics.Color.blue(bitmap.getPixel(32, 75)) > 220)
        bitmap.recycle()
    }

    fun testDuplicateNamesNeverOverwrite() {
        val input = input("normal.heic")
        val a = convert(input, "duplicate.heic")
        val b = convert(input, "duplicate.heic")
        assertFalse(a == b)
        val converter = Converter(target.contentResolver)
        assertEquals("${prefix}_duplicate.jpg", converter.displayName(a))
        assertEquals("${prefix}_duplicate_1.jpg", converter.displayName(b))
    }

    fun testCorruptAndWrongFormatAreRejectedWithoutOutput() {
        for (asset in listOf("broken.heic", "not-heic.png")) {
            try {
                convert(input(asset), asset)
                fail("Invalid input accepted: $asset")
            } catch (_: java.io.IOException) { }
        }
        assertEquals(0, created.size)
    }

    fun testBatchContinuesAfterCorruptImage() {
        val uris = listOf(input("normal.heic", "batch_first.heic"),
            input("broken.heic", "batch_broken.heic"), input("normal.heic", "batch_last.heic"))
        val model = target.applicationContext as ConverterApplication
        instrumentation.runOnMainSync { model.select(uris) }
        await { !model.loading }
        instrumentation.runOnMainSync { model.start() }
        await { model.finished }
        instrumentation.runOnMainSync {
            assertEquals(3, model.completed)
            assertEquals(2, model.success)
            assertFalse(model.busy)
            assertFalse(model.items[0].failed)
            assertTrue(model.items[1].failed)
            assertFalse(model.items[2].failed)
        }
    }

    fun testSafeFileNames() {
        assertEquals("photo.jpg", Converter.jpgName("photo.HEIC"))
        assertEquals("photo.jpg", Converter.jpgName("/some/path/photo.heif"))
        assertEquals("kep.jpg", Converter.jpgName(".heic"))
        assertEquals("a_b.jpg", Converter.jpgName("a:b.heic"))
    }

    private fun await(condition: () -> Boolean) {
        val limit = System.currentTimeMillis() + 30000
        while (true) {
            var done = false
            instrumentation.runOnMainSync { done = condition() }
            if (done) return
            if (System.currentTimeMillis() > limit) fail("Timed out waiting for conversion")
            Thread.sleep(50)
        }
    }

    override fun tearDown() {
        val resolver = target.contentResolver
        created.forEach { resolver.delete(it, null, null) }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        resolver.query(collection, arrayOf(MediaStore.Images.Media._ID),
            "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?", arrayOf("$prefix%"), null)?.use {
            while (it.moveToNext()) resolver.delete(ContentUris.withAppendedId(collection, it.getLong(0)), null, null)
        }
        inputs.forEach { it.delete() }
        super.tearDown()
    }
}
