package com.gaojiluyin.data.remote.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    private var downloadId: Long = -1L
    private var downloadedFile: File? = null

    suspend fun checkForUpdate(): VersionInfo? = withContext(Dispatchers.IO) {
        _updateState.value = UpdateState.Checking

        val urls = UpdateConfig.getVersionCheckUrls()
        for (url in urls) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.code == 200) {
                    val body = response.body?.string() ?: continue
                    val json = JSONObject(body)
                    val versionInfo = VersionInfo(
                        versionCode = json.getInt("versionCode"),
                        versionName = json.getString("versionName"),
                        apkUrl = json.getString("apkUrl"),
                        apkSize = json.getLong("apkSize"),
                        md5 = json.getString("md5"),
                        changelog = json.getString("changelog"),
                        forceUpdate = json.optBoolean("forceUpdate", false)
                    )

                    val currentVersion = getCurrentVersionCode()
                    if (versionInfo.versionCode > currentVersion) {
                        _updateState.value = UpdateState.Available(versionInfo)
                        return@withContext versionInfo
                    } else {
                        _updateState.value = UpdateState.UpToDate
                        return@withContext null
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }
        _updateState.value = UpdateState.Error("无法检查更新")
        null
    }

    fun startDownload(apkUrl: String) {
        _updateState.value = UpdateState.Downloading(0)

        val urls = UpdateConfig.getApkDownloadUrls(apkUrl)
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(Uri.parse(urls[0])).apply {
            setTitle("高级录音 更新")
            setDescription("正在下载新版本...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "gaojiluyin-update.apk")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        downloadId = downloadManager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    context.unregisterReceiver(this)
                    handleDownloadComplete(downloadManager)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        pollDownloadProgress(downloadManager)
    }

    private fun pollDownloadProgress(downloadManager: DownloadManager) {
        Thread {
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))

                    when (status) {
                        DownloadManager.STATUS_RUNNING -> {
                            val progress = if (total > 0) (downloaded * 100 / total).toInt() else 0
                            _updateState.value = UpdateState.Downloading(progress)
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> downloading = false
                        DownloadManager.STATUS_FAILED -> {
                            _updateState.value = UpdateState.Error("下载失败")
                            downloading = false
                        }
                    }
                }
                cursor.close()
                Thread.sleep(500)
            }
        }.start()
    }

    private fun handleDownloadComplete(downloadManager: DownloadManager) {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val uriString = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            val file = File(Uri.parse(uriString).path!!)
            downloadedFile = file
            _updateState.value = UpdateState.Downloaded(file)
        }
        cursor.close()
    }

    fun installUpdate(file: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.fromFile(file)
            }
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun getCurrentVersionCode(): Int {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode.toInt()
                else @Suppress("DEPRECATION") it.versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) { 0 }
    }
}

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data object UpToDate : UpdateState()
    data class Available(val info: VersionInfo) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class Downloaded(val file: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}
