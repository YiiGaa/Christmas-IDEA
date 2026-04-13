package com.stoprefactoring.christmas

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.lsp.api.LspServerManager
import com.stoprefactoring.christmas.base.Language
import com.stoprefactoring.christmas.lsp.LinkFilter
import com.stoprefactoring.christmas.lsp.NodeEnv
import com.stoprefactoring.christmas.lsp.XmasLSPSupport
import com.stoprefactoring.christmas.lsp.XmasLspServerDescriptor
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.ContainerEvent
import java.io.File
import javax.swing.SwingUtilities

class Init : ProjectActivity {
    override suspend fun execute(project: Project) {
        //STEP::Check project has Christmas/Christmas.py or not
        val filePath = project.basePath + "/Christmas/Christmas.py"
        val fileSystem = LocalFileSystem.getInstance()
        val file = fileSystem.findFileByPath(filePath.replace('/', File.separatorChar))
        val isAvailable = file != null && file.exists()

        //STEP::Hide or show plugin
        ToolWindowManager.getInstance(project).invokeLater{
            if (isAvailable) {
                ToolWindowManager.getInstance(project).getToolWindow("Christmas")?.setAvailable(true, null)
            } else {
                ToolWindowManager.getInstance(project).getToolWindow("Christmas")?.setAvailable(false, null)
            }
        }

        //STEP::Prepare Node environment
        if(isAvailable) {
            NodeEnv.Trigger(project).thenAccept { nodePath ->
                ApplicationManager.getApplication().invokeLater {
                    NodeEnv.isReady = true
                    NodeEnv.node = nodePath
                    if(NodeEnv.isCall) {
                        NodeEnv.isStartLSP = true
                        LspServerManager.getInstance(project).ensureServerStarted(
                            XmasLSPSupport::class.java,
                            XmasLspServerDescriptor(project)
                        )
                    }
                }
            }.exceptionally { throwable ->
                val cause = throwable.cause ?: throwable
                val errorMessage = cause.message ?: throwable.toString()
                Notifications.Bus.notify(
                    Notification(
                        "Christmas Notifications",
                        Language.text("lsp.error.title"),
                        errorMessage,
                        NotificationType.ERROR
                    ),
                    project
                )
                null
            }
        }

        //STEP::Event listen for link of hover document(xmas://)
        if(isAvailable) {
            val awtListener = AWTEventListener { event ->
                if (event is ContainerEvent && event.id == ContainerEvent.COMPONENT_ADDED) {
                    SwingUtilities.invokeLater {
                        LinkFilter.Trigger(event.child, project)
                    }
                }
            }
            Toolkit.getDefaultToolkit().addAWTEventListener(awtListener, AWTEvent.CONTAINER_EVENT_MASK)
            val connection = project.messageBus.connect()
            Disposer.register(connection, Disposable {
                Toolkit.getDefaultToolkit().removeAWTEventListener(awtListener)
            })
        }
    }
}