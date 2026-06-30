package com.azizjonkasimov.lifesimulator.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class UpdatePromptState internal constructor(
    private val manager: UpdateManager,
    private val scope: CoroutineScope,
    private val onMessage: (String) -> Unit,
) {
    val currentVersionLabel: String = manager.currentVersionLabel()

    var info by mutableStateOf<UpdateInfo?>(null)
        private set

    var progress by mutableIntStateOf(-1)
        private set

    var checking by mutableStateOf(false)
        private set

    fun checkForUpdates(showResult: Boolean = false) {
        if (checking || progress >= 0) return
        scope.launch {
            checking = true
            try {
                when (val result = manager.checkLatest()) {
                    is UpdateCheckResult.Available -> info = result.info
                    UpdateCheckResult.UpToDate -> {
                        if (showResult) onMessage("App is up to date")
                    }
                    UpdateCheckResult.Unavailable -> {
                        if (showResult) onMessage("Could not check for updates")
                    }
                }
            } catch (e: Exception) {
                if (showResult) onMessage("Could not check for updates")
            } finally {
                checking = false
            }
        }
    }

    fun dismiss() {
        if (progress < 0) info = null
    }

    fun installUpdate() {
        val update = info ?: return
        if (progress >= 0) return
        scope.launch {
            progress = 0
            val file = runCatching {
                manager.downloadApk(update) { progress = it }
            }
            progress = -1
            file.onSuccess { manager.installApk(it) }
                .onFailure { onMessage("Update download failed") }
        }
    }
}

@Composable
fun rememberUpdatePromptState(
    onMessage: (String) -> Unit = {},
): UpdatePromptState {
    val context = LocalContext.current
    val manager = remember { UpdateManager(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val latestOnMessage = rememberUpdatedState(onMessage)
    return remember(manager, scope) {
        UpdatePromptState(
            manager = manager,
            scope = scope,
            onMessage = { latestOnMessage.value(it) },
        )
    }
}

@Composable
fun UpdatePrompt(
    state: UpdatePromptState,
) {
    LaunchedEffect(state) {
        state.checkForUpdates()
    }

    val update = state.info ?: return

    AlertDialog(
        onDismissRequest = { state.dismiss() },
        title = { Text(text = "Update available") },
        text = {
            Column {
                Text(text = "Version ${update.versionName} is ready to install.")
                if (update.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = update.notes,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (state.progress >= 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Downloading... ${state.progress}%",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = state.progress < 0,
                onClick = { state.installUpdate() },
            ) {
                Text(text = "Update")
            }
        },
        dismissButton = {
            if (state.progress < 0) {
                TextButton(onClick = { state.dismiss() }) {
                    Text(text = "Later")
                }
            }
        },
    )
}
