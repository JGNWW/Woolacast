package nl.woolacast.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nl.woolacast.data.download.DownloadProgress
import nl.woolacast.data.local.DownloadRecord
import nl.woolacast.data.local.DownloadState
import nl.woolacast.ui.theme.LocalChartColors

/** Hoe een download er voor de gebruiker voor staat. */
sealed interface DownloadUi {
    data object None : DownloadUi
    /** In de rij; [wifi] als hij wacht tot er wifi is. */
    data class Waiting(val wifi: Boolean) : DownloadUi
    data class Running(val fraction: Float?) : DownloadUi
    data object Done : DownloadUi
    data class Failed(val reason: String) : DownloadUi
}

fun downloadUi(record: DownloadRecord?, progress: DownloadProgress?, waitingForWifi: Boolean): DownloadUi = when {
    record == null -> DownloadUi.None
    record.state == DownloadState.DONE -> DownloadUi.Done
    record.state == DownloadState.FAILED -> DownloadUi.Failed(record.error ?: "Mislukt")
    progress != null -> DownloadUi.Running(progress.fraction)
    else -> DownloadUi.Waiting(waitingForWifi)
}

/** De korte tekst die bij de toestand hoort, voor onder een titel. */
fun DownloadUi.label(): String? = when (this) {
    DownloadUi.None -> null
    is DownloadUi.Waiting -> if (wifi) "Wacht op wifi" else "In de rij"
    is DownloadUi.Running -> fraction?.let { "Downloaden ${(it * 100).toInt()}%" } ?: "Downloaden"
    DownloadUi.Done -> "Gedownload"
    is DownloadUi.Failed -> "Mislukt"
}

/**
 * Eén knop van 44 dp die de hele levensloop draagt: pijl omlaag om te
 * beginnen, een ring die volloopt (tik om te stoppen), een vinkje als hij er is,
 * en een waarschuwing als het misging (tik om opnieuw te proberen).
 */
@Composable
fun DownloadButton(
    state: DownloadUi,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val colors = LocalChartColors.current
    val action: () -> Unit
    val description: String
    when (state) {
        DownloadUi.None -> { action = onDownload; description = "Downloaden" }
        is DownloadUi.Waiting -> { action = onCancel; description = "Download stoppen" }
        is DownloadUi.Running -> { action = onCancel; description = "Download stoppen" }
        DownloadUi.Done -> { action = onDone; description = "Gedownload" }
        is DownloadUi.Failed -> { action = onRetry; description = "Opnieuw downloaden" }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = action)
            .semantics {
                contentDescription = description
                state.label()?.let { stateDescription = it }
            },
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            DownloadUi.None -> Icon(WoolIcons.Download, null, modifier = Modifier.size(22.dp))
            DownloadUi.Done -> Icon(WoolIcons.Downloaded, null, tint = colors.rise, modifier = Modifier.size(22.dp))
            is DownloadUi.Failed -> Icon(WoolIcons.Warning, null, tint = colors.fall, modifier = Modifier.size(21.dp))
            is DownloadUi.Waiting -> ProgressRing(null, dashed = true)
            is DownloadUi.Running -> ProgressRing(state.fraction, dashed = false)
        }
    }
}

/** Een ring van 22 dp met een stopblokje erin; zonder percentage draait hij niet maar is hij gestippeld. */
@Composable
private fun ProgressRing(fraction: Float?, dashed: Boolean) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(22.dp)) {
        val stroke = 2.4.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
            topLeft = Offset(inset, inset), size = arcSize,
            style = Stroke(stroke, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null)
        )
        if (fraction != null) {
            drawArc(
                color = accent, startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        val square = 6.dp.toPx()
        drawRoundRect(
            color = ink,
            topLeft = Offset((size.width - square) / 2, (size.height - square) / 2),
            size = Size(square, square),
            cornerRadius = CornerRadius(1.dp.toPx())
        )
    }
}

/** Megabytes of gigabytes, met een komma zoals in het Nederlands. */
fun byteSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return when {
        mb >= 1024 -> String.format(java.util.Locale("nl"), "%.1f GB", mb / 1024)
        mb >= 10 -> "${mb.toInt()} MB"
        else -> String.format(java.util.Locale("nl"), "%.1f MB", mb)
    }
}
