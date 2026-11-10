package ru.nto.guestmanagement.mobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.nto.guestmanagement.mobile.data.RequestsApi
import ru.nto.guestmanagement.mobile.data.SessionStorage


//пока примерно, буду ещё менять логику, т.к. не до конца поняла как оформить для успешной проверки задания
class LoginViewModel(
    private val requestsApi: RequestsApi,
    private val sessionStorage: SessionStorage
) : ViewModel() {
    private val _state = MutableStateFlow(LoginContract.State())
    val state = _state.asStateFlow()

    fun onIntent(intent: LoginContract.Intent) {
        when(intent){
            is LoginContract.Intent.EnterLogin -> enterLogin(intent.login)
            is LoginContract.Intent.ClickEnterButton -> login()
        }
    }

    private fun enterLogin(login: String){
        viewModelScope.launch {
            _state.update {
                it.copy(
                    login = login,
                    isButtonActive = filterLogin(login)
                )
            }
        }
    }

    private fun login(){
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    isButtonActive = false
                )
            }
            val code = _state.value.login!!
            // добавить обработку успеха/ошибки
            try {
                val auth = requestsApi.auth(code)
            }  catch (e : Exception){
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
            sessionStorage.save(code)
        }
    }

    private fun filterLogin(login: String) : Boolean{
        val isValid :Boolean =  login.all{ it.isLetterOrDigit() && !it.isWhitespace()} && login.length == 4
        return isValid
    }
}