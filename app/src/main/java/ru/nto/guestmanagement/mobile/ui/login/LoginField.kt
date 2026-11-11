package ru.nto.guestmanagement.mobile.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginField(
    onValueChange: (String) -> Unit,
    modifier: Modifier
){
    val state = rememberTextFieldState()
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { text ->
            onValueChange(text)
        }
    }
    BasicTextField(
        state = state,
        lineLimits = TextFieldLineLimits.SingleLine,
        decorator = {
            val text = state.text
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4){
                    Symbol(
                        text.getOrNull(it) ?: ' ',
                        isCursorVisible = false
                    )
                }
            }
        }
    )
}