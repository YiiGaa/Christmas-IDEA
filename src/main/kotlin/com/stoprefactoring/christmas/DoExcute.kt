package com.stoprefactoring.christmas

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.observable.util.whenDisposed
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.ui.TerminalWidget
import com.intellij.ui.content.ContentFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import org.jetbrains.plugins.terminal.ui.TerminalContainer
import java.awt.event.MouseEvent
import java.io.File
import java.nio.charset.Charset
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath


val viewPainting = ViewPainting()
fun DoExcute_Reflash(project: Project) {
    //STEP::Clean all contents
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Christmas")
    //val view = toolWindow?.contentManager?.getContent(0) as JBScrollPane

    //STEP::Make again
    ApplicationManager.getApplication().invokeLater {
        //STEP-IN::Sync file
        val filePath = project.basePath
        val fileSystem = LocalFileSystem.getInstance()
        val file = fileSystem.findFileByPath(filePath?.replace('/', File.separatorChar) ?: "")
        VfsUtil.markDirtyAndRefresh(false, true, true,file)

        //STEP-IN::Remake
        val factory = ContentFactory.getInstance()
        val viewBody = viewPainting.start(project)
        val content = factory.createContent(viewBody, null, true)
        toolWindow?.contentManager?.removeAllContents(true)
        toolWindow?.contentManager?.addContent(content)
        toolWindow?.show()
    }
}

var DoExcute_ConsoleExcute_Terminal:TerminalWidget ?= null
fun DoExcute_ConsoleExcute(event:MouseEvent, tree: JTree, project:Project) {
    //STEP::Get tree selected node
    val path: TreePath = tree.getClosestPathForLocation(event.x, event.y) ?: return
    val lastPathComponent: Any = path.getLastPathComponent()
    val node = lastPathComponent as DefaultMutableTreeNode

    //WHEN::The node is not leaf
    if(!node.isLeaf) {return}

    //STEP::Get task function
    val parentNode = lastPathComponent.parent as DefaultMutableTreeNode
    val function = parentNode.userObject as String + "/" + node.userObject as String
    
    //STEP::Run command
    DoExcute_ConsoleExcute_Terminal_Do(project, function)
}
fun DoExcute_MarkSlected_Run(project:Project){
    DoExcute_ConsoleExcute_Terminal_Do(project, DoExcute_MarkSlected_Slect)
}
fun DoExcute_ConsoleExcute_Terminal_Do(project: Project, taskFuncion:String){
    ApplicationManager.getApplication().invokeLater {
        //STEP-IN::Make command
        val cmds = ArrayList<String>()
        if (SystemInfo.isWindows)
            cmds.add("powershell.exe")
        cmds.add("python3")
        cmds.add("Christmas/Christmas.py")
        cmds.add(taskFuncion)
        val generalCommandLine: GeneralCommandLine = GeneralCommandLine(cmds)
        generalCommandLine.charset = Charset.forName("UTF-8")
        generalCommandLine.setWorkDirectory(project.basePath)

        //STEP-IN::Run command and active toolWindow
        val tabName = "Christmas-Run"
        val consoleWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal")
        consoleWindow?.show()
        val terminalWindow = TerminalToolWindowManager.getInstance(project)
        if(DoExcute_ConsoleExcute_Terminal == null) {
            for(widget in (terminalWindow.terminalWidgets)){
                if(widget.terminalTitle.defaultTitle.equals(tabName)||
                    widget.terminalTitle.userDefinedTitle.equals(tabName)){
                    DoExcute_ConsoleExcute_Terminal = widget
                    break
                }
            }
        }
        var termianlContainer:TerminalContainer ?=null
        if(DoExcute_ConsoleExcute_Terminal!=null)
            termianlContainer = terminalWindow.getContainer(DoExcute_ConsoleExcute_Terminal!!)
        if(termianlContainer==null){
            DoExcute_ConsoleExcute_Terminal = terminalWindow.createShellWidget(project.basePath, tabName, true, true)
            DoExcute_ConsoleExcute_Terminal?.sendCommandToExecute(generalCommandLine.commandLineString)
            DoExcute_ConsoleExcute_Terminal?.requestFocus()
        }else{
            DoExcute_ConsoleExcute_Terminal?.whenDisposed {
                DoExcute_ConsoleExcute_Terminal = terminalWindow.createShellWidget(project.basePath, tabName, true, true)
                DoExcute_ConsoleExcute_Terminal?.sendCommandToExecute(generalCommandLine.commandLineString)
                DoExcute_ConsoleExcute_Terminal?.requestFocus()
            }
            termianlContainer.closeAndHide()
        }
    }
}

var DoExcute_MarkSlected_Slect = "";
fun DoExcute_MarkSlected(event:MouseEvent, tree: JTree, project:Project):Boolean {
    //STEP::Get tree selected node
    val path: TreePath = tree.getClosestPathForLocation(event.x, event.y) ?: return false
    val lastPathComponent: Any = path.getLastPathComponent()
    val node = lastPathComponent as DefaultMutableTreeNode

    //WHEN::The node is not leaf
    if(!node.isLeaf) {return false}

    //STEP::Get task function
    val parentNode = lastPathComponent.parent as DefaultMutableTreeNode
    DoExcute_MarkSlected_Slect = parentNode.userObject as String + "/" + node.userObject as String
    return true
}
fun DoExcute_MarkSlected_OpenFile(project:Project, tail:String){
    //STEP::Find file
    val filePath = project.basePath + "/Christmas/Input/"+DoExcute_MarkSlected_Slect+"/"+tail
    val fileSystem = LocalFileSystem.getInstance()
    val file = fileSystem.findFileByPath(filePath.replace('/', File.separatorChar))

    //WHEN::Open file when found
    if (file != null && file.exists()) {
        val descriptor = OpenFileDescriptor(project, file)
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
    }
    //WHEN::Output error info when not found
    else {
//        val consoleWindow = ToolWindowManager.getInstance(project).getToolWindow("Christmas-Run")
//        var consoleComponent = consoleWindow?.contentManager?.getContent(0)?.component
//        val console: ConsoleView = consoleComponent as ConsoleView
//        consoleWindow?.show()
//        console.clear()
//        console.print(filePath+" not found", ConsoleViewContentType.ERROR_OUTPUT)
    }
}