package ru.nto.guestmanagement.mobile.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.nto.guestmanagement.mobile.data.ApiResult
import ru.nto.guestmanagement.mobile.data.CreateRequestBody
import ru.nto.guestmanagement.mobile.data.RequestsApi
import ru.nto.guestmanagement.mobile.data.SessionStorage
import ru.nto.guestmanagement.mobile.data.TimeProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

class RequestFormViewModel(
    private val requestsApi: RequestsApi,
    private val sessionStorage: SessionStorage,
    private val timeProvider: TimeProvider,
    sessionExpired : Boolean
) : ViewModel() {
    val dateFormat: DateTimeFormatter? = DateTimeFormatter.ofPattern("dd.MM.yyyy").withResolverStyle(ResolverStyle.STRICT)
    val timeFormat: DateTimeFormatter? = DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT)
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

    fun onIntent(intent : RequestFormContract.Intent){
        when(intent){
            is RequestFormContract.Intent.OnFormSend -> sendForm()
            is RequestFormContract.Intent.EnterFullName -> {
                _state.update {
                    it.copy(
                        fio = intent.fullName,
                        isButtonActive = true
                    )
                }
            }
            is RequestFormContract.Intent.EnterOrganization -> {
                _state.update {
                    it.copy(
                        organization = intent.organization,
                        isButtonActive = true
                    )
                }
            }
            is RequestFormContract.Intent.EnterPhoneNumber -> {
                _state.update {
                    it.copy(
                        phoneNumber = intent.phoneNumber,
                        isButtonActive = true
                    )
                }
            }
            is RequestFormContract.Intent.EnterPurpose -> {
                _state.update {
                    it.copy(
                        purpose = intent.purpose,
                        isButtonActive = true
                    )
                }
            }
            is RequestFormContract.Intent.EnterValidFrom -> {
                _state.update {
                    it.copy(
                        validFrom = intent.validFrom,
                        isButtonActive = true
                    )
                }
            }
            is RequestFormContract.Intent.EnterValidTo -> {
                _state.update {
                    it.copy(
                        validTo = intent.validTo,
                        isButtonActive = true
                    )
                }
            }

            is RequestFormContract.Intent.EnterDate -> {
                _state.update {
                    it.copy(
                        date = intent.date,
                        isButtonActive = true
                    )
                }
            }
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
        val date = _state.value.date
        val timeNow = timeProvider.now()

        if (validateFullName(fullName) != null){
            _state.update {
                it.copy(errorFullName = validateFullName(fullName),
                    isLoading = false
                )
            }
            return
        }


        if (validatePhone(phoneNumber) != null){
            _state.update {
                it.copy(
                    errorPhoneNumber = validatePhone(phoneNumber),
                    isLoading = false
                )
            }
            return
        }

        if (organization != null && organization.trim().length > 150){
            _state.update {
                it.copy(
                    errorOrganization = "Количество символов превышает 150",
                    isLoading = false
                )
            }
            return
        }

        if (validatePurpose(purpose) != null){
            _state.update {
                it.copy(
                    errorPurpose = validatePurpose(purpose),
                    isLoading = false
                )
            }
            return
        }

        val te = validateTime(validFrom, validTo, timeNow, date)
        if (te.timeToError != null || te.timeFromError != null || te.dateError != null){
            _state.update {
                it.copy(
                    isLoading = false,
                    errorDate = te.dateError,
                    errorValidFrom = te.timeFromError,
                    errorValidTo = te.timeToError
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    isButtonActive = false,
                    error = null,
                    isSessionExpired = false
                )
            }

            val parseDate = LocalDate.parse(date?.trim(), dateFormat)
            val parseTimeFrom = LocalTime.parse(validFrom?.trim(), timeFormat)
            val parseTimeTo = LocalTime.parse(validTo?.trim(), timeFormat)

            val dateTimeFrom = LocalDateTime.of(parseDate, parseTimeFrom)
            val dateTimeTo = LocalDateTime.of(parseDate, parseTimeTo)

            val code = sessionStorage.current() ?: return@launch

            val body = CreateRequestBody(
                fullName,
                phoneNumber,
                organization,
                purpose,
                dateTimeFrom.toString(),
                dateTimeTo.toString()
            )
            when(val sendForm = requestsApi.create(code, body)){
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            isButtonActive = true
                        )
                    }
                    _event.emit(RequestFormContract.Event.OnCreated)
                }
                is ApiResult.Conflict -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = sendForm.message,
                            isButtonActive = true
                        )
                    }
                }

                is ApiResult.Failure -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = sendForm.message,
                            isButtonActive = true
                        )
                    }
                }
                is ApiResult.FieldErrors -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка в поле",
                            isButtonActive = true
                        )
                    }
                }
                is ApiResult.Network -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Нет связи",
                            isButtonActive = true
                        )
                    }
                }

                ApiResult.NotFound -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка в поле",
                            isButtonActive = true
                        )
                    }
                }
                ApiResult.Unauthorized -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Неизвестный код",
                            isSessionExpired = true,
                            isButtonActive = true
                        )
                    }
                    sessionStorage.clear()
                    _event.emit(RequestFormContract.Event.OnSessionExpired)
                }
            }
        }


    }

    private fun validateTime(timeFrom: String?, timeTo : String?, timeNow :  LocalDateTime, date : String?) : TimeError{

        val parseDate = runCatching { LocalDate.parse(date?.trim(), dateFormat) }.getOrNull()
        val parseTimeFrom = runCatching { LocalTime.parse(timeFrom?.trim(), timeFormat) }.getOrNull()
        val parseTimeTo = runCatching { LocalTime.parse(timeTo?.trim(), timeFormat) }.getOrNull()

        val dateTimeFrom = if(parseDate != null && parseTimeFrom != null)
            LocalDateTime.of(parseDate, parseTimeFrom) else null

        val dateTimeTo = if (parseDate != null && parseTimeTo != null)
            LocalDateTime.of(parseDate, parseTimeTo) else null

        val dateError = when{
            date.isNullOrEmpty() -> "Отстуствует дата"
            parseDate == null -> "Неверная дата"
            else -> null
        }

        val timeFromError = when{
            timeFrom.isNullOrEmpty() -> "Отсутствует время начала"
            parseTimeFrom == null -> "Неверное время начала"
            dateTimeFrom != null && dateTimeFrom.isBefore(timeNow) -> "Время начала не может быть в прошлом"
            else -> null
        }

        val timeToError = when {
            timeTo.isNullOrEmpty() -> "Отсутствует время окончания"
            parseTimeTo == null -> "Неверное время окончания"
            dateTimeTo != null && !dateTimeTo.isAfter(dateTimeFrom) -> "Время окончания должно быть позже времени начала"
            else -> null
        }


        return TimeError(dateError, timeFromError, timeToError)
    }

    private fun validateFullName(fullName : String?) : String? {
        return if (fullName.isNullOrEmpty()){
            "Отсутвует ФИО"
        } else if (fullName.trim().length !in 5..150){
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

        return if ((phoneTrim.startsWith("+7")) && (phoneTrim.length == 12) && (phoneTrim.substring(2).all { it in '0'..'9' })){
            null
        } else {
            "Номер телефона не соответствует формату"
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