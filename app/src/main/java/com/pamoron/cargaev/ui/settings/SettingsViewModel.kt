package com.pamoron.cargaev.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pamoron.cargaev.CargaEvApplication
import com.pamoron.cargaev.data.settings.SettingsRepository
import com.pamoron.cargaev.domain.model.EstimationMode
import com.pamoron.cargaev.domain.model.PriceThresholds
import com.pamoron.cargaev.domain.model.VehicleProfile
import com.pamoron.cargaev.ui.format.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Campos editables del perfil del vehículo, como texto.
 *
 * Se editan en local y solo se escriben en DataStore al pulsar "Guardar", para
 * que un campo vacío a medio escribir no invalide los cálculos de la pantalla
 * principal.
 */
data class VehicleForm(
    val brand: String = "",
    val model: String = "",
    val grossCapacity: String = "",
    val usableCapacity: String = "",
    val maxDcPower: String = "",
    val maxAcPower: String = "",
    val consumption: String = "",
    val acLoss: String = "",
    val dcLoss: String = "",
) {

    /** Convierte el formulario en un perfil, o `null` si algún campo no es válido. */
    fun toProfile(): VehicleProfile? {
        val gross = Formatters.parseDecimal(grossCapacity) ?: return null
        val usable = Formatters.parseDecimal(usableCapacity)?.takeIf { it > 0.0 } ?: return null
        val dc = Formatters.parseDecimal(maxDcPower)?.takeIf { it > 0.0 } ?: return null
        val ac = Formatters.parseDecimal(maxAcPower)?.takeIf { it > 0.0 } ?: return null
        val avg = Formatters.parseDecimal(consumption)?.takeIf { it > 0.0 } ?: return null
        val lossAc = Formatters.parseDecimal(acLoss)?.takeIf { it >= 0.0 && it < 100.0 } ?: return null
        val lossDc = Formatters.parseDecimal(dcLoss)?.takeIf { it >= 0.0 && it < 100.0 } ?: return null
        if (brand.isBlank() && model.isBlank()) return null
        return VehicleProfile(
            id = VehicleProfile.DEFAULT_ID,
            brand = brand.trim(),
            model = model.trim(),
            grossCapacityKWh = gross,
            usableCapacityKWh = usable,
            maxDcPowerKw = dc,
            maxAcPowerKw = ac,
            consumptionKWhPer100Km = avg,
            acLossPercent = lossAc,
            dcLossPercent = lossDc,
        )
    }

    companion object {
        fun from(profile: VehicleProfile): VehicleForm = VehicleForm(
            brand = profile.brand,
            model = profile.model,
            grossCapacity = Formatters.toEditableText(profile.grossCapacityKWh),
            usableCapacity = Formatters.toEditableText(profile.usableCapacityKWh),
            maxDcPower = Formatters.toEditableText(profile.maxDcPowerKw),
            maxAcPower = Formatters.toEditableText(profile.maxAcPowerKw),
            consumption = Formatters.toEditableText(profile.consumptionKWhPer100Km),
            acLoss = Formatters.toEditableText(profile.acLossPercent),
            dcLoss = Formatters.toEditableText(profile.dcLossPercent),
        )
    }
}

/** Umbrales de precio en edición. */
data class ThresholdsForm(
    val veryCheapMax: String = "",
    val goodMax: String = "",
    val normalMax: String = "",
    val expensiveMax: String = "",
) {

    /** Convierte el formulario en unos umbrales válidos, o `null`. */
    fun toThresholds(): PriceThresholds? {
        val a = Formatters.parseDecimal(veryCheapMax) ?: return null
        val b = Formatters.parseDecimal(goodMax) ?: return null
        val c = Formatters.parseDecimal(normalMax) ?: return null
        val d = Formatters.parseDecimal(expensiveMax) ?: return null
        return PriceThresholds(a, b, c, d).takeIf { it.isValid() }
    }

    companion object {
        fun from(thresholds: PriceThresholds): ThresholdsForm = ThresholdsForm(
            veryCheapMax = Formatters.toEditableText(thresholds.veryCheapMax),
            goodMax = Formatters.toEditableText(thresholds.goodMax),
            normalMax = Formatters.toEditableText(thresholds.normalMax),
            expensiveMax = Formatters.toEditableText(thresholds.expensiveMax),
        )
    }
}

/** Estado de la pantalla de ajustes. */
data class SettingsUiState(
    val vehicleForm: VehicleForm = VehicleForm(),
    val thresholdsForm: ThresholdsForm = ThresholdsForm(),
    val estimationMode: EstimationMode = EstimationMode.DEFAULT,
    val loaded: Boolean = false,
    val savedMessage: Boolean = false,
) {
    val canSaveVehicle: Boolean get() = vehicleForm.toProfile() != null
    val canSaveThresholds: Boolean get() = thresholdsForm.toThresholds() != null
}

/** Lógica de presentación de la pantalla de ajustes. */
class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {

    private val _forms = MutableStateFlow(SettingsUiState())
    private var loaded = false

    val uiState: StateFlow<SettingsUiState> = combine(
        _forms,
        repository.settings.map { it.estimationMode },
    ) { forms, mode ->
        forms.copy(estimationMode = mode)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SettingsUiState(),
    )

    /** Carga los ajustes guardados en los formularios. La pantalla la llama al entrar. */
    fun load() {
        if (loaded) return
        loaded = true
        viewModelScope.launch { refreshForms() }
    }

    private suspend fun refreshForms() {
        val settings = repository.settings.first()
        _forms.value = SettingsUiState(
            vehicleForm = VehicleForm.from(settings.vehicle),
            thresholdsForm = ThresholdsForm.from(settings.priceThresholds),
            estimationMode = settings.estimationMode,
            loaded = true,
        )
    }

    // --- Edición del vehículo ----------------------------------------------

    fun onVehicleFieldChange(update: (VehicleForm) -> VehicleForm) {
        _forms.update { it.copy(vehicleForm = update(it.vehicleForm), savedMessage = false) }
    }

    fun onThresholdFieldChange(update: (ThresholdsForm) -> ThresholdsForm) {
        _forms.update { it.copy(thresholdsForm = update(it.thresholdsForm), savedMessage = false) }
    }

    fun onEstimationModeChange(mode: EstimationMode) {
        viewModelScope.launch { repository.updateEstimationMode(mode) }
    }

    /** Guarda el perfil y los umbrales si ambos son válidos. */
    fun onSave() {
        val state = _forms.value
        val profile = state.vehicleForm.toProfile()
        val thresholds = state.thresholdsForm.toThresholds()
        viewModelScope.launch {
            if (profile != null) repository.updateVehicle(profile)
            if (thresholds != null) repository.updatePriceThresholds(thresholds)
            _forms.update { it.copy(savedMessage = profile != null || thresholds != null) }
        }
    }

    /** Restaura los valores de fábrica del perfil y de los umbrales. */
    fun onRestoreDefaults() {
        viewModelScope.launch {
            repository.restoreDefaults()
            refreshForms()
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                ) as CargaEvApplication
                SettingsViewModel(application.container.settingsRepository)
            }
        }
    }
}
