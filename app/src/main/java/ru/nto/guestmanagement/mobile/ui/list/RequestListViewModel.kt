package ru.nto.guestmanagement.mobile.ui.list

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

class RequestListViewModel(
    private val api: RequestsApi,
    private val sessionStorage: SessionStorage
) : ViewModel() {
    private val _state = MutableStateFlow(RequestListContract.State())
    val state = _state.asStateFlow()

    private val _action = MutableSharedFlow<RequestListContract.Action>()
    val action = _action.asSharedFlow()

    fun onIntent(intent: RequestListContract.Intent) {
        when (intent) {
            is RequestListContract.Intent.LoadRequests -> loadRequests()
            is RequestListContract.Intent.SelectFilter -> {
                _state.update { it.copy(selectedFilter = intent.status) }
                loadRequests()
            }

            is RequestListContract.Intent.OpenDetails -> {
                viewModelScope.launch {
                    _action.emit(
                        RequestListContract.Action.NavigateToDetails(
                            intent.id
                        )
                    )
                }
            }

            is RequestListContract.Intent.Logout -> {
                viewModelScope.launch {
                    sessionStorage.clear()
                    _action.emit(RequestListContract.Action.NavigateToLogin)
                }
            }
        }
    }

    private fun loadRequests() {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, isOffline = false) }

            val code = sessionStorage.current()
            if (code == null) {
                _action.emit(RequestListContract.Action.NavigateToLogin)
                return@launch
            }

            when (val result = api.list(code, _state.value.selectedFilter)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            requests = result.value,
                            isLoading = false,
                            isInitialLoading = false
                        )
                    }
                }

                is ApiResult.Network -> {
                    if (_state.value.requests.isEmpty()) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isInitialLoading = false,
                                error = "Нет связи"
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false, isOffline = true) }
                    }
                }

                ApiResult.Unauthorized -> {
                    sessionStorage.clear()
                    _action.emit(RequestListContract.Action.SessionExpired)
                }

                else -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isInitialLoading = false,
                            error = "Ошибка загрузки"
                        )
                    }
                }
            }
        }
    }
}