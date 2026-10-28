package com.sanctuary.core.sync

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal data class HttpResponse(
    val statusCode: Int,
    val body: String,
)

internal class SupabaseHttp(
    private val config: SupabaseConfig,
    private val timeoutMillis: Int = 15_000,
) {
    fun post(
        path: String,
        body: String,
        bearerToken: String,
        extraHeaders: Map<String, String> = emptyMap(),
    ): HttpResponse {
        val connection = (URL(config.baseUrl + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.doOutput = true
            connection.setRequestProperty("apikey", config.publishableKey)
            connection.setRequestProperty("Authorization", "Bearer $bearerToken")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("User-Agent", "Sanctuary/0.1.0")
            extraHeaders.forEach { (name, value) ->
                connection.setRequestProperty(name, value)
            }

            val bytes = body.toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return HttpResponse(status, responseBody)
        } finally {
            connection.disconnect()
        }
    }
}

internal open class SupabaseHttpException(
    val statusCode: Int,
    message: String,
) : IOException(message)

internal class SupabaseAuthException(
    statusCode: Int,
    message: String,
) : SupabaseHttpException(statusCode, message)

internal class SupabaseRestException(
    statusCode: Int,
    message: String,
) : SupabaseHttpException(statusCode, message)
