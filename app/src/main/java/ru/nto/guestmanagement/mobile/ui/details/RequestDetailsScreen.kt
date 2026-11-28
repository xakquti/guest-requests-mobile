package ru.nto.guestmanagement.mobile.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.nto.guestmanagement.mobile.data.ApiResult
import ru.nto.guestmanagement.mobile.data.RequestDetails
import ru.nto.guestmanagement.mobile.data.RequestStatus
import ru.nto.guestmanagement.mobile.data.RequestsApi
import ru.nto.guestmanagement.mobile.data.SessionStorage
import ru.nto.guestmanagement.mobile.data.formatDateTime
import ru.nto.guestmanagement.mobile.di.AppContainer
import ru.nto.guestmanagement.mobile.ui.TestIds
import ru.nto.guestmanagement.mobile.ui.common.StatusLabel
import ru.nto.guestmanagement.mobile.ui.common.containerViewModel
import ru.nto.guestmanagement.mobile.ui.refresh.RefreshGate
import ru.nto.guestmanagement.mobile.ui.refresh.ResumeEffect

sealed interface RequestDetailsContract {
    data class State(
        val detail: RequestDetails? = null,
        val isLoading: Boolean = false,
        val isInitialLoading: Boolean = true,
        val isOffline: Boolean = false,
        val error: String? = null,
        val cancelError: String? = null,
        val showCancelConfirm: Boolean = false
    )

    sealed interface Intent {
        data object LoadDetail : Intent
        data object CancelRequest : Intent
        data object ConfirmCancel : Intent
        data object DismissCancel : Intent
        data object Refresh : Intent
    }

    sealed interface Action {
        data object NavigateBack : Action
        data object SessionExpired : Action
    }
}

