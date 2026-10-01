package nl.woolacast

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.woolacast.data.inbox.NewEpisodeNotifier
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
        // Na het draaien van het scherm of een herstart komt dezelfde intent terug; die is al verwerkt.
        if (savedInstanceState == null) {
            takeOpml(intent)
            takeNotification(intent)
        }

        setContent {
            val theme by appContainer.store.theme.collectAsStateWithLifecycle()
            WoolacastTheme(
                darkTheme = when (theme) {
                    "dark" -> true
                    "system" -> isSystemInDarkTheme()
                    else -> false
                }
            ) {
                WoolacastNav(appContainer)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        takeOpml(intent)
        takeNotification(intent)
    }

    /** Uit de melding over nieuwe afleveringen: meteen afspelen, of naar Nieuw. */
    private fun takeNotification(intent: Intent?) {
        intent ?: return
        intent.getStringExtra(NewEpisodeNotifier.EXTRA_PLAY)?.let { id ->
            NewEpisodeNotifier.cancel(this)
            container.incomingPlay.value = id
        }
        if (intent.getBooleanExtra(NewEpisodeNotifier.EXTRA_OPEN_NEW, false)) container.incomingOpenNew.value = true
        intent.removeExtra(NewEpisodeNotifier.EXTRA_PLAY)
        intent.removeExtra(NewEpisodeNotifier.EXTRA_OPEN_NEW)
    }

    /**
     * Een OPML-bestand dat met Toadcast geopend of gedeeld werd, bijvoorbeeld
     * de export uit Pocket Casts. De navigatie opent er het importscherm voor;
     * of het echt OPML is, blijkt daar bij het lezen.
     */
    private fun takeOpml(intent: Intent?) {
        val uri: Uri? = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            else -> null
        }
        if (uri != null) {
            container.incomingOpml.value = uri
            // Eén keer is genoeg; bij draaien van het scherm niet opnieuw importeren.
            intent?.action = null
        }
    }

    override fun onDestroy() {
        container.player.release()
        super.onDestroy()
    }
}
