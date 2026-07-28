package com.pamoron.electroperico.ui.format

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Formato de números en español.
 *
 * Los símbolos se fijan a mano (coma decimal, punto de millares) en lugar de
 * confiar en la configuración regional del dispositivo: así el resultado es
 * idéntico en cualquier terminal y las pruebas unitarias son deterministas.
 *
 * Antes del símbolo del euro se usa un espacio duro, que es lo correcto en
 * español y evita que el importe se parta al final de una línea.
 */
object Formatters {

    /** Espacio duro (U+00A0). */
    const val NBSP: String = "\u00A0"

    private val SPANISH: Locale = Locale.forLanguageTag("es-ES")

    private val symbols: DecimalFormatSymbols = DecimalFormatSymbols(SPANISH).apply {
        decimalSeparator = ','
        groupingSeparator = '.'
    }

    private fun formatter(pattern: String): DecimalFormat =
        DecimalFormat(pattern, symbols)

    private val moneyFormat = formatter("#,##0.00")
    private val rateFormat = formatter("#,##0.00")
    private val energyFormat = formatter("#,##0.00")
    private val powerFormat = formatter("#,##0.#")
    private val integerFormat = formatter("#,##0")
    private val percentFormat = formatter("#,##0.#")

    /** Importe con dos decimales y símbolo de euro: `12,45 €`. */
    fun money(value: BigDecimal): String = moneyFormat.format(value) + NBSP + "€"

    /** Importe a partir de un [Double]. */
    fun money(value: Double): String = money(BigDecimal.valueOf(value))

    /** Importe sin el símbolo, para cuando la unidad ya está en la etiqueta. */
    fun moneyPlain(value: BigDecimal): String = moneyFormat.format(value)

    /** Precio unitario: `0,45 €/kWh`. */
    fun pricePerKWh(value: BigDecimal): String = rateFormat.format(value) + NBSP + "€/kWh"

    /** Precio unitario con signo explícito, para diferencias: `+0,07 €/kWh`. */
    fun signedPricePerKWh(value: BigDecimal): String {
        val sign = if (value.signum() > 0) "+" else ""
        return sign + pricePerKWh(value)
    }

    /** Energía con dos decimales: `40,43 kWh`. */
    fun energy(value: Double): String = energyFormat.format(value) + NBSP + "kWh"

    /** Potencia con un decimal como mucho: `155 kW`, `7,4 kW`. */
    fun power(value: Double): String = powerFormat.format(value) + NBSP + "kW"

    /** Distancia redondeada a kilómetros: `225 km`. */
    fun distance(value: Double): String = integerFormat.format(Math.round(value)) + NBSP + "km"

    /** Porcentaje: `60 %`, `12,5 %`. */
    fun percent(value: Double): String = percentFormat.format(value) + NBSP + "%"

    /**
     * Duración redondeada a minutos.
     *
     * Por debajo de una hora se muestra `27 min`; a partir de ahí, `1 h 27 min`.
     */
    fun duration(minutes: Int): String {
        if (minutes < 60) return "$minutes${NBSP}min"
        val hours = minutes / 60
        val rest = minutes % 60
        return if (rest == 0) "$hours${NBSP}h" else "$hours${NBSP}h $rest${NBSP}min"
    }

    /**
     * Abreviaturas de los meses.
     *
     * Se escriben a mano en lugar de usar los datos regionales del sistema para
     * que la fecha se vea igual en cualquier dispositivo y las pruebas sean
     * deterministas.
     */
    private val MESES = arrayOf(
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic",
    )

    /** Fecha y hora de una recarga del historial: `28 jul 2026, 01:15`. */
    fun dateTime(millis: Long, zone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = millis
        val dia = cal.get(Calendar.DAY_OF_MONTH)
        val mes = MESES[cal.get(Calendar.MONTH)]
        val anio = cal.get(Calendar.YEAR)
        val hora = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val minuto = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
        return "$dia $mes $anio, $hora:$minuto"
    }

    /**
     * Convierte texto escrito por el usuario en un número.
     *
     * Acepta coma o punto como separador decimal y espacios sobrantes.
     * Devuelve `null` si el texto no es un número válido.
     */
    fun parseDecimal(text: String): Double? {
        val cleaned = text.trim().replace(',', '.')
        if (cleaned.isEmpty()) return null
        return cleaned.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    /**
     * Igual que [parseDecimal] pero devuelve `0.0` cuando el campo está vacío.
     * Se usa en los costes adicionales, que son opcionales y parten de cero.
     */
    fun parseOptionalDecimal(text: String): Double? =
        if (text.isBlank()) 0.0 else parseDecimal(text)

    /** Escribe un número para precargarlo en un campo de texto, sin ceros sobrantes. */
    fun toEditableText(value: Double): String {
        if (value == 0.0) return ""
        val formatted = formatter("#0.####").format(value)
        return formatted
    }
}
