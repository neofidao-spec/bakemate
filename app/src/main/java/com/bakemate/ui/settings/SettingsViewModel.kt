package com.bakemate.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bakemate.BuildConfig
import com.bakemate.data.backup.BackupManager
import com.bakemate.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val appVersion: String = "1.0.0",
    val isBackingUp: Boolean = false,
    val backupMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val backupManager: BackupManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(appVersion = BuildConfig.VERSION_NAME))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferences.darkMode.collect { dark ->
                _uiState.update { it.copy(darkMode = dark) }
            }
        }
        viewModelScope.launch {
            appPreferences.notificationsEnabled.collect { notif ->
                _uiState.update { it.copy(notificationsEnabled = notif) }
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setDarkMode(enabled)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setNotificationsEnabled(enabled)
        }
    }

    /** Export data → file cache → share intent (FileProvider). */
    fun exportBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, backupMessage = null) }
            runCatching {
                val file = backupManager.exportToFile()
                val uri = FileProvider.getUriForFile(
                    context,
                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Simpan Backup BakeMate"))
            }.onSuccess {
                _uiState.update { it.copy(isBackingUp = false, backupMessage = "Backup dibuat") }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isBackingUp = false, backupMessage = "Gagal backup: ${e.message}")
                }
            }
        }
    }

    /** Import data dari URI (hasil GetContent). */
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, backupMessage = null) }
            val result = backupManager.importFromUri(uri)
            result.onSuccess { count ->
                _uiState.update {
                    it.copy(isBackingUp = false, backupMessage = "Import berhasil: $count resep")
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isBackingUp = false, backupMessage = "Gagal import: ${e.message}")
                }
            }
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(backupMessage = null) }
}
