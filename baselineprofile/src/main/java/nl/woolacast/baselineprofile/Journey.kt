package nl.woolacast.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

const val PACKAGE = "nl.woolacast"

/**
 * Wat een gewone sessie doet: opstarten, door de hitlijst scrollen, een
 * podcast openen, en langs de andere tabbladen. Alles op naam en vorm, niet op
 * plek: de inhoud van de lijsten verschilt per dag.
 */
fun MacrobenchmarkScope.startApp() {
    pressHome()
    // Anders staat de vraag om meldingen over de hitlijst heen.
    device.executeShellCommand("pm grant $PACKAGE android.permission.POST_NOTIFICATIONS")
    startActivityAndWait()
}

fun MacrobenchmarkScope.browse() {
    // De hitlijst komt van het net; geef hem even.
    device.wait(Until.hasObject(By.text("Hitlijsten")), 10_000)
    device.waitForIdle()
    mainList()?.let { list ->
        list.scroll(Direction.DOWN)
        list.scroll(Direction.UP)
        openFirstRow(list)
    }

    for (tab in listOf("Ontdek", "Bibliotheek", "Hitlijsten")) {
        device.findObject(By.text(tab))?.click() ?: continue
        device.waitForIdle()
        mainList()?.scroll(Direction.DOWN)
    }
}

/** De grootste lijst op het scherm; de rij categorieën bovenaan schuift ook. */
private fun MacrobenchmarkScope.mainList(): UiObject2? {
    device.wait(Until.hasObject(By.scrollable(true)), 15_000)
    return device.findObjects(By.scrollable(true)).maxByOrNull { it.visibleBounds.height() }
        ?.also { it.setGestureMargin(device.displayWidth / 5) }
}

private fun UiObject2.scroll(direction: Direction) {
    runCatching { fling(direction) }
}

/** Tikt op een rij bovenin de lijst, kijkt rond op de podcastpagina en gaat terug. */
private fun MacrobenchmarkScope.openFirstRow(list: UiObject2) {
    val bounds = list.visibleBounds
    device.click(bounds.centerX(), bounds.top + bounds.height() / 4)
    device.waitForIdle()
    device.wait(Until.hasObject(By.scrollable(true)), 10_000)
    mainList()?.scroll(Direction.DOWN)
    device.pressBack()
    device.waitForIdle()
}
