package nl.woolacast

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import nl.woolacast.ui.WoolacastNav
import nl.woolacast.ui.theme.WoolacastTheme

class MainActivity : ComponentActivity() {

    /**
     * Vanaf Android 13 is de afspeelmelding — de bediening in het
     * meldingenpaneel en op het vergrendelscherm — pas zichtbaar na
     * toestemming. Zonder die vraag lijkt de achtergrondspeler te ontbreken.
     */
    private val askNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* geweigerd: afspelen werkt nog, alleen de melding blijft weg */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val appContainer = container
        appContainer.player.connect()

        setContent {
            WoolacastTheme {
                WoolacastNav(appContainer)
            }
        }
    }

    override fun onDestroy() {
        container.player.release()
        super.onDestroy()
    }
}
