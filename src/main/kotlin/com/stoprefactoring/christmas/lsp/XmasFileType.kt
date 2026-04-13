package com.stoprefactoring.christmas.lsp

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon
import com.intellij.lang.Language
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor

object XmasLanguage : Language("christmas")

object XmasFileType : LanguageFileType(XmasLanguage) {
    private val ICON_LIGHT = IconLoader.getIcon("/icon/file_light.svg", javaClass)
    private val ICON_DARK = IconLoader.getIcon("/icon/file_dark.svg", javaClass)
    override fun getName() = "christmas"
    override fun getDescription() = ".xmas file of Christmas"
    override fun getDefaultExtension() = "xmas"
    override fun getIcon(): Icon {
        if(!JBColor.isBright()){
            return ICON_DARK
        }
        return ICON_LIGHT
    }
}
