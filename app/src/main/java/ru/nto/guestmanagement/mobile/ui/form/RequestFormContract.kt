package ru.nto.guestmanagement.mobile.ui.form

sealed interface RequestFormContract {
    data class State(
        val isLoading : Boolean = false,
        val isButtonActive : Boolean = true,
        //errors
        val error : String? = null,
        val errorFullName : String? = null,
        val errorPurpose : String? = null,
        val errorOrganization : String? = null,
        val errorValidFrom : String? = null,
        val errorValidTo : String? = null,
        val errorPhoneNumber : String? = null,
        val errorDate : String? = null,
        //fields
        val fio : String? = null,
        val phoneNumber : String? = null,
        val organization : String? = null,
        val purpose : String? = null,
        val date : String? = null,
        val validFrom : String? = null,
        val validTo : String? = null,
        val isSessionExpired : Boolean = false
    )

    sealed interface Intent {
        data object OnFormSend : Intent
        data class EnterFullName(val fullName : String) : Intent
        data class EnterPhoneNumber(val phoneNumber : String) : Intent
        data class EnterOrganization(val organization : String) : Intent
        data class EnterPurpose(val purpose : String) : Intent
        data class EnterDate(val date : String) : Intent
        data class EnterValidFrom(val validFrom : String) : Intent
        data class EnterValidTo(val validTo : String) : Intent
    }

    sealed interface Event{
        data object OnBack : Event
        data object OnSessionExpired : Event
        data object OnCreated : Event
    }
}