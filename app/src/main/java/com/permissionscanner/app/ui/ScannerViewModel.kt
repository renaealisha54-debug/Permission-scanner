package com.permissionscanner.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.permissionscanner.app.scanner.Finding
import com.permissionscanner.app.scanner.scanPath
import com.permissionscanner.app.scanner.scanUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Done(val findings: List<Finding>) : ScanState()
    data class Error(val message: String) : ScanState()
}

class ScannerViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state

    fun scanFromUri(uri: Uri) {
        _state.value = ScanState.Scanning
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { scanUri(getApplication(), uri) }
                .onSuccess { _state.value = ScanState.Done(it) }
                .onFailure { _state.value = ScanState.Error(it.message ?: "Unknown error") }
        }
    }

    fun scanFromPath(path: String) {
        _state.value = ScanState.Scanning
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { scanPath(path.trim()) }
                .onSuccess { _state.value = ScanState.Done(it) }
                .onFailure { _state.value = ScanState.Error(it.message ?: "Unknown error") }
        }
    }

    fun reset() { _state.value = ScanState.Idle }
}
