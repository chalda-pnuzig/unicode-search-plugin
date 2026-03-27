package com.plugin.unicode

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class UnicodeToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val unicodeSearchPanel = UnicodeSearchPanel(project)
        val content = ContentFactory.getInstance().createContent(unicodeSearchPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
