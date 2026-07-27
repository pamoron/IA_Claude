package com.pamoron.electroperico.ui.comparator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pamoron.electroperico.ElectroPericoApplication
import com.pamoron.electroperico.data.comparator.ComparatorRepository
import com.pamoron.electroperico.data.settings.SettingsRepository
import com.pamoron.electroperico.domain.calc.ChargerComparator
import com.pamoron.electroperico.domain.model.ChargerOption
import com.pamoron.electroperico.domain.model.Comparison
import com.pamoron.electroperico.domain.model.ComparisonSort
import com.pamoron.electroperico.domain.model.CurrentType
import com.pamoron.electroperico.ui.format.Formatters
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Formulario de alta o edición de una opción del comparador. */
data class OptionForm(
    val id: String? = null,
    val name: String = "",
    val priceText: String = "",
    val powerText: String = "",
    val currentType: CurrentType = CurrentType.DC,
    val startSoc: Int = 20,
    val targetSoc: Int = 80,
    val startFeeText: String = "",
    val pricePerMinuteText: String = "",
    val parkingFeeText: String = "",
) {

    /** `true` si se está editando una opción existente. */
    val isEditing: Boolean get() = id != null

    /** Convierte el formulario en una opción, o `null` si falta algo. */
    fun toOption(): ChargerOption? {
        val price = Formatters.parseDecimal(priceText) ?: return null
        val power = Formatters.parseDecimal(powerText) ?: return null
        val startFee = Formatters.parseOptionalDecimal(startFeeText) ?: return null
        val perMinute = Formatters.parseOptionalDecimal(pricePerMinuteText) ?: return null
        val parking = Formatters.parseOptionalDecimal(parkingFeeText) ?: return null
        if (price < 0.0 || power <= 0.0 || targetSoc <= startSoc) return null
        return ChargerOption(
            id = id ?: UUID.randomUUID().toString(),
            name = name.trim().ifBlank { DEFAULT_NAME },
            pricePerKWh = price,
            chargerPowerKw = power,
            currentType = currentType,
            startSocPercent = startSoc,
            targetSocPercent = targetSoc,
            startFeeEur = startFee,
            pricePerMinuteEur = perMinute,
            parkingFeeEur = parking,
        )
    }

    companion object {
        const val DEFAULT_NAME: String = "Cargador"

        fun from(option: ChargerOption): OptionForm = OptionForm(
            id = option.id,
            name = option.name,
            priceText = Formatters.toEditableText(option.pricePerKWh),
            powerText = Formatters.toEditableText(option.chargerPowerKw),
            currentType = option.currentType,
            startSoc = option.startSocPercent,
            targetSoc = option.targetSocPercent,
            startFeeText = Formatters.toEditableText(option.startFeeEur),
            pricePerMinuteText = Formatters.toEditableText(option.pricePerMinuteEur),
            parkingFeeText = Formatters.toEditableText(option.parkingFeeEur),
        )
    }
}

/** Estado de la pantalla del comparador. */
data class ComparatorUiState(
    val comparison: Comparison? = null,
    val optionCount: Int = 0,
    val sort: ComparisonSort = ComparisonSort.DEFAULT,
    val editing: OptionForm? = null,
    val vehicleName: String = "",
) {
    /** `true` si todavía caben más opciones. */
    val canAdd: Boolean get() = optionCount < ChargerOption.MAX_OPTIONS

    /** `true` si no hay nada guardado todavía. */
    val isEmpty: Boolean get() = optionCount == 0
}

/**
 * Lógica de presentación del comparador.
 *
 * Recalcula la comparación completa cada vez que cambian las opciones, los
 * ajustes o el criterio de ordenación, de modo que editar el perfil del
 * vehículo se refleja aquí al instante.
 */
class ComparatorViewModel(
    private val repository: ComparatorRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _editing = MutableStateFlow<OptionForm?>(null)

    val uiState: StateFlow<ComparatorUiState> = combine(
        repository.options,
        repository.sort,
        settingsRepository.settings,
        _editing,
    ) { options, sort, settings, editing ->
        ComparatorUiState(
            comparison = ChargerComparator.compare(
                options = options,
                vehicle = settings.vehicle,
                sort = sort,
                estimationMode = settings.estimationMode,
                priceThresholds = settings.priceThresholds,
            ),
            optionCount = options.size,
            sort = sort,
            editing = editing,
            vehicleName = settings.vehicle.displayName,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ComparatorUiState(),
    )

    // --- Alta y edición -----------------------------------------------------

    /** Abre el formulario en blanco. */
    fun onAddClick() {
        _editing.value = OptionForm()
    }

    /** Abre el formulario con los datos de una opción existente. */
    fun onEditClick(option: ChargerOption) {
        _editing.value = OptionForm.from(option)
    }

    /** Duplica una opción con un nombre nuevo. */
    fun onDuplicate(option: ChargerOption) {
        viewModelScope.launch {
            repository.upsert(
                option.copy(
                    id = UUID.randomUUID().toString(),
                    name = "${option.name} (copia)",
                ),
            )
        }
    }

    fun onFormChange(update: (OptionForm) -> OptionForm) {
        _editing.update { current -> current?.let(update) }
    }

    /** Ajusta los porcentajes manteniendo el objetivo por encima del inicial. */
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

    /** Guarda el formulario abierto y lo cierra. */
    fun onSaveForm() {
        val option = _editing.value?.toOption() ?: return
        viewModelScope.launch {
            repository.upsert(option)
            _editing.value = null
        }
    }

    fun onCancelForm() {
        _editing.value = null
    }

    // --- Lista ---------------------------------------------------------------

    fun onDelete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun onClearAll() {
        viewModelScope.launch { repository.clear() }
    }

    fun onSortChange(sort: ComparisonSort) {
        viewModelScope.launch { repository.setSort(sort) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                ) as ElectroPericoApplication
                ComparatorViewModel(
                    repository = application.container.comparatorRepository,
                    settingsRepository = application.container.settingsRepository,
                )
            }
        }
    }
}
