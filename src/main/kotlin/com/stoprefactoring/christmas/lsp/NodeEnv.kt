package com.stoprefactoring.christmas.lsp
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.stoprefactoring.christmas.base.Language
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CompletableFuture
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

    //TIPS::Get download info
    private data class DownloadInfo(val fileName: String, val url: String)
    private fun GetDownloadInfo(): DownloadInfo? {
        //STEP::Get x64/arm64 arch
        val arch = System.getProperty("os.arch").lowercase()
        val archSuffix = when {
            arch.contains("aarch64") || arch.contains("arm64") -> "arm64"
            else -> "x64"
        }

        //STEP::Get download info
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
        val pluginRootDir = GetPluginRootDir() ?: return null
        return File(pluginRootDir, "runtime").also { it.mkdirs() }
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
        val lspDir = File(runtimeDir, "lsp").also { it.mkdirs() }
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

    //TIPS::Download and extract node.js
    private fun DownloadAndExtract(
        downloadUrl: String,
        targetDir: File,
        indicator: ProgressIndicator
    ) {
        //STEP::Update message text
        indicator.text = Language.text("lsp.info.link")
        indicator.isIndeterminate = false

        //STEP::Connect download url
        val uri = URI(downloadUrl)
        val url = uri.toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()
        val contentLength = connection.contentLength
        val inputStream = connection.inputStream
        val isZip = downloadUrl.endsWith(".zip")

        //WHEN::Windows(.zip)
        if (isZip) {
            ZipInputStream(inputStream).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    val targetFile = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        if(entry.name.endsWith("node.exe")) {
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { out ->
                                val buffer = ByteArray(8192)
                                var len: Int
                                var totalRead = 0L
                                while (zipIn.read(buffer).also { len = it } > 0) {
                                    out.write(buffer, 0, len)
                                    totalRead += len
                                    if (contentLength > 0) {
                                        indicator.fraction = totalRead.toDouble() / contentLength
                                    }
                                }
                            }
                        }
                    }
                    entry = zipIn.nextEntry
                }
            }
        }
        //WHEN::macOS/Linux(.tar.gz)
        else {
            val tempFile = File.createTempFile("node", ".tar.gz")
            try {
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

                indicator.text = Language.text("lsp.info.extract")
                val process = ProcessBuilder("tar", "-xzf", tempFile.absolutePath, "-C", targetDir.absolutePath)
                    .start()
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw RuntimeException(Language.text("lsp.error.extract")+"$exitCode")
                }

                //STEP-IN::Copy bin/node
                targetDir.walkTopDown().maxDepth(3).forEach { file ->
                    if (file.name == "node" && file.parentFile?.name == "bin") {
                        file.copyTo(File(targetDir, "node"), overwrite = true)
                        file.delete()
                    }
                }
            } finally {
                tempFile.delete()
            }
        }
        inputStream.close()
        connection.disconnect()
    }

    //TIPS::Check local node
    private fun CheckLocal(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("node", "-v"))
            val versionString = process.inputStream.bufferedReader().readText().trim()
            val majorVersion = versionString.removePrefix("v").split(".").firstOrNull()?.toInt() ?: 0
            majorVersion >= NODE_LOCAL
        } catch (e: Exception) {
            false
        }
    }

    //TIPS::Trigger function
    fun Trigger(project: Project): CompletableFuture<String> {
        //STEP::Init return
        val future = CompletableFuture<String>()
        //TIPS::For testing
        if(CheckLocal()){
            future.complete("node")
            return future
        }

        //STEP::Create node dir('runtime')
        val targetDir = GetRuntimeDir()
        if (targetDir == null) {
            future.completeExceptionally(IllegalStateException(Language.text("lsp.error.dir")))
            return future
        }

        //STEP::Get download info
        val downloadInfo = GetDownloadInfo()
        if (downloadInfo == null) {
            if(CheckLocal()){
                future.complete("node")
                return future
            }
            future.completeExceptionally(IllegalStateException(Language.text("lsp.error.system")))
            return future
        }

        //STEP::Check file exists
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

        //STEP::Download and extract node
        val task = object : Task.Backgroundable(project, Language.text("lsp.info.download"), true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    DownloadAndExtract(downloadInfo.url, targetDir, indicator)
                    if (!SystemInfo.isWindows) {
                        nodeFile.setExecutable(true)
                    }
                    future.complete(nodeFile.absolutePath)
                } catch (e: Exception) {
                    LOG.warn(Language.text("lsp.error.download"), e)
                    if(CheckLocal()){
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
