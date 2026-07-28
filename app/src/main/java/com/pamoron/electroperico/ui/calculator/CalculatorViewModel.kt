package com.pamoron.electroperico.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pamoron.electroperico.ElectroPericoApplication
import com.pamoron.electroperico.data.comparator.ComparatorRepository
import com.pamoron.electroperico.data.history.HistoryRepository
import com.pamoron.electroperico.data.settings.AppSettings
import com.pamoron.electroperico.data.settings.SettingsRepository
import com.pamoron.electroperico.domain.calc.ChargeCalculator
import com.pamoron.electroperico.domain.calc.Money
import com.pamoron.electroperico.domain.model.CalculationOutcome
import com.pamoron.electroperico.domain.model.ChargerOption
import com.pamoron.electroperico.domain.model.HistoryEntry
import com.pamoron.electroperico.domain.model.CurrentType
import com.pamoron.electroperico.domain.ocr.ChargerTextCandidates
import com.pamoron.electroperico.ui.format.Formatters
import java.util.UUID
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Avisos puntuales de la calculadora. */
enum class CalculatorMessage {
    GUARDADO_EN_COMPARADOR,
    COMPARADOR_LLENO,
    GUARDADO_EN_HISTORIAL,
}

/**
 * Lógica de presentación de la calculadora.
 *
 * El cálculo se rehace en cuanto cambia cualquier campo, de modo que los
 * resultados están siempre al día; el botón "Calcular" solo decide *cuándo*
 * empiezan a mostrarse. Así la pantalla es inmediata sin sorprender al usuario
 * enseñándole cifras antes de que haya terminado de escribir.
 */
