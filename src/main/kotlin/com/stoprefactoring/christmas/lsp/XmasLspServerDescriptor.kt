package com.stoprefactoring.christmas.lsp
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import java.util.Locale

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
        val serverJsFile = NodeEnv.GetLspServerFile()

        //STEP::Set command
        return GeneralCommandLine().apply {
            withExePath(NodeEnv.node)
            //withExePath("node")
            addParameter("--max-old-space-size=1024")
            addParameter(serverJsFile.absolutePath)
            addParameter("--stdio")
            withWorkDirectory(project.basePath)
        }
    }

    //TIPS::Set extension filter
    override fun isSupportedFile(file: VirtualFile): Boolean {
        return file.extension == XmasFileType.defaultExtension
    }
}
