package com.stoprefactoring.christmas.lsp
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

internal class XmasLSPSupport : LspServerSupportProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ) {
        if (file.extension != "xmas") return
        if(NodeEnv.isReady){
            NodeEnv.isStartLSP = true
            serverStarter.ensureServerStarted(XmasLspServerDescriptor(project))
        } else{
            NodeEnv.isCall = true
        }
    }
}