class RequestDetailsViewModel(
    private val api: RequestsApi,
    private val sessionStorage: SessionStorage,
    private val requestId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(RequestDetailsContract.State())
    val state = _state.asStateFlow()
    private val _action = MutableSharedFlow<RequestDetailsContract.Action>()
    val action = _action.asSharedFlow()

    fun onIntent(intent: RequestDetailsContract.Intent) {
        when (intent) {
            is RequestDetailsContract.Intent.LoadDetail -> loadDetail()
            is RequestDetailsContract.Intent.Refresh -> loadDetail()
            is RequestDetailsContract.Intent.CancelRequest ->
                _state.update { it.copy(showCancelConfirm = true, cancelError = null) }

            is RequestDetailsContract.Intent.ConfirmCancel -> confirmCancel()
            is RequestDetailsContract.Intent.DismissCancel ->
                _state.update { it.copy(showCancelConfirm = false, cancelError = null) }
        }
    }

    private fun loadDetail() {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, isOffline = false) }
            val code = sessionStorage.current() ?: run {
                _action.emit(RequestDetailsContract.Action.SessionExpired); return@launch
            }
            when (val r = api.get(code, requestId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(detail = r.value, isLoading = false, isInitialLoading = false)
                }

                is ApiResult.Network ->
                    if (_state.value.detail == null) _state.update {
                        it.copy(
                            isLoading = false,
                            isInitialLoading = false,
                            error = "Ошибка загрузки"
                        )
                    } else _state.update { it.copy(isLoading = false, isOffline = true) }

                ApiResult.Unauthorized -> {
                    sessionStorage.clear()
                    _action.emit(RequestDetailsContract.Action.SessionExpired)
                }

                else -> _state.update {
                    it.copy(isLoading = false, isInitialLoading = false, error = "Ошибка загрузки")
                }
            }
        }
    }

    private fun confirmCancel() {
        viewModelScope.launch {
            val code = sessionStorage.current() ?: run {
                _action.emit(RequestDetailsContract.Action.SessionExpired); return@launch
            }
            when (val r = api.cancel(code, requestId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(detail = r.value, showCancelConfirm = false, cancelError = null)
                }

                is ApiResult.Conflict -> {
                    _state.update {
                        it.copy(
                            showCancelConfirm = false,
                            cancelError = "Заявка уже рассмотрена"
                        )
                    }
                    loadDetail()
                }

                is ApiResult.Network -> _state.update {
                    it.copy(showCancelConfirm = false, cancelError = "Ошибка сети")
                }

                ApiResult.Unauthorized -> {
                    sessionStorage.clear()
                    _action.emit(RequestDetailsContract.Action.SessionExpired)
                }

                else -> _state.update {
                    it.copy(
                        showCancelConfirm = false,
                        cancelError = "Ошибка отмены"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailsScreen(
    container: AppContainer,
    requestId: Long,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val viewModel = containerViewModel {
        RequestDetailsViewModel(RequestsApi(container.httpClient), container.session, requestId)
    }
    val state by viewModel.state.collectAsState()
    val detail = state.detail
    val gate = remember { RefreshGate() }

    DisposableEffect(Unit) {
        onDispose { gate.cancel() }
    }

    LaunchedEffect(Unit) {
        viewModel.action.collect { a ->
            when (a) {
                is RequestDetailsContract.Action.NavigateBack -> onBack()
                is RequestDetailsContract.Action.SessionExpired -> onSessionExpired()
            }
        }
    }

    ResumeEffect {
        gate.launch(viewModel.viewModelScope) {
            viewModel.onIntent(RequestDetailsContract.Intent.LoadDetail)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали заявки") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag(TestIds.DETAILS_BACK)
                    ) {
                        Text("←")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.onIntent(RequestDetailsContract.Intent.Refresh) },
                        modifier = Modifier.testTag(TestIds.DETAILS_REFRESH)
                    ) { Text("Обновить") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(16.dp)) {
            when {
                state.isInitialLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                state.error != null -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(state.error!!, modifier = Modifier.testTag(TestIds.DETAILS_ERROR))
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.onIntent(RequestDetailsContract.Intent.Refresh) },
                        modifier = Modifier.testTag(TestIds.DETAILS_RETRY)
                    ) { Text("Повторить") }
                }

                detail != null -> {
                    Row {
                        Text("ID: ")
                        Text("${detail.id}", modifier = Modifier.testTag(TestIds.DETAILS_ID))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(detail.fullName, modifier = Modifier.testTag(TestIds.DETAILS_NAME))
                    Spacer(Modifier.height(8.dp))
                    Text(detail.phone, modifier = Modifier.testTag(TestIds.DETAILS_PHONE))
                    if (!detail.organization.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            detail.organization,
                            modifier = Modifier.testTag(TestIds.DETAILS_ORGANIZATION)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(detail.purpose, modifier = Modifier.testTag(TestIds.DETAILS_PURPOSE))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        formatDateTime(detail.validFrom),
                        modifier = Modifier.testTag(TestIds.DETAILS_VALID_FROM)
                    )
                    Text(
                        formatDateTime(detail.validTo),
                        modifier = Modifier.testTag(TestIds.DETAILS_VALID_TO)
                    )
                    Spacer(Modifier.height(8.dp))
                    StatusLabel(status = detail.status, tag = TestIds.DETAILS_STATUS)
                    Spacer(Modifier.height(8.dp))

                    Row {
                        Text("Создана: ")
                        Text(
                            formatDateTime(detail.createdAt),
                            modifier = Modifier.testTag(TestIds.DETAILS_CREATED_AT)
                        )
                    }

                    Row {
                        Text("Изменена: ")
                        Text(
                            formatDateTime(detail.updatedAt),
                            modifier = Modifier.testTag(TestIds.DETAILS_UPDATED_AT)
                        )
                    }

                    if (!detail.comment.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Комментарий охраны:", style = MaterialTheme.typography.labelMedium)
                        Text(detail.comment, modifier = Modifier.testTag(TestIds.DETAILS_COMMENT))
                    }

                    if (state.cancelError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.cancelError!!, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(TestIds.DETAILS_CANCEL_ERROR)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    if (detail.status == RequestStatus.PENDING && !state.showCancelConfirm) {
                        Button(
                            onClick = { viewModel.onIntent(RequestDetailsContract.Intent.CancelRequest) },
                            modifier = Modifier.testTag(TestIds.DETAILS_CANCEL)
                        ) { Text("Отменить заявку") }
                    }

                    if (state.showCancelConfirm) {
                        AlertDialog(
                            onDismissRequest = { viewModel.onIntent(RequestDetailsContract.Intent.DismissCancel) },
                            title = { Text("Подтверждение") },
                            text = { Text("Вы уверены, что хотите отменить заявку?") },
                            confirmButton = {
                                Button(
                                    onClick = { viewModel.onIntent(RequestDetailsContract.Intent.ConfirmCancel) },
                                    modifier = Modifier.testTag(TestIds.DETAILS_CANCEL_CONFIRM)
                                ) { Text("Отменить заявку") }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { viewModel.onIntent(RequestDetailsContract.Intent.DismissCancel) },
                                    modifier = Modifier.testTag(TestIds.DETAILS_CANCEL_DISMISS)
                                ) { Text("Назад") }
                            }
                        )
                    }
                }
            }
            if (state.isOffline) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Нет связи", color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag(TestIds.DETAILS_OFFLINE)
                )
            }
        }
    }
}