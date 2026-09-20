package app.shutterup.ui.day

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import app.shutterup.domain.model.Entry
import app.shutterup.domain.take.DiptychCrop
import app.shutterup.domain.take.TakeInterval
import app.shutterup.ui.components.Kicker
import coil3.compose.AsyncImage
import java.io.File

data class DiptychFrame(
    val date: java.time.LocalDate,
    val kicker: String,
    val entry: Entry?,
    val originalMissing: Boolean,
)

data class DiptychUi(
    val first: DiptychFrame,
    val second: DiptychFrame,
    val crop: DiptychCrop,
    val rest: List<java.time.LocalDate>,
)

@Composable
fun TakeDiptych(
    ui: DiptychUi,
    expanded: Boolean,
    onOpenPhoto: (Entry) -> Unit,
    onOpenDay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TakeFrame(
                    frame = ui.first,
                    crop = ui.crop,
                    pair = ui.second,
                    onOpenPhoto = onOpenPhoto,
                    compact = false,
                    modifier = Modifier.weight(1f),
                )
                TakeFrame(
                    frame = ui.second,
                    crop = ui.crop,
                    pair = ui.first,
                    onOpenPhoto = onOpenPhoto,
                    compact = false,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            TakeFrame(
                frame = ui.first,
                crop = ui.crop,
                pair = ui.second,
                onOpenPhoto = onOpenPhoto,
                compact = true,
            )
            TakeFrame(
                frame = ui.second,
                crop = ui.crop,
                pair = ui.first,
                onOpenPhoto = onOpenPhoto,
                compact = true,
            )
        }
        if (ui.rest.isNotEmpty()) {
            OtherTakesRow(dates = ui.rest, onOpenDay = onOpenDay)
        }
    }
}

@Composable
private fun OtherTakesRow(
    dates: List<java.time.LocalDate>,
    onOpenDay: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (dates.size == 1) "Another take" else "Other takes",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        dates.take(3).forEach { date ->
            TextButton(onClick = { onOpenDay(date.toString()) }) {
                Text(TakeInterval.friendlyDate(date))
            }
        }
        if (dates.size > 3) {
            Text(
                text = "+${dates.size - 3}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TakeFrame(
    frame: DiptychFrame,
    crop: DiptychCrop,
    pair: DiptychFrame,
    onOpenPhoto: (Entry) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val ratio = aspectRatio(frame.entry, pair.entry, crop)
    val file = frame.entry?.thumbPath?.let(::File)
    val hasFile = file != null && file.exists()
    val missing = frame.originalMissing || frame.entry == null
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (compact) Modifier.heightIn(max = 240.dp) else Modifier),
        ) {
            val height = min(maxWidth / ratio, maxHeight)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(shape)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = shape,
                    )
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .then(
                        if (frame.entry != null && hasFile && !missing) {
                            Modifier.clickable { onOpenPhoto(frame.entry) }
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    hasFile && !missing -> {
                        AsyncImage(
                            model = file,
                            contentDescription = frame.kicker,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = if (crop == DiptychCrop.SQUARE) {
                                ContentScale.Crop
                            } else {
                                ContentScale.Fit
                            },
                        )
                    }
                    missing -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.BrokenImage,
                                contentDescription = "Original missing",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Original missing",
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Kicker(text = frame.kicker)
    }
}

private fun aspectRatio(a: Entry?, b: Entry?, crop: DiptychCrop): Float {
    if (crop == DiptychCrop.SQUARE) return 1f
    val entry = a ?: b
    if (entry == null || entry.height <= 0) return 3f / 4f
    return entry.width.toFloat() / entry.height.toFloat()
}
