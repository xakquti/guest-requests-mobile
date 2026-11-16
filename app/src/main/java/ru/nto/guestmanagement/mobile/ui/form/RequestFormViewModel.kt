package ru.nto.guestmanagement.mobile.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.nto.guestmanagement.mobile.data.RequestsApi
import ru.nto.guestmanagement.mobile.data.SessionStorage

class RequestFormViewModel(
    private val requestsApi: RequestsApi,
    private val sessionStorage: SessionStorage,
    sessionExpired : Boolean
) : ViewModel() {
    private val _state = MutableStateFlow(RequestFormContract.State())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<RequestFormContract.Event>()
    val event = _event.asSharedFlow()

    init{
        if(sessionExpired){
            viewModelScope.launch {
                sessionStorage.clear()
            }
            _state.update {
                it.copy(
                    isSessionExpired = true
                )
            }
        }
    }

    public fun onIntent(intent : RequestFormContract.Intent){
        when(intent){
            is RequestFormContract.Intent.OnFormSend -> sendForm()
            is RequestFormContract.Intent.EnterFullName -> TODO()
            is RequestFormContract.Intent.EnterOrganization -> TODO()
            is RequestFormContract.Intent.EnterPhoneNumber -> TODO()
            is RequestFormContract.Intent.EnterPurpose -> TODO()
            is RequestFormContract.Intent.EnterValidFrom -> TODO()
            is RequestFormContract.Intent.EnterValidTo -> TODO()
        }
    }

    private fun sendForm(){
        _state.update {
            it.copy(
                isLoading = true
            )
        }
        val fullName = _state.value.fio
        val organization = _state.value.organization
        val phoneNumber = _state.value.phoneNumber
        val purpose = _state.value.purpose
        val validFrom = _state.value.validFrom
        val validTo = _state.value.validTo

        if (validateFullName(fullName) != null){
            _state.update {
                it.copy(errorFullName = validateFullName(fullName),
                    isLoading = false
                )
            }
        }


        if (validatePhone(phoneNumber) != null){
            _state.update {
                it.copy(
                    errorPhoneNumber = validatePhone(phoneNumber),
                    isLoading = false
                )
            }
        }

        if (organization != null && organization.length > 150){
            _state.update {
                it.copy(
                    errorOrganization = "Количество символов превышает 150",
                    isLoading = false
                )
            }
        }

        if (validatePurpose(purpose) != null){
            _state.update {
                it.copy(
                    errorPurpose = validatePurpose(purpose),
                    isLoading = false
                )
            }
        }


    }

    private fun validateFullName(fullName : String?) : String? {
        return if (fullName.isNullOrEmpty()){
            "Отсутвует ФИО"
        } else if (fullName.trim().length in 5..150){
            "ФИО должно быть в пределах 5-150 символов"
        } else {
            null
        }
    }

    private fun validatePhone(phone : String?) : String? {
        if (phone.isNullOrEmpty()){
            return "Отсутствует номер телефона"
        }
        val phoneTrim = phone.trim()

        return if (!(phoneTrim.startsWith("+7")) || (phoneTrim.length != 12) || !(phoneTrim.substring(1..11).any { it !in '0'..'9' })){
            "Номер телефона не соответствует формату"
        } else {
            null
        }
    }

    private fun validatePurpose(purpose : String?) : String? {
        return if (purpose.isNullOrEmpty()){
            "Цель отсутствует"
        } else if (purpose.trim().length !in 5..200){
            "Количство символов 5-200"
        } else {
            null
        }
    }

}