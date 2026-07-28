package com.pamoron.electroperico.domain.ocr

/** Valores candidatos detectados en la pantalla de un cargador. */
data class ChargerTextCandidates(
    val pricePerKWh: Double? = null,
    val chargerPowerKw: Double? = null,
    val pricePerMinuteEur: Double? = null,
)

/**
 * Extrae las tarifas y la potencia de texto reconocido por OCR.
 *
 * No rellena ningún campo de la aplicación: la interfaz muestra siempre estos
 * valores como propuestas para que la persona confirme o corrija la lectura.
 * El parser no depende de Android ni de ML Kit para poder probar las reglas
 * con textos reales de distintos operadores.
 */
object ChargerTextParser {

    private val pricePerKwh = Regex(
        """(?<![\d.,])(\d{1,2}(?:[.,]\d{1,4})?)\s*(?:€|eur)\s*/?\s*kwh\b""",
        RegexOption.IGNORE_CASE,
    )
    private val power = Regex(
        """(?<![\d.,])(\d{1,4}(?:[.,]\d{1,2})?)\s*kw\b""",
        RegexOption.IGNORE_CASE,
    )
    private val pricePerMinute = Regex(
        """(?<![\d.,])(\d{1,2}(?:[.,]\d{1,4})?)\s*(?:€|eur)\s*/?\s*(?:min|minuto)\b""",
        RegexOption.IGNORE_CASE,
    )

    /** Devuelve la primera coincidencia razonable de cada tipo de dato. */
    fun parse(text: String): ChargerTextCandidates = ChargerTextCandidates(
        pricePerKWh = firstValid(pricePerKwh, text, 0.0..10.0),
        chargerPowerKw = firstValid(power, text, 1.0..1_000.0),
        pricePerMinuteEur = firstValid(pricePerMinute, text, 0.0..10.0),
    )

    private fun firstValid(regex: Regex, text: String, range: ClosedRange<Double>): Double? =
        regex.findAll(text)
            .mapNotNull { result -> result.groupValues[1].replace(',', '.').toDoubleOrNull() }
            .firstOrNull { value -> value.isFinite() && value in range }
}