class CalculatorViewModel(
    private val repository: SettingsRepository,
    private val comparatorRepository: ComparatorRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _inputs = MutableStateFlow(CalculatorInputs())
    private val _resultsRequested = MutableStateFlow(false)

    /** Evita que la precarga se lance más de una vez al recomponer la pantalla. */
    private var started = false

    private val _message = MutableStateFlow<CalculatorMessage?>(null)

    /** Aviso puntual que la pantalla muestra como mensaje emergente. */
    val message: StateFlow<CalculatorMessage?> = _message.asStateFlow()

    /** Estado observado por la pantalla. */
    val uiState: StateFlow<CalculatorUiState> = combine(
        _inputs,
        repository.settings,
        _resultsRequested,
    ) { inputs, settings, requested ->
        CalculatorUiState(
            inputs = inputs,
            vehicle = settings.vehicle,
            estimationMode = settings.estimationMode,
            calculation = calculate(inputs, settings),
            resultsRequested = requested,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = CalculatorUiState(),
    )

    /**
     * Carga los valores recordados de la última sesión y activa su guardado
     * automático. La pantalla la llama una sola vez, al entrar.
     */
    @OptIn(FlowPreview::class)
    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            // Precarga los valores de la última sesión...
            val stored = repository.settings.first().lastSession
            _inputs.value = CalculatorInputs.from(stored)
            // ...y a partir de ahí guarda los cambios sin escribir en cada tecla.
            _inputs.debounce(SAVE_DEBOUNCE_MS).collect { current ->
                repository.saveLastSession(current.toLastSession())
            }
        }
    }

    // --- Eventos de la pantalla --------------------------------------------

    fun onPriceChange(value: String) = _inputs.update { it.copy(priceText = value.sanitize()) }

    fun onPowerChange(value: String) = _inputs.update { it.copy(powerText = value.sanitize()) }

    fun onCurrentTypeChange(type: CurrentType) = _inputs.update { it.copy(currentType = type) }

    /** El objetivo siempre debe quedar por encima del punto de partida. */
    fun onStartSocChange(value: Int) = _inputs.update { current ->
        val start = value.coerceIn(0, 99)
        current.copy(startSoc = start, targetSoc = maxOf(current.targetSoc, start + 1))
    }

    fun onTargetSocChange(value: Int) = _inputs.update { current ->
        val target = value.coerceIn(1, 100)
        current.copy(startSoc = minOf(current.startSoc, target - 1), targetSoc = target)
    }

    fun onStartFeeChange(value: String) = _inputs.update { it.copy(startFeeText = value.sanitize()) }

    fun onPricePerMinuteChange(value: String) =
        _inputs.update { it.copy(pricePerMinuteText = value.sanitize()) }

    fun onParkingFeeChange(value: String) =
        _inputs.update { it.copy(parkingFeeText = value.sanitize()) }

    /** Aplica únicamente los valores que la persona ha confirmado en el lector OCR. */
    fun onOcrValuesConfirmed(values: ChargerTextCandidates) = _inputs.update { current ->
        current.copy(
            priceText = values.pricePerKWh?.let(Formatters::toEditableText) ?: current.priceText,
            powerText = values.chargerPowerKw?.let(Formatters::toEditableText) ?: current.powerText,
            pricePerMinuteText = values.pricePerMinuteEur?.let(Formatters::toEditableText)
                ?: current.pricePerMinuteText,
        )
    }

    fun onToggleExtras() = _inputs.update { it.copy(extrasExpanded = !it.extrasExpanded) }

    /** Intercambia el porcentaje actual y el objetivo. */
    fun onSwapSoc() = _inputs.update { current ->
        // Solo tiene sentido si el resultado sigue siendo un intervalo válido.
        if (current.startSoc == current.targetSoc) {
            current
        } else {
            current.copy(startSoc = current.targetSoc, targetSoc = current.startSoc)
        }
    }

    /** Vacía todos los campos y oculta los resultados. */
    fun onClear() {
        _inputs.update { CalculatorInputs(currentType = it.currentType) }
        _resultsRequested.value = false
    }

    /** Muestra los resultados. A partir de aquí se actualizan solos. */
    fun onCalculate() {
        _resultsRequested.value = true
    }

    /** Guarda la simulación actual como una opción del comparador. */
    fun onSaveToComparator() {
        val option = currentOption() ?: return
        viewModelScope.launch {
            _message.value = if (comparatorRepository.upsert(option)) {
                CalculatorMessage.GUARDADO_EN_COMPARADOR
            } else {
                CalculatorMessage.COMPARADOR_LLENO
            }
        }
    }

    /** Guarda la simulación actual en el historial local. */
    fun onSaveToHistory() {
        val option = currentOption() ?: return
        val resultado = (uiState.value.calculation as? CalculationState.Listo)?.result ?: return
        viewModelScope.launch {
            historyRepository.upsert(
                HistoryEntry.from(
                    id = option.id,
                    charger = option,
                    result = resultado,
                    timestampMillis = System.currentTimeMillis(),
                ),
            )
            _message.value = CalculatorMessage.GUARDADO_EN_HISTORIAL
        }
    }

    /**
     * Construye una opción a partir de lo que hay escrito en la pantalla.
     *
     * El nombre se compone con la potencia y el precio, que es lo que permite
     * reconocer el cargador de un vistazo; se puede cambiar luego al editarlo.
     */
    private fun currentOption(): ChargerOption? {
        val inputs = _inputs.value
        val input = inputs.toChargeInput() ?: return null
        return ChargerOption(
            id = UUID.randomUUID().toString(),
            name = "${Formatters.power(input.chargerPowerKw)} · " +
                Formatters.pricePerKWh(Money.rate(input.pricePerKWh)),
            pricePerKWh = input.pricePerKWh,
            chargerPowerKw = input.chargerPowerKw,
            currentType = input.currentType,
            startSocPercent = inputs.startSoc,
            targetSocPercent = inputs.targetSoc,
            startFeeEur = input.startFeeEur,
            pricePerMinuteEur = input.pricePerMinuteEur,
            parkingFeeEur = input.parkingFeeEur,
        )
    }

    /** La pantalla avisa de que ya ha mostrado el mensaje. */
    fun onMessageShown() {
        _message.value = null
    }

    // --- Cálculo ------------------------------------------------------------

    private fun calculate(inputs: CalculatorInputs, settings: AppSettings): CalculationState {
        if (!inputs.hasRequiredFields) return CalculationState.Incompleto
        val input = inputs.toChargeInput() ?: return CalculationState.Incompleto
        return when (
            val outcome = ChargeCalculator.calculate(
                input = input,
                vehicle = settings.vehicle,
                estimationMode = settings.estimationMode,
                priceThresholds = settings.priceThresholds,
            )
        ) {
            is CalculationOutcome.Success -> CalculationState.Listo(outcome.result)
            is CalculationOutcome.Failure -> CalculationState.Invalido(outcome.errors)
        }
    }

    /**
     * Limpia el texto de un campo numérico: quita espacios y símbolos que el
     * teclado decimal puede colar, y deja como mucho un separador decimal.
     */
    private fun String.sanitize(): String {
        val filtered = filter { it.isDigit() || it == ',' || it == '.' }
        val firstSeparator = filtered.indexOfFirst { it == ',' || it == '.' }
        if (firstSeparator < 0) return filtered
        val head = filtered.substring(0, firstSeparator + 1)
        val tail = filtered.substring(firstSeparator + 1).filter { it.isDigit() }
        return head + tail
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val SAVE_DEBOUNCE_MS = 800L

        /** Fábrica que resuelve el repositorio desde el contenedor de la aplicación. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                ) as ElectroPericoApplication
                CalculatorViewModel(
                    repository = application.container.settingsRepository,
                    comparatorRepository = application.container.comparatorRepository,
                    historyRepository = application.container.historyRepository,
                )
            }
        }
    }
}
