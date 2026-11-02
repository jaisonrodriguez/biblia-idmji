package org.idmji.biblia.sync

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GitHubUpdateService(
    private val repoOwner: String = DEFAULT_OWNER,
    private val repoName: String = DEFAULT_REPO
) {
    companion object {
        private const val TAG = "GitHubUpdateService"
        const val DEFAULT_OWNER = "jaisonrodriguez"
        const val DEFAULT_REPO = "biblia-idmji"
        const val DEFAULT_FALLBACK_APK_URL = "https://github.com/jaisonrodriguez/biblia-idmji/releases/download/v1.0.0/Biblia_IDMJI.apk"
    }

    suspend fun checkForUpdates(currentVersionName: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val endpoint = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 15000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "BibliaIDMJI-Android")
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                return@withContext UpdateCheckResult.Error("HTTP $responseCode al consultar GitHub: $errorMsg")
            }

            val responseJson = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use {
                it.readText()
            }

            parseReleaseJson(responseJson, currentVersionName)
        } catch (e: Exception) {
            Log.e(TAG, "Error al comprobar actualizaciones", e)
            UpdateCheckResult.Error("Error de red: ${e.localizedMessage ?: "No se pudo conectar a GitHub"}")
        }
    }

    fun parseReleaseJson(jsonString: String, currentVersionName: String): UpdateCheckResult {
        val root = JSONObject(jsonString)
        val tagName = root.optString("tag_name", "")
        val releaseName = root.optString("name", tagName)
        val body = root.optString("body", "")
        val htmlUrl = root.optString("html_url", "https://github.com/$repoOwner/$repoName/releases")

        if (tagName.isBlank()) {
            return UpdateCheckResult.Error("Respuesta de versión no válida en GitHub")
        }

        // Buscar el APK en assets
        var downloadUrl = ""
        val assets = root.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                val assetUrl = asset.optString("browser_download_url", "")
                if (assetName.equals("Biblia_IDMJI.apk", ignoreCase = true)) {
                    downloadUrl = assetUrl
                    break
                } else if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.isEmpty()) {
                    downloadUrl = assetUrl
                }
            }
        }

        if (downloadUrl.isEmpty()) {
            downloadUrl = htmlUrl
        }

        val hasUpdate = isNewerVersion(tagName, currentVersionName)

        val info = AppUpdateInfo(
            latestVersion = tagName,
            currentVersion = currentVersionName,
            isUpdateAvailable = hasUpdate,
            releaseName = releaseName,
            releaseNotes = body,
            downloadUrl = downloadUrl
        )

        return if (hasUpdate) {
            UpdateCheckResult.Available(info)
        } else {
            UpdateCheckResult.UpToDate(currentVersionName)
        }
    }

    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = remoteTag.removePrefix("v").removePrefix("V").trim()
        val cleanCurrent = currentVersion.removePrefix("v").removePrefix("V").trim()

        val remoteParts = cleanRemote.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
