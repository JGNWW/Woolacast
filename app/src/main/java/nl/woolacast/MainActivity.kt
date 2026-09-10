package nl.woolacast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import nl.woolacast.ui.WoolacastNav
import nl.woolacast.ui.theme.WoolacastTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
