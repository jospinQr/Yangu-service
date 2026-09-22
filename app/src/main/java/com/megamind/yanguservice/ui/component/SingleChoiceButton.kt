package com.megamind.yanguservice.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButtonDefaults.colors
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.megamind.yanguservice.domain.model.SendChannel

@Composable
fun SingleChoiceButton(
    selected: SendChannel,
    onSelect: (SendChannel) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val channels = SendChannel.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        channels.forEachIndexed { index, channel ->
            SegmentedButton(
                colors = colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                selected = selected == channel,
                onClick = { onSelect(channel) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = channels.size),
                label = { Text(if (channel == SendChannel.WHATSAPP) "WhatsApp" else "SMS") }
            )
        }
    }
}
