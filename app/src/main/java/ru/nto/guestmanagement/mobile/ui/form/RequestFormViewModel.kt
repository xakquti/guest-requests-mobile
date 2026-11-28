package ru.nto.guestmanagement.mobile.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
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
import java.util.Locale

class RequestFormViewModel(
    private val requestsApi: RequestsApi,
    private val sessionStorage: SessionStorage,
    private val timeProvider: TimeProvider
) : ViewModel() {
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.ROOT).withResolverStyle(
        ResolverStyle.STRICT)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withResolverStyle(
        ResolverStyle.STRICT)
    private val apiFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT)
    private val _state = MutableStateFlow(RequestFormContract.State())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<RequestFormContract.Event>()
    val event = _event.asSharedFlow()

    private var job: Job? = null

    fun onIntent(intent : RequestFormContract.Intent){
        when(intent){
            is RequestFormContract.Intent.OnFormSend -> sendForm()
            is RequestFormContract.Intent.EnterFullName -> {
                _state.update {
                    it.copy(
                        fio = intent.fullName,
                        errorFullName = if (it.isAfterFirstSubmit) validateFullName(intent.fullName) else null,
                    )
                }
            }
            is RequestFormContract.Intent.EnterOrganization -> {
                _state.update {
                    it.copy(
                        organization = intent.organization,
                        errorOrganization = if (it.isAfterFirstSubmit) validateOrganization(intent.organization) else null
                    )
                }
            }
            is RequestFormContract.Intent.EnterPhoneNumber -> {
                _state.update {
                    it.copy(
                        phoneNumber = intent.phoneNumber,
                        errorPhoneNumber = if (it.isAfterFirstSubmit) validatePhone(intent.phoneNumber) else null
                    )
                }
            }
            is RequestFormContract.Intent.EnterPurpose -> {
                _state.update {
                    it.copy(
                        purpose = intent.purpose,
                        errorPurpose = if (it.isAfterFirstSubmit) validatePurpose(intent.purpose) else null
                    )
                }
            }
            is RequestFormContract.Intent.EnterValidFrom -> {
                _state.update {
                    it.copy(
                        validFrom = intent.validFrom,
                        errorValidFrom = if(it.isAfterFirstSubmit) validateTime(intent.validFrom, it.validTo, timeProvider.now(), it.date).timeFromError else null,
                        errorValidTo = if(it.isAfterFirstSubmit) validateTime(intent.validFrom, it.validTo, timeProvider.now(), it.date).timeToError else null,
                        errorDate = if(it.isAfterFirstSubmit) validateTime(intent.validFrom, it.validTo, timeProvider.now(), it.date).dateError else null
                    )
                }
            }
            is RequestFormContract.Intent.EnterValidTo -> {
                _state.update {
                    it.copy(
                        validTo = intent.validTo,
                        errorValidTo = if (it.isAfterFirstSubmit) validateTime(it.validFrom,intent.validTo, timeProvider.now(), it.date).timeToError else null,
                        errorValidFrom = if (it.isAfterFirstSubmit) validateTime(it.validFrom,intent.validTo, timeProvider.now(), it.date).timeFromError else null,
                        errorDate = if (it.isAfterFirstSubmit) validateTime(it.validFrom,intent.validTo, timeProvider.now(), it.date).dateError else null
                    )
                }
            }

            is RequestFormContract.Intent.EnterDate -> {
                _state.update {
                    it.copy(
                        date = intent.date,
                        errorDate = if (it.isAfterFirstSubmit) validateTime(it.validFrom, it.validTo, timeProvider.now(), intent.date).dateError else null,
                        errorValidFrom = if (it.isAfterFirstSubmit) validateTime(it.validFrom, it.validTo, timeProvider.now(), intent.date).timeFromError else null,
                        errorValidTo = if (it.isAfterFirstSubmit) validateTime(it.validFrom, it.validTo, timeProvider.now(), intent.date).timeToError else null
                    )
                }
            }
        }
    }

    private fun sendForm(){
        if (_state.value.isLoading || job?.isActive == true){
            return
        }
        _state.update {
            it.copy(
                error = null
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

        val fullNameErr = validateFullName(fullName)
        val phoneErr = validatePhone(phoneNumber)
        val orgErr = validateOrganization(organization)
        val purpErr = validatePurpose(purpose)
        val te = validateTime(validFrom, validTo, timeNow, date)
        _state.update {
            it.copy(
                errorFullName = fullNameErr,
                errorPhoneNumber = phoneErr,
                errorOrganization = orgErr,
                errorPurpose = purpErr,
                errorDate = te.dateError,
                errorValidFrom = te.timeFromError,
                errorValidTo = te.timeToError,
                isAfterFirstSubmit = true
            )
        }
        if (fullNameErr != null || phoneErr != null || orgErr != null || purpErr != null || te.dateError != null || te.timeToError != null || te.timeFromError != null){
            return
        }
        job = viewModelScope.launch {
            val code = sessionStorage.current() ?: return@launch
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

            val body = CreateRequestBody(
                fullName?.trim(),
                phoneNumber?.trim(),
                organization?.trim()?.takeIf { it.isNotEmpty() },
                purpose?.trim(),
                dateTimeFrom.format(apiFormat),
                dateTimeTo.format(apiFormat)
            )
            when(val sendForm = requestsApi.create(code, body)){
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            isButtonActive = true,
                            errorDate = null,
                            errorValidTo = null,
                            errorValidFrom = null,
                            errorPurpose = null,
                            errorOrganization = null,
                            errorFullName = null,
                            errorPhoneNumber = null
                        )
                    }
                    _event.emit(RequestFormContract.Event.OnCreated)
                }
                is ApiResult.Conflict -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isButtonActive = true,
                            error = "На это время уже есть заявка для указанного телефона",
                        )
                    }
                }

                is ApiResult.Failure -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Нет связи",
                            isButtonActive = true
                        )
                    }
                }
                is ApiResult.FieldErrors -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            isButtonActive = true,
                            errorValidFrom = sendForm.errors["validFrom"],
                            errorValidTo = sendForm.errors["validTo"],
                            errorFullName = sendForm.errors["fullName"],
                            errorPhoneNumber = sendForm.errors["phone"],
                            errorOrganization = sendForm.errors["organization"],
                            errorPurpose = sendForm.errors["purpose"],
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
                            error = "Ошибка: не найдено",
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
        var dateError: String? = null
        if (date.isNullOrBlank()){
            dateError = "Отстуствует дата"
        }
        var timeToError: String? = null
        if (timeTo.isNullOrBlank()){
            timeToError = "Отсутствует время окончания"
        }
        var timeFromError: String? = null
        if (timeFrom.isNullOrBlank()){
            timeFromError = "Отсутствует время начала"
        }
        if (dateError != null && timeToError != null && timeFromError != null){
            return TimeError(dateError, timeFromError, timeToError)
        }


        val parseDate = runCatching { LocalDate.parse(date?.trim(), dateFormat) }.getOrNull()
        val parseTimeFrom = runCatching { LocalTime.parse(timeFrom?.trim(), timeFormat) }.getOrNull()
        val parseTimeTo = runCatching { LocalTime.parse(timeTo?.trim(), timeFormat) }.getOrNull()

        val dateTimeFrom = if(parseDate != null && parseTimeFrom != null)
            LocalDateTime.of(parseDate, parseTimeFrom) else null

        val dateTimeTo = if (parseDate != null && parseTimeTo != null)
            LocalDateTime.of(parseDate, parseTimeTo) else null

        when{
            parseDate == null && dateError == null-> dateError = "Неверная дата"
        }

        when{
            parseTimeFrom == null && timeFromError == null-> timeFromError = "Неверное время начала"
            dateTimeFrom != null && dateTimeFrom.isBefore(timeNow) && timeFromError == null-> timeFromError = "Время начала не может быть в прошлом"
        }

       when {
            parseTimeTo == null && timeToError == null-> timeToError = "Неверное время окончания"
            dateTimeFrom != null && dateTimeTo != null && !dateTimeTo.isAfter(dateTimeFrom) && timeToError == null -> timeToError = "Время окончания должно быть позже времени начала"
            dateTimeFrom != null && dateTimeTo != null && dateTimeTo.toLocalDate() != dateTimeFrom.toLocalDate() && timeToError == null -> timeToError = "Окончание должно быть в тот же день"
        }
        return TimeError(dateError, timeFromError, timeToError)
    }

    private fun validateOrganization(organization: String?) : String? {
        return if (organization?.trim() != null && organization.trim().length > 150){
            "Количество символов превышает 150"
        } else {
            null
        }
    }

    private fun validateFullName(fullName : String?) : String? {
        return if (fullName?.trim().isNullOrEmpty()){
            "Отсутвует ФИО"
        } else if (fullName.trim().length !in 5..150){
            "ФИО должно быть в пределах 5-150 символов"
        } else {
            null
        }
    }

    private fun validatePhone(phone : String?) : String? {
        val phoneTrim = phone?.trim()
        if (phoneTrim.isNullOrEmpty()){
            return "Отсутствует номер телефона"
        }

        return if ((phoneTrim.startsWith("+7")) && (phoneTrim.length == 12) && (phoneTrim.substring(2).all { it in '0'..'9' })){
            null
        } else {
            "Номер телефона не соответствует формату"
        }
    }

    private fun validatePurpose(purpose : String?) : String? {
        return if (purpose?.trim().isNullOrEmpty()){
            "Цель отсутствует"
        } else if (purpose.trim().length !in 5..200){
            "Количство символов 5-200"
        } else {
            null
        }
    }

}