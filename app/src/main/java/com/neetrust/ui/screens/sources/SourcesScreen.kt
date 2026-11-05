package com.neetrust.ui.screens.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neetrust.core.logger.AppLogger
import com.neetrust.data.repository.SourceApiException
import com.neetrust.data.repository.SourceRepository
import com.neetrust.domain.model.SourceDto
import com.neetrust.ui.components.LoadingState
import com.neetrust.ui.components.NeetRustTopBar
import com.neetrust.ui.theme.MoriAmberAccent
import com.neetrust.ui.theme.MoriBgCard
import com.neetrust.ui.theme.MoriBgDeep
import com.neetrust.ui.theme.MoriBorder
import com.neetrust.ui.theme.MoriCoralAlert
import com.neetrust.ui.theme.MoriEmeraldDark
import com.neetrust.ui.theme.MoriEmeraldLight
import com.neetrust.ui.theme.MoriEmeraldPrimary
import com.neetrust.ui.theme.MoriTextInverse
import com.neetrust.ui.theme.MoriTextMuted
import com.neetrust.ui.theme.MoriTextPrimary
import com.neetrust.ui.theme.MoriTextSecondary
import kotlinx.coroutines.launch

data class SourcesUiState(
    val isLoading: Boolean = true,
    val sources: List<SourceDto> = emptyList(),
    val error: String? = null,
    val isAdding: Boolean = false,
    val addMessage: String? = null,
    val addIsError: Boolean = false,
    val pendingForceUrl: String? = null
)

class SourcesViewModel(
    private val repository: SourceRepository
) : ViewModel() {
    private val logger = AppLogger.of("SourcesViewModel")

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(SourcesUiState())
    val state: kotlinx.coroutines.flow.StateFlow<SourcesUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.list().fold(
                onSuccess = { _state.value = _state.value.copy(isLoading = false, sources = it, error = null) },
                onFailure = {
                    logger.e("Gagal memuat sumber", it)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Tidak bisa terhubung ke backend: ${it.message}"
                    )
                }
            )
        }
    }

    fun add(url: String, force: Boolean = false) {
        if (url.isBlank() || _state.value.isAdding) return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isAdding = true,
                addMessage = "Menguji situs, ini bisa memakan waktu...",
                addIsError = false,
                pendingForceUrl = null
            )
            repository.add(url, force).fold(
                onSuccess = { result ->
                    val summary = result.probe?.summary.orEmpty()
                    _state.value = _state.value.copy(
                        isAdding = false,
                        addMessage = "Ditambahkan: ${result.source.name}. $summary".trim(),
                        addIsError = false
                    )
                    refresh()
                },
                onFailure = { e ->
                    val rejected = e is SourceApiException && e.code == 422
                    _state.value = _state.value.copy(
                        isAdding = false,
                        addMessage = e.message ?: "Gagal menambahkan sumber",
                        addIsError = true,
                        pendingForceUrl = if (rejected) url else null
                    )
                }
            )
        }
    }

    fun toggle(id: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(id, enabled).onSuccess { refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.delete(id).onSuccess { refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun move(id: String, delta: Int) {
        val ids = _state.value.sources.map { it.id }.toMutableList()
        val from = ids.indexOf(id)
        val to = from + delta
        if (from < 0 || to < 0 || to >= ids.size) return
        ids.removeAt(from)
        ids.add(to, id)
        viewModelScope.launch {
            repository.reorder(ids).fold(
                onSuccess = { _state.value = _state.value.copy(sources = it, error = null) },
                onFailure = { _state.value = _state.value.copy(error = it.message) }
            )
        }
    }

    fun resetHealth(id: String) {
        viewModelScope.launch {
            repository.resetHealth(id).onSuccess { refresh() }
        }
    }
}

@Composable
fun SourcesScreen(
    viewModel: SourcesViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var urlInput by remember { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<SourceDto?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            NeetRustTopBar(
                title = "Sumber Website",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = MoriBgDeep,
        modifier = Modifier.testTag("sources_screen")
    ) { padding ->
        if (state.isLoading) {
            LoadingState(message = "Memuat daftar sumber...", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Urutan menentukan prioritas. Kalau sumber teratas gagal, aplikasi otomatis mencoba sumber berikutnya.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MoriTextSecondary
                    )
                }

                if (state.error != null) {
                    item {
                        Text(
                            text = state.error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MoriCoralAlert
                        )
                    }
                }

                itemsIndexed(state.sources) { index, source ->
                    SourceRow(
                        source = source,
                        isFirst = index == 0,
                        isLast = index == state.sources.lastIndex,
                        onToggle = { viewModel.toggle(source.id, it) },
                        onMoveUp = { viewModel.move(source.id, -1) },
                        onMoveDown = { viewModel.move(source.id, 1) },
                        onDelete = { toDelete = source },
                        onResetHealth = { viewModel.resetHealth(source.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tambah situs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MoriEmeraldLight
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("source_url_input"),
                        placeholder = { Text("https://contoh-situs-anime.com", color = MoriTextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MoriEmeraldPrimary,
                            unfocusedBorderColor = MoriBorder,
                            focusedContainerColor = MoriBgCard,
                            unfocusedContainerColor = MoriBgCard,
                            focusedTextColor = MoriTextPrimary,
                            unfocusedTextColor = MoriTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.add(urlInput) },
                        enabled = urlInput.isNotBlank() && !state.isAdding,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MoriEmeraldPrimary,
                            contentColor = MoriTextInverse
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isAdding) "Menguji..." else "Uji & Tambah")
                    }
                    if (state.addMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.addMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.addIsError) MoriCoralAlert else MoriTextSecondary
                        )
                    }
                    if (state.pendingForceUrl != null) {
                        TextButton(onClick = { viewModel.add(state.pendingForceUrl!!, force = true) }) {
                            Text("Simpan tetap", color = MoriAmberAccent)
                        }
                    }
                }
            }
        }
    }

    val target = toDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Hapus sumber?") },
            text = { Text("${target.name} akan dihapus dari daftar.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target.id)
                    toDelete = null
                }) { Text("Hapus", color = MoriCoralAlert) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun SourceRow(
    source: SourceDto,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onResetHealth: () -> Unit
) {
    val status = when {
        !source.enabled -> "Nonaktif"
        !source.healthy -> "Cooldown ${source.cooldownRemaining} dtk"
        else -> "Aktif"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MoriBgCard)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (source.builtin) "${source.name} (default)" else source.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MoriTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = source.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MoriTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(checked = source.enabled, onCheckedChange = onToggle)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium,
            color = if (source.enabled && source.healthy) MoriEmeraldLight else MoriAmberAccent
        )
        if (!source.healthy && !source.lastError.isNullOrBlank()) {
            Text(
                text = source.lastError!!,
                style = MaterialTheme.typography.bodySmall,
                color = MoriTextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Naikkan prioritas", tint = MoriTextPrimary)
            }
            IconButton(onClick = onMoveDown, enabled = !isLast) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Turunkan prioritas", tint = MoriTextPrimary)
            }
            if (!source.healthy) {
                TextButton(onClick = onResetHealth) { Text("Coba lagi", color = MoriAmberAccent) }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (!source.builtin) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MoriCoralAlert)
                }
            }
        }
    }
}
