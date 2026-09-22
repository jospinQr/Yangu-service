package com.megamind.yanguservice.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.megamind.yanguservice.R


@Composable
fun MessagetextField(
    modifier: Modifier = Modifier,
    message: String,
    enabled: Boolean = true,
    showImageAction: Boolean = true,
    onMessageChange: (String) -> Unit,
    onTrailingAction: () -> Unit,
    onLeadingAction: () -> Unit,
) {


    TextField(

        value = message,
        onValueChange = onMessageChange,
        modifier = modifier,
        enabled = enabled,
        placeholder = {
            Text("Message")
        },
        shape = MaterialTheme.shapes.extraExtraLarge,
        colors = TextFieldDefaults.colors(
            errorIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        trailingIcon = if (showImageAction) {
            {
                AnimatedVisibility(showImageAction) {
                    IconButton(onClick = onTrailingAction, enabled = enabled) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_image_24),
                            contentDescription = "Pièce jointe"
                        )
                    }
                }
            }
        } else null,
        leadingIcon = {

            IconButton(onClick = onLeadingAction, enabled = enabled) {
                Icon(
                    painter = painterResource(R.drawable.baseline_person_24),
                    contentDescription = "Pièce jointe"
                )
            }
        }


    )
}
