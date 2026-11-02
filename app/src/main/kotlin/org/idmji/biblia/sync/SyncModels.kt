package org.idmji.biblia.sync

data class CompendioItemSync(
    val id: Int,
    val tipo: String,
    val libroId: Int?,
    val capitulo: Int?,
    val versiculoInicio: Int?,
    val versiculoFin: Int?,
    val cita: String?,
    val titulo: String,
    val videoUrl: String?,
    val videoId: String?,
    val seccion: String?,
    val transcripcion: String?
)

sealed class SyncResult {
    data class Success(val newItemsCount: Int, val totalCount: Int) : SyncResult()
    data class AlreadyUpToDate(val totalCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

data class AppUpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val isUpdateAvailable: Boolean,
    val releaseName: String,
    val releaseNotes: String,
    val downloadUrl: String
)

sealed class UpdateCheckResult {
    data class Available(val info: AppUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}
