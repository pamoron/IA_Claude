package com.pamoron.electroperico.data.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

data class AvailableUpdate(val version: String, val downloadUrl: String)

sealed interface UpdateCheckResult {
    data class Available(val update: AvailableUpdate) : UpdateCheckResult
    data object Current : UpdateCheckResult
    data object Unavailable : UpdateCheckResult
}

/** Consulta la release pública sin enviar datos de la persona usuaria. */
class GithubUpdateRepository(private val context: Context) {
    suspend fun check(): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Electro-Perico-Android")
            }
            try {
                if (connection.responseCode !in 200..299) return@runCatching UpdateCheckResult.Unavailable
                val root = Json.parseToJsonElement(response.inputStream.bufferedReader().readText()).jsonObject
                val version = root["tag_name"]?.jsonPrimitive?.content.orEmpty().removePrefix("v")
                val apk = root["assets"]?.jsonArray
                    ?.firstOrNull { it.jsonObject["name"]?.jsonPrimitive?.content == "Electro_Perico.apk" }
                    ?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.content
                    ?: return@runCatching UpdateCheckResult.Unavailable
                if (isNewer(version, installedVersion())) UpdateCheckResult.Available(AvailableUpdate(version, apk))
                else UpdateCheckResult.Current
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(UpdateCheckResult.Unavailable)
    }

    private fun installedVersion(): String = context.packageManager
        .getPackageInfo(context.packageName, 0).versionName.orEmpty()

    private fun isNewer(remote: String, local: String): Boolean {
        fun parts(value: String) = value.split(Regex("[^0-9]+"))
            .filter { it.isNotBlank() }.map { it.toIntOrNull() ?: 0 }
        val remoteParts = parts(remote)
        val localParts = parts(local)
        val size = maxOf(remoteParts.size, localParts.size)
        return (0 until size).firstOrNull { remoteParts.getOrElse(it) { 0 } != localParts.getOrElse(it) { 0 } }
            ?.let { remoteParts.getOrElse(it) { 0 } > localParts.getOrElse(it) { 0 } } ?: false
    }

    private companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/pamoron/IA_Claude/releases/latest"
        const val TIMEOUT_MS = 10_000
    }
}
