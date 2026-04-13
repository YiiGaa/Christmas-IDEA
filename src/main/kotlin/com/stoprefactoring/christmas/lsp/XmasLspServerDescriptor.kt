package com.stoprefactoring.christmas.lsp
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.openapi.extensions.PluginId
import com.stoprefactoring.christmas.base.Language
import java.util.Locale

private const val PLUGIN_ID = "com.stoprefactoring.Christmas"

class XmasLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, presentableName = "My Language Server") {
    //TIPS::Set 'getLanguageId' of .xmas file
    override fun getLanguageId(file: VirtualFile): String {
        if (file.extension == XmasFileType.defaultExtension) {
            return XmasFileType.name
        }
        return super.getLanguageId(file)
    }

    //TIPS::Set lsp init param
    override fun createInitializationOptions(): Any {
        val options: MutableMap<String?, Any?> = HashMap()
        val language: String? = Locale.getDefault().language
        options["lang"] = language
        options["root"] = project.basePath
        options["ide"] = "jetbrains"
        return options
    }

    //TIPS::Make lsp command
    override fun createCommandLine(): GeneralCommandLine {
        //STEP::Find lsp program(index.js)
        val pluginId = PluginId.getId(PLUGIN_ID)
        val pluginPath = PluginManagerCore.getPlugin(pluginId)?.pluginPath
            ?: throw IllegalStateException(Language.text("lsp.error.dir"))
        val serverJsFile = pluginPath.resolve("lsp/index.js").toFile()
        if (!serverJsFile.exists()) {
            val fallbackFile = pluginPath.resolve("resources/lsp/index.js").toFile()
            if (!fallbackFile.exists()) {
                throw IllegalStateException(Language.text("lsp.error.file"))
            }
        }

        //STEP::Set command
        return GeneralCommandLine().apply {
            withExePath(NodeEnv.node)
            //withExePath("node")
            addParameter(serverJsFile.absolutePath)
            addParameter("--stdio")
            addParameter("--max-old-space-size=1024")
            withWorkDirectory(project.basePath)
        }
    }

    //TIPS::Set extension filter
    override fun isSupportedFile(file: VirtualFile): Boolean {
        return file.extension == XmasFileType.defaultExtension
    }
}