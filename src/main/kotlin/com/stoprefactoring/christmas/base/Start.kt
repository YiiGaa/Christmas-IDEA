package com.stoprefactoring.christmas.base

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader.getIcon
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.JBColor
import com.intellij.ui.content.ContentFactory
import javax.swing.Icon

internal class Start : ToolWindowFactory {
    private val viewPainting = ViewPainting()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        //TIPS::For icon bug in light theme
        val activeIcon = getIcon("/icon/logo.svg", javaClass)
        val normalIcon = getIcon("/icon/logo_light.svg", javaClass)
        val darkIcon = getIcon("/icon/logo_dark.svg", javaClass)
        updateIcon(toolWindow, activeIcon, normalIcon,darkIcon, "null");
        project.messageBus.connect()
            .subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
                override fun stateChanged(toolWindowManager: ToolWindowManager): Unit {
                    toolWindowManager.activeToolWindowId
                    val activeId: String = toolWindowManager.activeToolWindowId?: "null"
                    updateIcon(toolWindow,activeIcon, normalIcon, darkIcon,activeId)
                }
            })

        //STEP::Draw window
        ApplicationManager.getApplication().invokeLater {
            val factory = ContentFactory.getInstance()
            val viewBody = viewPainting.start(project)
            val content = factory.createContent(viewBody, null, true)
            toolWindow.contentManager.addContent(content)
        }
    }
    private fun updateIcon(toolWindow: ToolWindow, activeIcon: Icon, normalIcon: Icon, darkIcon:Icon, activeToolWindowId: String) {
        if (toolWindow.isVisible && activeToolWindowId=="Christmas") {
                toolWindow.setIcon(activeIcon)
        } else {
            if(JBColor.isBright()) {
                toolWindow.setIcon(normalIcon)}
            else {
                toolWindow.setIcon(darkIcon)
            }
        }
    }
}