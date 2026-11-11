package ru.nto.guestmanagement.mobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.nto.guestmanagement.mobile.data.ApiResult
import ru.nto.guestmanagement.mobile.data.RequestsApi
import ru.nto.guestmanagement.mobile.data.SessionStorage


class LoginViewModel(
    private val requestsApi: RequestsApi,
    private val sessionStorage: SessionStorage
) : ViewModel() {
    private val _state = MutableStateFlow(LoginContract.State())
    val state = _state.asStateFlow()

    private val _action = MutableSharedFlow<LoginContract.Action>()
    val action = _action.asSharedFlow()

    fun onIntent(intent: LoginContract.Intent) {
        when(intent){
            is LoginContract.Intent.EnterLogin -> enterLogin(intent.login)
            is LoginContract.Intent.ClickEnterButton -> login()
        }
    }

    private fun enterLogin(login: String){
        _state.update {
            it.copy(
                login = login,
                isButtonActive = filterLogin(login),
                error = null
            )
        }
    }

    private fun login(){
        if (_state.value.isLoading){
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    isButtonActive = false,
                    error = null
                )
            }
            val code = _state.value.login ?: return@launch

            when(val auth = requestsApi.auth(code)){
                is ApiResult.Success<*> -> {
                    sessionStorage.save(code)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                    _action.emit(LoginContract.Action.NavigateToMainApp)
                }
                is ApiResult.Failure -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = auth.message,
                            isButtonActive = filterLogin(it.login)
                        )
                    }
                }
                is ApiResult.Network -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Нет связи",
                            isButtonActive = filterLogin(it.login)
                        )
                    }
                }
                is ApiResult.Conflict -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = auth.message,
                            isButtonActive = filterLogin(it.login)
                        )
                    }
                }

                is ApiResult.FieldErrors -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка в поле",
                            isButtonActive = filterLogin(it.login)
                        )
                    }
                }
                ApiResult.NotFound -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка в поле",
                            isButtonActive = filterLogin(it.login)
                        )
                    }
                }
                ApiResult.Unauthorized -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Неизвестный код",
                            isButtonActive = filterLogin(it.login)
                        )
                    }
                }
            }
        }
    }

    private fun filterLogin(login: String?) : Boolean{
        val isValid :Boolean = login != null && login.all{ ((it in 'a'..'z') || (it in 'A'..'Z') || (it in '0'..'9')) } && login.length == 4
        return isValid
    }
}