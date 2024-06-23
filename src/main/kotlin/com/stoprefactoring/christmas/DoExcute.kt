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
    ApplicationManager.getApplication().invokeLater {
        //STEP-IN::Make command
        val cmds = ArrayList<String>()
        if (SystemInfo.isWindows)
            cmds.add("powershell.exe")
        cmds.add("python3")
        cmds.add("Christmas/Christmas.py")
        cmds.add(function)
        val generalCommandLine: GeneralCommandLine = GeneralCommandLine(cmds)
        generalCommandLine.charset = Charset.forName("UTF-8")
        generalCommandLine.setWorkDirectory(project.basePath)

        //STEP-IN::Run command and active toolWindow
        val shellWindow = TerminalToolWindowManager.getInstance(project)
        if (DoExcute_ConsoleExcute_Terminal != null) {
            val container = shellWindow.getContainer(DoExcute_ConsoleExcute_Terminal!!)
            DoExcute_ConsoleExcute_Terminal?.whenDisposed {
                DoExcute_ConsoleExcute_Terminal = shellWindow.createShellWidget(project.basePath, "Christmas-Run", true, true)
                DoExcute_ConsoleExcute_Terminal?.sendCommandToExecute(generalCommandLine.commandLineString)
                DoExcute_ConsoleExcute_Terminal?.requestFocus()
            }
            container?.closeAndHide()
        } else{
            DoExcute_ConsoleExcute_Terminal = shellWindow.createShellWidget(project.basePath, "Christmas-Run", true, true)
            DoExcute_ConsoleExcute_Terminal?.sendCommandToExecute(generalCommandLine.commandLineString)
            DoExcute_ConsoleExcute_Terminal?.requestFocus()
        }
    }
}
fun DoExcute_MarkSlected_Run(project:Project){
    ApplicationManager.getApplication().invokeLater {
        //STEP::Make command
        val cmds = ArrayList<String>()
        if (SystemInfo.isWindows)
            cmds.add("powershell.exe")
        cmds.add("python3")
        cmds.add("Christmas/Christmas.py")
        cmds.add(DoExcute_MarkSlected_Slect)
        val generalCommandLine: GeneralCommandLine = GeneralCommandLine(cmds)
        generalCommandLine.charset = Charset.forName("UTF-8")
        generalCommandLine.setWorkDirectory(project.basePath)

        //STEP::Run command and active toolWindow
        val shellWindow = TerminalToolWindowManager.getInstance(project)
        if (DoExcute_ConsoleExcute_Terminal != null) {
            val container = shellWindow.getContainer(DoExcute_ConsoleExcute_Terminal!!)
            DoExcute_ConsoleExcute_Terminal?.whenDisposed {
                DoExcute_ConsoleExcute_Terminal = shellWindow.createShellWidget(project.basePath, "Christmas-Run", true, true)
                DoExcute_ConsoleExcute_Terminal?.sendCommandToExecute(generalCommandLine.commandLineString)
                DoExcute_ConsoleExcute_Terminal?.requestFocus()
            }
            container?.closeAndHide()
        } else{
            DoExcute_ConsoleExcute_Terminal = shellWindow.createShellWidget(project.basePath, "Christmas-Run", true, true)
            DoExcute_ConsoleExcute_Terminal?.sendCommandToExecute(generalCommandLine.commandLineString)
            DoExcute_ConsoleExcute_Terminal?.requestFocus()
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
        val consoleWindow = ToolWindowManager.getInstance(project).getToolWindow("Christmas-Run")
        var consoleComponent = consoleWindow?.contentManager?.getContent(0)?.component
        val console: ConsoleView = consoleComponent as ConsoleView
        consoleWindow?.show()
        console.clear()
        console.print(filePath+" not found", ConsoleViewContentType.ERROR_OUTPUT)
    }
}

class HidePluginAction : AnAction {
    // 如果需要，可以添加构造函数来接受参数，例如图标或文本
    constructor(text: String, description: String, icon: Icon?) : super(text, description, icon)

    // 无参构造函数
    constructor() : super("My Hidden Action", "Description of my hidden action", null)

    override fun actionPerformed(e: AnActionEvent) {
        // 动作被触发时的逻辑
        val project = e.project
        println("- actionPerformed")
        if (project != null) {
            // 执行需要项目上下文的代码
        } else {
            // 处理没有打开项目的特殊情况
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        println("- update")
        val project = e.project
        // 根据项目是否存在来设置动作的可见性和可用性
        e.presentation.isEnabledAndVisible = project != null && isApplicable(project)
    }

    private fun isApplicable(project: Project): Boolean {
        // 这里实现你的逻辑，决定是否隐藏动作
        // 例如，检查项目中是否存在特定的文件或配置
        return false // 假设默认不适用，根据条件返回 true 或 false
    }
}