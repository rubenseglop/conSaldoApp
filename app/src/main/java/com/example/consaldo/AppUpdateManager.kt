package com.example.consaldo

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String
)

object AppUpdateManager {
    private const val RELEASE_URL =
        "https://api.github.com/repos/rubenseglop/conSaldoApp/releases/latest"

    suspend fun checkForUpdate(context: Context): AppUpdate? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(RELEASE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "conSaldo-Android")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null

            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (json.optBoolean("draft", false) || json.optBoolean("prerelease", false)) {
                return@withContext null
            }

            val latestVersion = json.optString("tag_name", "").removePrefix("v")
            if (latestVersion.isBlank()) return@withContext null

            val currentVersion = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName ?: "0"
            if (compareVersions(latestVersion, currentVersion) <= 0) return@withContext null

            val assets = json.optJSONArray("assets") ?: return@withContext null
            var apkUrl: String? = null
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    if (!apkUrl.isNullOrBlank()) break
                }
            }

            if (apkUrl.isNullOrBlank()) return@withContext null
            AppUpdate(latestVersion, json.optString("body", "").take(1200), apkUrl)
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    fun downloadUpdate(context: Context, update: AppUpdate) {
        val request = DownloadManager.Request(Uri.parse(update.apkUrl)).apply {
            setTitle("conSaldo ${update.versionName}")
            setDescription("Descargando actualización. Pulsa la notificación cuando termine para instalarla.")
            setMimeType("application/vnd.android.package-archive")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
            setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "conSaldo-${update.versionName}.apk"
            )
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }

    private fun compareVersions(first: String, second: String): Int {
        val a = first.split(".", "-", "_").map { it.toIntOrNull() ?: 0 }
        val b = second.split(".", "-", "_").map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(a.size, b.size)) {
            val left = a.getOrElse(index) { 0 }
            val right = b.getOrElse(index) { 0 }
            if (left != right) return left.compareTo(right)
        }
        return 0
    }
}
