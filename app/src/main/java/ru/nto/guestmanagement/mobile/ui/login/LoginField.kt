package ru.nto.guestmanagement.mobile.ui.login

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.nto.guestmanagement.mobile.R

@Composable
fun LoginField(
    value : String,
    onValueChange: (String) -> Unit,
    modifier: Modifier
) {
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
            .clip(RoundedCornerShape(24.dp))
            .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(24.dp)),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        label = {
            Text(
                text = stringResource(R.string.login_code_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
            )
        }
    )
}