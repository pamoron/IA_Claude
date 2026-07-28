package com.pamoron.electroperico.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pamoron.electroperico.ElectroPericoApplication
import com.pamoron.electroperico.data.history.HistoryRepository
import com.pamoron.electroperico.data.settings.SettingsRepository
import com.pamoron.electroperico.domain.calc.ChargeCalculator
import com.pamoron.electroperico.domain.model.CalculationOutcome
import com.pamoron.electroperico.domain.model.HistoryEntry
import com.pamoron.electroperico.domain.model.HistorySort
import com.pamoron.electroperico.domain.model.orderedBy
import com.pamoron.electroperico.ui.comparator.OptionForm
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla del historial. */
data class HistoryUiState(
    val entries: List<HistoryEntry> = emptyList(),
    val sort: HistorySort = HistorySort.DEFAULT,
    val editing: OptionForm? = null,
) {
    val isEmpty: Boolean get() = entries.isEmpty()

    /** Número de cargadores marcados como favoritos. */
    val favoriteCount: Int get() = entries.count { it.favorite }
}

/**
 * Lógica de presentación del historial.
 *
 * Al editar una entrada se **recalcula** con el perfil de vehículo actual, pero
 * se conserva la fecha original: sigue siendo la misma recarga, corregida.
 */
class HistoryViewModel(
    private val repository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _editing = MutableStateFlow<OptionForm?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.entries,
        repository.sort,
        _editing,
    ) { entries, sort, editing ->
        HistoryUiState(
            entries = entries.orderedBy(sort),
            sort = sort,
            editing = editing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HistoryUiState(),
    )

    // --- Acciones sobre una entrada -----------------------------------------

    fun onEdit(entry: HistoryEntry) {
        _editing.value = OptionForm.from(entry.charger)
    }

    fun onFormChange(update: (OptionForm) -> OptionForm) {
        _editing.update { current -> current?.let(update) }
    }

    fun onStartSocChange(value: Int) = _editing.update { form ->
        form?.let {
            val start = value.coerceIn(0, 99)
            it.copy(startSoc = start, targetSoc = maxOf(it.targetSoc, start + 1))
        }
    }

    fun onTargetSocChange(value: Int) = _editing.update { form ->
        form?.let {
            val target = value.coerceIn(1, 100)
            it.copy(startSoc = minOf(it.startSoc, target - 1), targetSoc = target)
        }
    }

    /** Guarda la entrada editada, recalculada y con su fecha original. */
    fun onSaveForm() {
        val option = _editing.value?.toOption() ?: return
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val outcome = ChargeCalculator.calculate(
                input = option.toChargeInput(),
                vehicle = settings.vehicle,
                estimationMode = settings.estimationMode,
                priceThresholds = settings.priceThresholds,
            )
            if (outcome is CalculationOutcome.Success) {
                val anterior = repository.entries.first().firstOrNull { it.id == option.id }
                repository.upsert(
                    HistoryEntry.from(
                        id = option.id,
                        charger = option,
                        result = outcome.result,
                        timestampMillis = anterior?.timestampMillis ?: System.currentTimeMillis(),
                        favorite = anterior?.favorite ?: false,
                    ),
                )
            }
            _editing.value = null
        }
    }

    fun onCancelForm() {
        _editing.value = null
    }

    /** Duplica una recarga como si se acabara de hacer otra vez en el mismo sitio. */
    fun onDuplicate(entry: HistoryEntry) {
        viewModelScope.launch {
            val nuevoId = UUID.randomUUID().toString()
            repository.upsert(
                entry.copy(
                    id = nuevoId,
                    timestampMillis = System.currentTimeMillis(),
                    charger = entry.charger.copy(id = nuevoId),
                    favorite = false,
                ),
            )
        }
    }

    fun onToggleFavorite(id: String) {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }

    fun onDelete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun onClearAll() {
        viewModelScope.launch { repository.clear() }
    }

    fun onSortChange(sort: HistorySort) {
        viewModelScope.launch { repository.setSort(sort) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                ) as ElectroPericoApplication
                HistoryViewModel(
                    repository = application.container.historyRepository,
                    settingsRepository = application.container.settingsRepository,
                )
            }
        }
    }
}
