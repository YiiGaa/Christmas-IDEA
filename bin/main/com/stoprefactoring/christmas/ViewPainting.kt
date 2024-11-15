package com.stoprefactoring.christmas

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel


class ViewPainting {
    private var menuPanel = JBPopupMenu()

    private fun menuMake_mouseHoverAdapter(): MouseAdapter {
        return object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                if(e.component is JBMenuItem) {
                    (e.component as JBMenuItem).background = UIManager.getColor("Table.selectionBackground")
                }
            }
            override fun mouseExited(e: MouseEvent) {
                if(e.component is JBMenuItem) {
                    (e.component as JBMenuItem).background = null
                }
            }
        }
    }

    private fun menuMake(project:Project): JBPopupMenu {
        //STEP::Init menu
        menuPanel.removeAll()
        menuPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        //STEP::Init menu item
        val menuItem_1 = JBMenuItem(Language.text("view.menuRun"))
        val menuItem_2 = JBMenuItem(Language.text("view.menuOpenTarget"))
        val menuItem_3 = JBMenuItem(Language.text("view.menuOpenConfig"))
        menuItem_1.icon = AllIcons.Actions.Execute
        menuItem_2.icon = AllIcons.Actions.ProjectWideAnalysisOn
        menuItem_3.icon = AllIcons.Actions.ProjectWideAnalysisOn
        menuItem_1.iconTextGap = 10
        menuItem_2.iconTextGap = 10
        menuItem_3.iconTextGap = 10
        menuItem_1.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
        menuItem_2.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
        menuItem_3.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)

        menuItem_1.addActionListener { e1 -> DoExcute_MarkSlected_Run(project) }
        menuItem_2.addActionListener { e1 -> DoExcute_MarkSlected_OpenFile(project,"target.json") }
        menuItem_3.addActionListener { e1 -> DoExcute_MarkSlected_OpenFile(project,"config.json") }

        //STEP::Insert menu item
        menuPanel.add(menuItem_1)
        menuPanel.add(menuItem_2)
        menuPanel.add(menuItem_3)

        //STEP::Add menu item listener
        menuItem_1.addMouseListener(menuMake_mouseHoverAdapter())
        menuItem_2.addMouseListener(menuMake_mouseHoverAdapter())
        menuItem_3.addMouseListener(menuMake_mouseHoverAdapter())
        
        return menuPanel
    }

    private fun taskListMake(project: Project):Tree{
        //STEP::Init tree root node
        val rootNode = DefaultMutableTreeNode("Christmas")

        //STEP::Get Christmas/Input file list as task list
        val taskMap: MutableMap<String, MutableList<String>> = TreeMap()
        val filePath = project.basePath + "/Christmas/Input"
        val fileSystem = LocalFileSystem.getInstance()
        val taskDir = fileSystem.findFileByPath(filePath.replace('/', File.separatorChar))
        if(taskDir != null){
            val children: Array<VirtualFile> = taskDir.children
            for (child in children) {
                if (child.isDirectory) {
                    val taskList: MutableList<String> = ArrayList()
                    child.children.forEach { taskDir ->
                        if (taskDir.isDirectory) {
                            taskList.add(taskDir.name)
                        }
                    }
                    taskList.sortWith(Comparator.naturalOrder())
                    taskMap.put(child.name, taskList)
                }
            }
            taskMap.forEach { key, value ->
                val taskDir = DefaultMutableTreeNode(key)
                value.forEach {
                    value_2 ->
                    taskDir.add(DefaultMutableTreeNode(value_2))
                }
                rootNode.add(taskDir)
            }
        }

        //STEP::Init tree
        val tree = Tree(DefaultTreeModel(rootNode))
        tree.isRootVisible = false

        //STEP::Setting tree render
        tree.cellRenderer = object: ColoredTreeCellRenderer() {
            override fun customizeCellRenderer(tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) {
                if (value is DefaultMutableTreeNode) {
                    if(leaf){
                        this.icon = AllIcons.CodeWithMe.CwmAccessDotOn
                        this.toolTipText = Language.text("view.tips")
                    }
                    append(value.userObject.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES,10, 20)
                }
            }
        }

        //STEP::Expand row
        tree.expandRow(tree.rowCount-1)
        tree.revalidate();
        tree.repaint();

        //STEP::Add Listener
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.getClickCount() === 2) {
                    DoExcute_ConsoleExcute(e, tree, project)
                }
            }
            override fun mouseReleased(e: MouseEvent?) {
                if (e!=null && e.button == MouseEvent.BUTTON3) {
                    if(DoExcute_MarkSlected(e, tree, project)) {
                        menuPanel.show(e.component, e.x, e.y)
                    }
                }
            }
        })

        return tree
    }

    private fun headMenu(project:Project): JPanel {
        //STEP::Init panel
        val headMenu = JPanel()
        headMenu.layout = BoxLayout(headMenu, BoxLayout.X_AXIS)
        headMenu.add(Box.createHorizontalStrut(5))

        //STEP::Init button
        val refreshButton = JButton()
        refreshButton.icon = AllIcons.Actions.Refresh
        refreshButton.setMargin(Insets(10,5,10,5))
        refreshButton.setContentAreaFilled(false)
        refreshButton.setLayout(FlowLayout(FlowLayout.LEFT, 0,0))
        refreshButton.setBorderPainted(false)
        refreshButton.setPreferredSize(Dimension(40, 30))
        refreshButton.toolTipText = Language.text("view.freshTips")

        //STEP::Add listener
        refreshButton.addMouseListener (object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                DoExcute_Reflash(project)
            }
        })

        //STEP::Init tips
        var label = JLabel(Language.text("view.freshTips"))

        //STEP::Add tips button
        headMenu.add(refreshButton)
        headMenu.add(label)
        headMenu.add(Box.createHorizontalGlue())

        return headMenu
    }

    fun start(project: Project):JBScrollPane{
        //STEP::Setting menu
        val menuPanel = menuMake(project)

        //STEP::Setting task list
        val taskListPanel = taskListMake(project)

        //STEP::Setting head menu
        val headMenuPanel = headMenu(project)

        //STEP::Insert body panel
        val bodyPanel = JPanel(BorderLayout())
        bodyPanel.add(headMenuPanel, BorderLayout.NORTH)
        bodyPanel.add(taskListPanel, BorderLayout.CENTER)

        //STEP::Setting body panel scroll
        val mainPanel = JBScrollPane(bodyPanel)
        mainPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mainPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        return mainPanel
    }
}