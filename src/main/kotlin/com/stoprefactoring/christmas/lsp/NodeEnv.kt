package com.stoprefactoring.christmas.lsp

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.stoprefactoring.christmas.base.Language
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

private const val PLUGIN_ID = "com.stoprefactoring.Christmas"
private const val NODE_VERSION = "v22.22.0"
private const val NODE_LOCAL = 20
private const val NODE_DOWNLOAD_BASE = "https://nodejs.org/dist/$NODE_VERSION/"

object NodeEnv {
    private val LOG = Logger.getInstance(NodeEnv::class.java)

    @Volatile
    var isReady: Boolean = false
    @Volatile
    var isCall: Boolean = false
    @Volatile
    var isStartLSP: Boolean = false
    @Volatile
    var node: String = "node"

    private data class DownloadInfo(val fileName: String, val url: String)

    private fun GetArchSuffix(): String {
        val arch = System.getProperty("os.arch").lowercase()
        return when {
            arch.contains("aarch64") || arch.contains("arm64") -> "arm64"
            else -> "x64"
        }
    }

    private fun GetDownloadInfo(): DownloadInfo? {
        val archSuffix = GetArchSuffix()
        return when {
            SystemInfo.isWindows -> {
                DownloadInfo(
                    fileName = "node.exe",
                    url = "${NODE_DOWNLOAD_BASE}node-$NODE_VERSION-win-$archSuffix.zip"
                )
            }
            SystemInfo.isMac -> {
                DownloadInfo(
                    fileName = "node",
                    url = "${NODE_DOWNLOAD_BASE}node-$NODE_VERSION-darwin-$archSuffix.tar.gz"
                )
            }
            SystemInfo.isLinux -> {
                DownloadInfo(
                    fileName = "node",
                    url = "${NODE_DOWNLOAD_BASE}node-$NODE_VERSION-linux-$archSuffix.tar.gz"
                )
            }
            else -> null
        }
    }

    private fun GetPluginRootDir(): File? {
        val pluginId = PluginId.getId(PLUGIN_ID)
        val pluginPath = PluginManagerCore.getPlugin(pluginId)?.pluginPath?.toFile()
        if (pluginPath != null && pluginPath.exists()) {
            return pluginPath
        }

        return try {
            val locationFile = File(NodeEnv::class.java.protectionDomain.codeSource.location.toURI())
            when {
                locationFile.isFile && locationFile.parentFile?.name == "lib" -> locationFile.parentFile?.parentFile
                locationFile.isDirectory -> locationFile
                else -> locationFile.parentFile
            }?.takeIf { it.exists() }
        } catch (e: Exception) {
            LOG.warn(Language.text("lsp.error.dir"), e)
            null
        }
    }

    private fun GetRuntimeDir(): File? {
        val platform = when {
            SystemInfo.isWindows -> "windows"
            SystemInfo.isMac -> "mac"
            SystemInfo.isLinux -> "linux"
            else -> "unknown"
        }
        val runtimeDir = File(
            PathManager.getSystemPath(),
            "Christmas/runtime/${NODE_VERSION.removePrefix("v")}-${platform}-${GetArchSuffix()}"
        ).canonicalFile
        return if (runtimeDir.exists() || runtimeDir.mkdirs()) runtimeDir else null
    }

    fun GetLspServerFile(): File {
        val pluginRootDir = GetPluginRootDir()
            ?: throw IllegalStateException(Language.text("lsp.error.dir"))

        val directFile = File(pluginRootDir, "lsp/index.js")
        if (directFile.exists()) {
            return directFile
        }

        val packagedFile = File(pluginRootDir, "resources/lsp/index.js")
        if (packagedFile.exists()) {
            return packagedFile
        }

        val runtimeDir = GetRuntimeDir()
            ?: throw IllegalStateException(Language.text("lsp.error.dir"))
        val lspDir = File(runtimeDir, "lsp")
        if (!lspDir.exists() && !lspDir.mkdirs()) {
            throw IllegalStateException(Language.text("lsp.error.dir"))
        }
        val runtimeFile = File(lspDir, "index.js")
        if (runtimeFile.exists()) {
            return runtimeFile
        }

        val resourceStream = NodeEnv::class.java.classLoader.getResourceAsStream("lsp/index.js")
            ?: throw IllegalStateException(Language.text("lsp.error.file"))
        resourceStream.use { input ->
            FileOutputStream(runtimeFile).use { output ->
                input.copyTo(output)
            }
        }
        return runtimeFile
    }

    private fun NormalizeArchiveEntry(entryName: String): String {
        return entryName.replace('\\', '/').trimStart('/')
    }

