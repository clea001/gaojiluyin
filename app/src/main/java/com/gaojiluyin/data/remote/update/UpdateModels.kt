package com.gaojiluyin.data.remote.update

data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val apkSize: Long,
    val md5: String,
    val changelog: String,
    val forceUpdate: Boolean = false,
    val mirrors: List<MirrorInfo> = emptyList()
)

data class MirrorInfo(
    val name: String,
    val baseUrl: String
)

object UpdateConfig {
    const val GITHUB_REPO = "Clea0/gaojiluyin"
    const val VERSION_CHECK_PATH = "releases/latest/version.json"

    val MIRRORS = listOf(
        MirrorInfo("GitHub", "https://github.com"),
        MirrorInfo("ghproxy", "https://mirror.ghproxy.com/https://github.com"),
        MirrorInfo("FastGit", "https://hub.gitmirror.com/https://github.com"),
        MirrorInfo("Gitee(需同步)", "https://gitee.com/Clea0/gaojiluyin/raw/main")
    )

    fun getVersionCheckUrls(): List<String> {
        val urls = mutableListOf<String>()
        for (mirror in MIRRORS) {
            urls.add("${mirror.baseUrl}/$GITHUB_REPO/raw/main/version.json")
        }
        return urls
    }

    fun getApkDownloadUrls(apkUrl: String): List<String> {
        val urls = mutableListOf<String>()
        for (mirror in MIRRORS) {
            urls.add("${mirror.baseUrl}/$GITHUB_REPO/releases/download/$apkUrl")
        }
        return urls
    }
}
