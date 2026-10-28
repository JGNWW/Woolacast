package nl.woolacast.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
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
    onMainList { it.fling(Direction.DOWN) }
    onMainList { it.fling(Direction.UP) }
    openFirstRow()

    for (tab in listOf("Ontdek", "Bibliotheek", "Hitlijsten")) {
        fresh { device.findObject(By.text(tab))?.click() }
        device.waitForIdle()
        onMainList { it.fling(Direction.DOWN) }
    }
}

/**
 * Compose bouwt een lijst na het scrollen opnieuw op, en dan verwijst een
 * eerder gevonden object nergens meer heen. Dus per stap opnieuw zoeken, en
 * bij zo'n verlopen verwijzing nog eens proberen.
 */
private fun MacrobenchmarkScope.fresh(step: () -> Unit) {
    repeat(3) {
        try {
            step()
            return
        } catch (_: StaleObjectException) {
            device.waitForIdle()
        }
    }
}

/** Doet iets met de grootste lijst op het scherm; de rij categorieën bovenaan schuift ook. */
private fun MacrobenchmarkScope.onMainList(action: (UiObject2) -> Unit) = fresh {
    device.wait(Until.hasObject(By.scrollable(true)), 15_000)
    device.findObjects(By.scrollable(true)).maxByOrNull { it.visibleBounds.height() }?.let { list ->
        list.setGestureMargin(device.displayWidth / 5)
        action(list)
    }
}

/** Tikt op een rij bovenin de lijst, kijkt rond op de podcastpagina en gaat terug. */
private fun MacrobenchmarkScope.openFirstRow() {
    var opened = false
    onMainList { list ->
        val bounds = list.visibleBounds
        device.click(bounds.centerX(), bounds.top + bounds.height() / 4)
        opened = true
    }
    if (!opened) return
    device.waitForIdle()
    onMainList { it.fling(Direction.DOWN) }
    device.pressBack()
    device.waitForIdle()
}
