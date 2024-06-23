package com.stoprefactoring.christmas

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import java.io.File

internal class Start : ToolWindowFactory {
    private val viewPainting = ViewPainting()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        ApplicationManager.getApplication().invokeLater {
            val factory = ContentFactory.getInstance()
            val viewBody = viewPainting.start(project)
            val content = factory.createContent(viewBody, null, true)
            toolWindow.contentManager.addContent(content)
        }
    }
}

internal class ListenOpenProject : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        //STEP::Wait toolWindow init
        ToolWindowManager.getInstance(project).invokeLater{
            //STEP-IN::Check project has Christmas/Christmas.py or not, not in project will hide plugin
            val filePath = project.basePath + "/Christmas/Christmas.py"
            val fileSystem = LocalFileSystem.getInstance()
            val file = fileSystem.findFileByPath(filePath.replace('/', File.separatorChar))
            if (file != null && file.exists()) {
                ToolWindowManager.getInstance(project).getToolWindow("Christmas")?.setAvailable(true, null)
            } else {
                ToolWindowManager.getInstance(project).getToolWindow("Christmas")?.setAvailable(false, null)
            }
        }
    }
}
