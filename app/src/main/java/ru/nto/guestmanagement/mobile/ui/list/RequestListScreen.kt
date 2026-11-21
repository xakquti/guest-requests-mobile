package ru.nto.guestmanagement.mobile.ui.list

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import ru.nto.guestmanagement.mobile.data.RequestStatus
import ru.nto.guestmanagement.mobile.data.RequestsApi
import ru.nto.guestmanagement.mobile.data.formatDate
import ru.nto.guestmanagement.mobile.data.formatInterval
import ru.nto.guestmanagement.mobile.di.AppContainer
import ru.nto.guestmanagement.mobile.ui.TestIds
import ru.nto.guestmanagement.mobile.ui.common.StatusLabel
import ru.nto.guestmanagement.mobile.ui.common.containerViewModel
import ru.nto.guestmanagement.mobile.ui.refresh.RefreshGate
import ru.nto.guestmanagement.mobile.ui.refresh.ResumeEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestListScreen(
    container: AppContainer,
    onOpenDetails: (Long) -> Unit,
    onAddRequest: () -> Unit,
    onLogout: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val viewModel = containerViewModel {
        RequestListViewModel(RequestsApi(container.httpClient), container.session)
    }
    val state by viewModel.state.collectAsState()
    val gate = remember { RefreshGate() }

    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            when (action) {
                is RequestListContract.Action.NavigateToDetails -> onOpenDetails(action.id)
                is RequestListContract.Action.NavigateToLogin -> onLogout()
                is RequestListContract.Action.SessionExpired -> onSessionExpired()
            }
        }
    }

    ResumeEffect {
        gate.launch(viewModel.viewModelScope) {
            viewModel.onIntent(RequestListContract.Intent.LoadRequests)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заявки") },
                actions = {
                    TextButton(
                        onClick = { viewModel.onIntent(RequestListContract.Intent.LoadRequests) },
                        modifier = Modifier.testTag(TestIds.REQUESTS_REFRESH)
                    ) { Text("Обновить") }
                    TextButton(
                        onClick = { viewModel.onIntent(RequestListContract.Intent.Logout) },
                        modifier = Modifier.testTag(TestIds.REQUESTS_LOGOUT)
                    ) { Text("Выйти") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRequest,
                modifier = Modifier.testTag(TestIds.REQUESTS_ADD)
            ) { Text("+") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = state.selectedFilter == null,
                    onClick = { viewModel.onIntent(RequestListContract.Intent.SelectFilter(null)) },
                    label = { Text("Все") },
                    modifier = Modifier.testTag(TestIds.REQUESTS_FILTER_ALL)
                )
                FilterChip(
                    selected = state.selectedFilter == RequestStatus.PENDING,
                    onClick = { viewModel.onIntent(RequestListContract.Intent.SelectFilter(RequestStatus.PENDING)) },
                    label = { Text("На рассмотрении") },
                    modifier = Modifier.testTag(TestIds.REQUESTS_FILTER_PENDING)
                )
                FilterChip(
                    selected = state.selectedFilter == RequestStatus.APPROVED,
                    onClick = { viewModel.onIntent(RequestListContract.Intent.SelectFilter(RequestStatus.APPROVED)) },
                    label = { Text("Одобрены") },
                    modifier = Modifier.testTag(TestIds.REQUESTS_FILTER_APPROVED)
                )
                FilterChip(
                    selected = state.selectedFilter == RequestStatus.REJECTED,
                    onClick = { viewModel.onIntent(RequestListContract.Intent.SelectFilter(RequestStatus.REJECTED)) },
                    label = { Text("Отклонены") },
                    modifier = Modifier.testTag(TestIds.REQUESTS_FILTER_REJECTED)
                )
                FilterChip(
                    selected = state.selectedFilter == RequestStatus.CANCELLED,
                    onClick = { viewModel.onIntent(RequestListContract.Intent.SelectFilter(RequestStatus.CANCELLED)) },
                    label = { Text("Отменены") },
                    modifier = Modifier.testTag(TestIds.REQUESTS_FILTER_CANCELLED)
                )
            }

            if (state.isOffline) {
                Text(
                    "Нет связи",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(8.dp)
                        .testTag(TestIds.REQUESTS_OFFLINE)
                )
            }

            when {
                state.isInitialLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    Column(Modifier.padding(16.dp)) {
                        Text(state.error!!, modifier = Modifier.testTag(TestIds.REQUESTS_ERROR))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.onIntent(RequestListContract.Intent.LoadRequests) }) {
                            Text("Повторить")
                        }
                    }
                }
                state.requests.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Заявок нет", modifier = Modifier.testTag(TestIds.REQUESTS_EMPTY))
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        itemsIndexed(state.requests) { index, request ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag(TestIds.requestsPos(index))
                                    .pointerInput(Unit) {
                                        detectTapGestures {
                                            viewModel.onIntent(
                                                RequestListContract.Intent.OpenDetails(request.id)
                                            )
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = request.fullName,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.testTag(TestIds.REQUEST_NAME)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatDate(request.validFrom),
                                        modifier = Modifier.testTag(TestIds.REQUEST_DATE)
                                    )
                                    Text(
                                        text = formatInterval(request.validFrom, request.validTo),
                                        modifier = Modifier.testTag(TestIds.REQUEST_TIME)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    StatusLabel(
                                        status = request.status,
                                        tag = TestIds.REQUEST_STATUS
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}