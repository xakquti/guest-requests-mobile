package ru.nto.guestmanagement.mobile.ui.list

import ru.nto.guestmanagement.mobile.data.RequestStatus
import ru.nto.guestmanagement.mobile.data.RequestSummary

sealed interface RequestListContract {
    data class State(
        val requests: List<RequestSummary> = emptyList(),
        val selectedFilter: RequestStatus? = null,
        val isLoading: Boolean = false,
        val isInitialLoading: Boolean = true,
        val isOffline: Boolean = false,
        val error: String? = null
    )

    sealed interface Intent {
        data object LoadRequests : Intent
        data class SelectFilter(val status: RequestStatus?) : Intent
        data object Logout : Intent
        data class OpenDetails(val id: Long) : Intent
    }

    sealed interface Action {
        data class NavigateToDetails(val id: Long) : Action
        data object NavigateToLogin : Action
        data object SessionExpired : Action
    }
}