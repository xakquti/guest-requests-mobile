package ru.nto.guestmanagement.mobile.ui.refresh

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class RefreshGate {
    private var currentJob: Job? = null

    fun launch(scope: CoroutineScope, block: suspend () -> Unit): Job? {
        if (currentJob?.isActive == true) return currentJob
        currentJob = scope.launch {
            try {
                block()
            } finally {
                currentJob = null
            }
        }
        return null
    }

    fun cancel() {
        currentJob?.cancel()
        currentJob = null
    }
}