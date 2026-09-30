package nl.woolacast.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import nl.woolacast.R
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * De knoppen van de mediamelding, getekend door Android zelf: wit op
 * doorzichtig, elk in een vak van 96 px. Om naast een schermafbeelding van
 * een echte melding te leggen. Draait alleen met -Pscreenshots; de PNG's komen
 * in app/build/screenshots/mediaknoppen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class MediaButtonScreenshots {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun mediaButtons() {
        assumeTrue("alleen met -Pscreenshots", System.getProperty("woolacast.screenshots") == "true")
        val outDir = File(System.getProperty("woolacast.screenshotDir") ?: "build/screenshots", "mediaknoppen")
            .apply { mkdirs() }
        val icons = mapOf(
            "speed_0_8" to R.drawable.ic_notif_speed_0_8,
            "speed_1_0" to R.drawable.ic_notif_speed_1_0,
            "speed_1_2" to R.drawable.ic_notif_speed_1_2,
            "speed_1_5" to R.drawable.ic_notif_speed_1_5,
            "speed_1_8" to R.drawable.ic_notif_speed_1_8,
            "speed_2_0" to R.drawable.ic_notif_speed_2_0,
            "skip_back" to R.drawable.ic_notif_skip_back,
            "skip_forward" to R.drawable.ic_notif_skip_forward,
            "play" to R.drawable.ic_notif_play,
            "pause" to R.drawable.ic_notif_pause,
            "star" to R.drawable.ic_notif_star,
            "star_filled" to R.drawable.ic_notif_star_filled
        )
        for ((name, res) in icons) {
            val drawable = ContextCompat.getDrawable(context, res)!!
            val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, SIZE, SIZE)
            drawable.draw(Canvas(bitmap))
            File(outDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private companion object {
        const val SIZE = 96
    }
}
