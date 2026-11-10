package ru.nto.guestmanagement.mobile.ui.login

sealed interface LoginContract{
    data class State(
        val isLoading: Boolean = false,
        val error: String? = null,
        val login: String? = null,
        val isButtonActive : Boolean = false
    )

    sealed interface Intent {
        data class EnterLogin(val login: String) : Intent
        data object ClickEnterButton : Intent
    }
}