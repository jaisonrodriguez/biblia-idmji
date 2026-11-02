package org.idmji.biblia.sync

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class TursoSyncService(
    private val endpointUrl: String = DEFAULT_TURSO_URL,
    private val authToken: String = DEFAULT_TURSO_TOKEN
) {
    companion object {
        private const val TAG = "TursoSyncService"
        const val DEFAULT_TURSO_URL = "https://biblia-idmji-jaisonrodriguez.aws-sa-east-1.turso.io/v2/pipeline"
        const val DEFAULT_TURSO_TOKEN = "eyJhbGciOiJFZERTQSIsInR5cCI6IkpXVCJ9.eyJhIjoicnciLCJpYXQiOjE3OTM2NDcxNDIsImlkIjoiMDFhMTlkYjItNWUwMS03MWI1LWIyZWYtN2U2MDEwZjdkMmU4Iiwia2lkIjoiWk8xNF9kUElKXzFCQTVRSERrVUF3T19QVjhUZ2M3TEhMMmFFNmNZUGlncyIsInJpZCI6ImEyNzU4YTk1LTBlODgtNDk5Mi05YThkLWUzNWEzMjRhMWE5ZiJ9.oNBIyZ99PN8Mz8-peTka86_NbymbcRmqXkCVcT8-4ecXBNvF0ORtVhEQ63JCjx0yapmkXkHZo8MxSddlMANwCA"
    }

    suspend fun fetchTeachingsAfterId(afterId: Int, limit: Int = 500): List<CompendioItemSync> = withContext(Dispatchers.IO) {
        val sql = "SELECT id, tipo, libro_id, capitulo, versiculo_inicio, versiculo_fin, cita, titulo, video_url, video_id, seccion, transcripcion FROM compendio_idmji WHERE id > $afterId ORDER BY id ASC LIMIT $limit"
        val responseJson = postPipelineQuery(sql)
        parseTursoRows(responseJson)
    }

    suspend fun fetchRemoteTotalCount(): Int = withContext(Dispatchers.IO) {
        val sql = "SELECT COUNT(*) FROM compendio_idmji"
        val responseJson = postPipelineQuery(sql)
        parseCountResult(responseJson)
    }

    private fun postPipelineQuery(sql: String): String {
        val url = URL(endpointUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 25000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $authToken")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        val requestBody = JSONObject().apply {
            val requests = JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "execute")
                    put("stmt", JSONObject().apply {
                        put("sql", sql)
                    })
                })
            }
            put("requests", requests)
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(requestBody.toString())
            writer.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            val errorStream = conn.errorStream ?: conn.inputStream
            val errorText = errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw java.io.IOException("HTTP $responseCode al conectar con Turso: $errorText")
        }

        return BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { reader ->
            reader.readText()
        }
    }

    fun parseTursoRows(responseBody: String): List<CompendioItemSync> {
        val root = JSONObject(responseBody)
        val results = root.optJSONArray("results") ?: return emptyList()
        if (results.length() == 0) return emptyList()
        val firstResult = results.getJSONObject(0)
        if (firstResult.optString("type") == "error") {
            val errObj = firstResult.optJSONObject("error")
            throw java.io.IOException("Error de Turso: ${errObj?.optString("message") ?: "Error desconocido"}")
        }
        val responseObj = firstResult.optJSONObject("response") ?: return emptyList()
        val resultObj = responseObj.optJSONObject("result") ?: return emptyList()
        val rows = resultObj.optJSONArray("rows") ?: return emptyList()

        val list = ArrayList<CompendioItemSync>(rows.length())
        for (i in 0 until rows.length()) {
            val row = rows.getJSONArray(i)
            val idVal = extractCell(row.getJSONObject(0))?.toIntOrNull() ?: continue
            val tipo = extractCell(row.getJSONObject(1)) ?: "CANONICO"
            val libroId = extractCell(row.getJSONObject(2))?.toIntOrNull()
            val capitulo = extractCell(row.getJSONObject(3))?.toIntOrNull()
            val versiculoInicio = extractCell(row.getJSONObject(4))?.toIntOrNull()
            val versiculoFin = extractCell(row.getJSONObject(5))?.toIntOrNull()
            val cita = extractCell(row.getJSONObject(6))
            val titulo = extractCell(row.getJSONObject(7)) ?: "Enseñanza"
            val videoUrl = extractCell(row.getJSONObject(8))
            val videoId = extractCell(row.getJSONObject(9))
            val seccion = extractCell(row.getJSONObject(10))
            val transcripcion = extractCell(row.getJSONObject(11))

            list.add(
                CompendioItemSync(
                    id = idVal,
                    tipo = tipo,
                    libroId = libroId,
                    capitulo = capitulo,
                    versiculoInicio = versiculoInicio,
                    versiculoFin = versiculoFin,
                    cita = cita,
                    titulo = titulo,
                    videoUrl = videoUrl,
                    videoId = videoId,
                    seccion = seccion,
                    transcripcion = transcripcion
                )
            )
        }
        return list
    }

    fun parseCountResult(responseBody: String): Int {
        val root = JSONObject(responseBody)
        val results = root.optJSONArray("results") ?: return 0
        if (results.length() == 0) return 0
        val firstResult = results.getJSONObject(0)
        val responseObj = firstResult.optJSONObject("response") ?: return 0
        val resultObj = responseObj.optJSONObject("result") ?: return 0
        val rows = resultObj.optJSONArray("rows") ?: return 0
        if (rows.length() == 0) return 0
        val firstRow = rows.getJSONArray(0)
        if (firstRow.length() == 0) return 0
        val countCell = firstRow.getJSONObject(0)
        return extractCell(countCell)?.toIntOrNull() ?: 0
    }

    private fun extractCell(cell: JSONObject): String? {
        val type = cell.optString("type")
        if (type == "null") return null
        return if (cell.has("value") && !cell.isNull("value")) cell.getString("value") else null
    }
}
