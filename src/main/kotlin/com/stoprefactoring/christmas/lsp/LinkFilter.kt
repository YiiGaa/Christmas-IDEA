package com.stoprefactoring.christmas.lsp

import com.intellij.ide.DataManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.WindowManager
import com.stoprefactoring.christmas.base.Language
import java.awt.Component
import java.awt.Container
import javax.swing.JEditorPane
import javax.swing.SwingConstants
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

object LinkFilter {
    //TIPS::Install listener
    private fun installListener(pane: JEditorPane, project: Project) {
        val key = "xmas.link.installed"
        if (pane.getClientProperty(key) == true) return
        val currentFile = FileEditorManager.getInstance(project).selectedEditor?.file
        if (currentFile?.extension != "xmas") return
        pane.putClientProperty(key, true)

        pane.addHyperlinkListener(HyperlinkListener { ev ->
            if (ev.eventType != HyperlinkEvent.EventType.ACTIVATED) return@HyperlinkListener
            val urlStr = ev.url?.toString() ?: ev.description ?: return@HyperlinkListener
            if (urlStr.startsWith("xmas://")) {
                openReadmeFile(urlStr.removePrefix("xmas://"), project)
            }
        })
    }

    //TIPS::Open readme file(xmas://)
    private fun openReadmeFile(fileName: String, project: Project) {
        //STEP::Get file path
        val basePath  = project.basePath ?: return
        val projectRoot =  LocalFileSystem.getInstance().findFileByPath(basePath)
        var readmeFile = projectRoot?.findFileByRelativePath(fileName)
        if (readmeFile == null) {
            readmeFile = LocalFileSystem.getInstance().findFileByPath(fileName)
        }

        //STEP::Open file
        if (readmeFile != null && readmeFile.exists()) {
            //STEP-IN::Get handler
            val manager = FileEditorManagerEx.Companion.getInstanceEx(project)
            val allWindows = manager.windows
            val currentWindow = manager.currentWindow ?: return

            //STEP-IN::Get target window
            val nextWindow = manager.getNextWindow(currentWindow)
            val targetWindow = if (allWindows.size > 1 && nextWindow != currentWindow) {
                nextWindow
            } else {
                null
            }

            //STEP-IN::Focus editor window
            ApplicationManager.getApplication().invokeLater({
                val editor = FileEditorManager.getInstance(project).selectedTextEditor
                if (editor != null) {
                    IdeFocusManager.getInstance(project).requestFocus(editor.contentComponent, true)
                }
            })

            //WHEN-IN::Has 2 split window
            if (targetWindow != null) {
                targetWindow.setAsCurrentWindow(true);
                manager.openFile(readmeFile, true)
            }
            //WHEN-IN::Only one window
            else{
                manager.createSplitter(SwingConstants.VERTICAL, currentWindow)
                val nextWindow = manager.getNextWindow(currentWindow)
                nextWindow?.setAsCurrentWindow(true)
                manager.openFile(readmeFile, true)
            }
            //manager.openFile(readmeFile, true)

            //STEP-IN::Press 'esc' for hide hover message
            ApplicationManager.getApplication().invokeLater {
                val editor = FileEditorManager.getInstance(project).selectedTextEditor
                val actionManager = ActionManager.getInstance()
                val escapeAction = actionManager.getAction(IdeActions.ACTION_EDITOR_ESCAPE)
                if (escapeAction != null) {
                    val context = DataManager.getInstance().getDataContext(editor?.contentComponent)
                    val event = AnActionEvent.createEvent(
                        context,
                        escapeAction.templatePresentation.clone(),
                        "ExternalDocHandler",
                        ActionUiKind.Companion.NONE,
                        null
                    )
                    ActionUtil.performAction(escapeAction, event)
                }
            }

        } else {
            Notifications.Bus.notify(
                Notification(
                    "Christmas Notifications",
                    Language.text("link.error.title"),
                    Language.text("link.error.message")+": $fileName",
                    NotificationType.ERROR
                ),
                project
            )
        }
    }

    //TIPS::Trigger
    fun Trigger(component: Component, project: Project) {
        when (component) {
            is JEditorPane -> {
                if (component.contentType?.startsWith("text/html") == true) {
                    installListener(component, project)
                }
            }
            is Container -> {
                for (c in component.components) Trigger(c, project)
            }
        }
    }
}