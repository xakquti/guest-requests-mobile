package ru.nto.guestmanagement.mobile.ui.form

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun Field(
    value : String,
    hintText : String,
    fieldName : String,
    modifier: Modifier,
    onValueChange : (String) -> Unit
){
    val state = rememberTextFieldState(value)
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { text ->
            onValueChange(text)
        }
    }

    TextField(
        state = state,
        textStyle = MaterialTheme.typography.titleLarge,
        modifier = modifier
            .shadow(elevation = 4.dp, RoundedCornerShape(24.dp), spotColor = MaterialTheme.colorScheme.primary)
            .clip(RoundedCornerShape(24.dp)),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        placeholder = {
            Text(
                text = hintText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
            )
        },
        label = {
            Text(
                text = fieldName,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
        },
        )
}