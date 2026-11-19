package com.plugin.unicode

import com.intellij.ide.HelpTooltip
import com.intellij.openapi.project.Project
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import java.awt.Cursor
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import javax.swing.border.LineBorder

class UnicodeCharacterComponent(
    private val unicodeChar: UnicodeChar,
    private val project: Project,
    private val formatProvider: () -> String,
    private val actionProvider: () -> String
) : JBLabel(unicodeChar.char, CENTER) {

    init {
        preferredSize = Dimension(40, 40)
        font = font.deriveFont(20f)
        border = LineBorder(JBColor.border())
        isOpaque = true
        background = JBColor.background()
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        setupTooltip()
        setupActions()
        setupHoverEffect()
    }

    private fun setupTooltip() {
        HelpTooltip()
            .setTitle(unicodeChar.name)
            .setDescription("Code: ${unicodeChar.hex}<br>Dec: ${unicodeChar.codePoint}<br>Block: ${unicodeChar.block}<br>Category: ${unicodeChar.category}")
            .installOn(this)
    }

    private fun setupActions() {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    val format = formatProvider()
                    val action = actionProvider()
                    if (action == "Copy") {
                        copyToClipboard(format)
                    } else {
                        insertIntoEditor(format)
                    }
                }
            }

            override fun mousePressed(e: MouseEvent) = handlePopup(e)
            override fun mouseReleased(e: MouseEvent) = handlePopup(e)

            private fun handlePopup(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    showPopupMenu(e)
                }
            }
        })
    }

    private fun showPopupMenu(e: MouseEvent) {
        val menu = javax.swing.JPopupMenu()
        val formats = listOf("Character", "Hex", "Decimal", "Entity", "Name")

        val copyMenu = javax.swing.JMenu("Copy as")
        for (f in formats) {
            val item = javax.swing.JMenuItem(f)
            item.addActionListener { copyToClipboard(f) }
            copyMenu.add(item)
        }
        menu.add(copyMenu)

        val insertMenu = javax.swing.JMenu("Insert as")
        for (f in formats) {
            val item = javax.swing.JMenuItem(f)
            item.addActionListener { insertIntoEditor(f) }
            insertMenu.add(item)
        }
        menu.add(insertMenu)

        menu.show(e.component, e.x, e.y)
    }

    private fun getText(format: String): String {
        return when (format) {
            "Character" -> unicodeChar.char
            "Hex" -> unicodeChar.hex
            "Decimal" -> unicodeChar.codePoint.toString()
            "Entity" -> "&#${unicodeChar.codePoint};"
            "Name" -> unicodeChar.name
            else -> unicodeChar.char
        }
    }

    private fun copyToClipboard(format: String) {
        CopyPasteManager.getInstance().setContents(StringSelection(getText(format)))
    }

    private fun insertIntoEditor(format: String) {
        val editor = LastFocusedEditorTracker.getInstance(project).getTargetEditor() ?: return
        val text = getText(format)
        val document = editor.document
        val caretModel = editor.caretModel

        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
            val selectionModel = editor.selectionModel
            if (selectionModel.hasSelection()) {
                val start = selectionModel.selectionStart
                document.replaceString(start, selectionModel.selectionEnd, text)
                selectionModel.removeSelection()
                caretModel.moveToOffset(start + text.length)
            } else {
                val offset = caretModel.offset
                document.insertString(offset, text)
                caretModel.moveToOffset(offset + text.length)
            }
        }

        // Give focus back to the editor (also brings a detached editor window to the front)
        com.intellij.openapi.wm.IdeFocusManager.getInstance(project).requestFocus(editor.contentComponent, true)
    }

    private fun setupHoverEffect() {
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                background = JBColor.namedColor("List.selectionBackground", JBColor.LIGHT_GRAY)
            }

            override fun mouseExited(e: MouseEvent) {
                background = JBColor.background()
            }
        })
    }
}