    private fun PrepareTargetFile(targetFile: File) {
        val parentFile = targetFile.parentFile
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            throw IllegalStateException(Language.text("lsp.error.dir"))
        }
    }

    private fun CopyStreamToFile(input: java.io.InputStream, targetFile: File) {
        PrepareTargetFile(targetFile)
        FileOutputStream(targetFile).use { output ->
            input.copyTo(output)
        }
    }

    private fun ExtractNodeBinaryFromZip(archiveFile: File, targetFile: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null) {
                val entryName = NormalizeArchiveEntry(entry.name)
                if (!entry.isDirectory && (entryName.endsWith("/node.exe") || entryName == "node.exe")) {
                    CopyStreamToFile(zipIn, targetFile)
                    return
                }
                entry = zipIn.nextEntry
            }
        }
        throw IllegalStateException(Language.text("lsp.error.file"))
    }

    private fun ExtractNodeBinaryFromTarGz(archiveFile: File, targetFile: File) {
        GZIPInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { gzipIn ->
            TarArchiveInputStream(gzipIn).use { tarIn ->
                var entry = tarIn.nextTarEntry
                while (entry != null) {
                    val entryName = NormalizeArchiveEntry(entry.name)
                    if (!entry.isDirectory && (entryName.endsWith("/bin/node") || entryName == "bin/node" || entryName == "node")) {
                        CopyStreamToFile(tarIn, targetFile)
                        return
                    }
                    entry = tarIn.nextTarEntry
                }
            }
        }
        throw IllegalStateException(Language.text("lsp.error.file"))
    }

    private fun DownloadAndExtract(
        downloadUrl: String,
        targetDir: File,
        indicator: ProgressIndicator,
        fileName: String
    ) {
        indicator.text = Language.text("lsp.info.link")
        indicator.isIndeterminate = false

        val uri = URI(downloadUrl)
        val url = uri.toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            connection.disconnect()
            throw RuntimeException(Language.text("lsp.error.download"))
        }

        val tempFile = File.createTempFile("node", if (downloadUrl.endsWith(".zip")) ".zip" else ".tar.gz")
        try {
            val contentLength = connection.contentLengthLong
            connection.inputStream.use { inputStream ->
                FileOutputStream(tempFile).use { out ->
                    val buffer = ByteArray(8192)
                    var len: Int
                    var totalRead = 0L
                    while (inputStream.read(buffer).also { len = it } > 0) {
                        out.write(buffer, 0, len)
                        totalRead += len
                        if (contentLength > 0) {
                            indicator.fraction = totalRead.toDouble() / contentLength
                        }
                    }
                }
            }

            indicator.text = Language.text("lsp.info.extract")
            val targetFile = File(targetDir, fileName)
            if (downloadUrl.endsWith(".zip")) {
                ExtractNodeBinaryFromZip(tempFile, targetFile)
            } else {
                ExtractNodeBinaryFromTarGz(tempFile, targetFile)
            }
        } finally {
            tempFile.delete()
            connection.disconnect()
        }
    }

    private fun CheckLocal(): Boolean {
        return try {
            val process = ProcessBuilder("node", "-v").start()
            val versionString = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            if (exitCode != 0 || versionString.isBlank()) {
                return false
            }
            val majorVersion = versionString.removePrefix("v").split(".").firstOrNull()?.toInt() ?: 0
            majorVersion >= NODE_LOCAL
        } catch (e: Exception) {
            false
        }
    }

    fun Trigger(project: Project): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        if (CheckLocal()) {
            future.complete("node")
            return future
        }

        val targetDir = GetRuntimeDir()
        if (targetDir == null) {
            future.completeExceptionally(IllegalStateException(Language.text("lsp.error.dir")))
            return future
        }

        val downloadInfo = GetDownloadInfo()
        if (downloadInfo == null) {
            if (CheckLocal()) {
                future.complete("node")
                return future
            }
            future.completeExceptionally(IllegalStateException(Language.text("lsp.error.system")))
            return future
        }

        val nodeFile = File(targetDir, downloadInfo.fileName)
        if (nodeFile.exists()) {
            if (!SystemInfo.isWindows && !nodeFile.canExecute()) {
                nodeFile.setExecutable(true)
            }
            if (SystemInfo.isWindows || nodeFile.canExecute()) {
                future.complete(nodeFile.absolutePath)
                return future
            }
        }

        val task = object : Task.Backgroundable(project, Language.text("lsp.info.download"), true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    DownloadAndExtract(downloadInfo.url, targetDir, indicator, downloadInfo.fileName)
                    if (!nodeFile.exists()) {
                        throw IllegalStateException(Language.text("lsp.error.file"))
                    }
                    if (!SystemInfo.isWindows && !nodeFile.canExecute()) {
                        nodeFile.setExecutable(true)
                    }
                    if (!SystemInfo.isWindows && !nodeFile.canExecute()) {
                        throw IllegalStateException(Language.text("lsp.error.file"))
                    }
                    future.complete(nodeFile.absolutePath)
                } catch (e: Exception) {
                    LOG.warn(Language.text("lsp.error.download"), e)
                    if (CheckLocal()) {
                        future.complete("node")
                    } else {
                        future.completeExceptionally(IllegalStateException(Language.text("lsp.error.download")))
                    }
                }
            }
        }
        task.queue()
        return future
    }
}
