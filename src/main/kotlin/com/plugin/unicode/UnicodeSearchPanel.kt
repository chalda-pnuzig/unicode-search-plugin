package com.plugin.unicode

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.icons.AllIcons
import javax.swing.JPanel
import javax.swing.JButton
import javax.swing.SwingUtilities

class UnicodeSearchPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val searchField = SearchTextField()
    private val copyFormatCombo = ComboBox(arrayOf("Character", "Hex", "Decimal", "Entity", "Name"))
    private val clickActionCombo = ComboBox(arrayOf("Copy", "Insert"))
    private val pageSizeCombo = ComboBox(arrayOf(100, 500, 1000))
    private val resultsPanel = object : JPanel(WrapLayout(FlowLayout.LEFT, 5, 5)), javax.swing.Scrollable {
        override fun getScrollableTracksViewportWidth(): Boolean = true
        override fun getScrollableTracksViewportHeight(): Boolean = false
        override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
        override fun getScrollableUnitIncrement(visibleRect: java.awt.Rectangle?, orientation: Int, direction: Int): Int = 10
        override fun getScrollableBlockIncrement(visibleRect: java.awt.Rectangle?, orientation: Int, direction: Int): Int = 100
    }
    private val loadMoreButton = javax.swing.JButton("Load More")
    private val loadMorePanel = JPanel(FlowLayout(FlowLayout.CENTER))
    private val mainResultsContainer = JPanel(BorderLayout())
    private val scrollPane = JBScrollPane(mainResultsContainer)
    private val settingsButton = JButton(AllIcons.General.GearPlain)

    private var currentOffset = 0

    init {
        setupUI()
        loadResults(reset = true)
    }

    private fun setupUI() {
        val topPanel = JPanel(BorderLayout())
        
        val searchPanel = JPanel(BorderLayout())
        searchPanel.add(searchField, BorderLayout.CENTER)
        searchPanel.add(settingsButton, BorderLayout.EAST)
        
        topPanel.add(searchPanel, BorderLayout.CENTER)
        
        mainResultsContainer.add(resultsPanel, BorderLayout.CENTER)
        loadMorePanel.add(loadMoreButton)
        mainResultsContainer.add(loadMorePanel, BorderLayout.SOUTH)
        loadMorePanel.isVisible = false

        add(topPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)

        searchField.textEditor.document.addDocumentListener(object : com.intellij.ui.DocumentAdapter() {
            override fun textChanged(e: javax.swing.event.DocumentEvent) {
                loadResults(reset = true)
            }
        })

        settingsButton.addActionListener {
            showSettingsPopup()
        }

        pageSizeCombo.addActionListener {
            loadResults(reset = true)
        }

        loadMoreButton.addActionListener {
            val limit = pageSizeCombo.selectedItem as Int
            currentOffset += limit
            loadResults(reset = false)
        }

        settingsButton.border = JBUI.Borders.empty(0, 5)
        settingsButton.isContentAreaFilled = false
        settingsButton.isBorderPainted = false
        settingsButton.focusTraversalKeysEnabled = false

        resultsPanel.border = JBUI.Borders.empty(5)
    }

    private fun showSettingsPopup() {
        val panel = JPanel(java.awt.GridBagLayout())
        panel.border = JBUI.Borders.empty(10)
        val gbc = java.awt.GridBagConstraints()
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL
        gbc.insets = JBUI.insets(5)

        gbc.gridx = 0; gbc.gridy = 0
        panel.add(com.intellij.ui.components.JBLabel("On click:"), gbc)
        gbc.gridx = 1
        panel.add(clickActionCombo, gbc)

        gbc.gridx = 0; gbc.gridy = 1
        panel.add(com.intellij.ui.components.JBLabel("Limit:"), gbc)
        gbc.gridx = 1
        panel.add(pageSizeCombo, gbc)

        gbc.gridx = 0; gbc.gridy = 2
        panel.add(com.intellij.ui.components.JBLabel("Copy as:"), gbc)
        gbc.gridx = 1
        panel.add(copyFormatCombo, gbc)

        JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, null)
            .setFocusable(true)
            .setRequestFocus(true)
            .setTitle("Settings")
            .setMovable(true)
            .createPopup()
            .show(RelativePoint.getSouthOf(settingsButton))
    }

    private fun loadResults(reset: Boolean) {
        if (reset) {
            currentOffset = 0
            resultsPanel.removeAll()
        }

        SwingUtilities.invokeLater {
            val limit = pageSizeCombo.selectedItem as Int
            val searchResult = UnicodeDataProvider.search(searchField.text, limit, currentOffset)
            
            for (unicodeChar in searchResult.results) {
                resultsPanel.add(UnicodeCharacterComponent(
                    unicodeChar, 
                    project,
                    { copyFormatCombo.selectedItem as String },
                    { clickActionCombo.selectedItem as String }
                ))
            }
            
            loadMorePanel.isVisible = searchResult.hasMore
            
            resultsPanel.revalidate()
            resultsPanel.repaint()
        }
    }
}

/**
 * A FlowLayout that calculates height based on width, allowing for proper wrapping in a JScrollPane.
 */
class WrapLayout(align: Int, hgap: Int, vgap: Int) : FlowLayout(align, hgap, vgap) {
    override fun preferredLayoutSize(target: java.awt.Container): Dimension = layoutSize(target, true)
    override fun minimumLayoutSize(target: java.awt.Container): Dimension = layoutSize(target, false).apply { width -= hgap + 1 }

    private fun layoutSize(target: java.awt.Container, preferred: Boolean): Dimension {
        synchronized(target.treeLock) {
            val targetWidth = target.width.takeIf { it > 0 } ?: super.preferredLayoutSize(target).width
            val insets = target.insets
            val maxWidth = targetWidth - (insets.left + insets.right + hgap * 2)

            val dim = Dimension(0, 0)
            var rowWidth = 0
            var rowHeight = 0

            for (i in 0 until target.componentCount) {
                val m = target.getComponent(i)
                if (m.isVisible) {
                    val d = if (preferred) m.preferredSize else m.minimumSize
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        dim.width = Math.max(dim.width, rowWidth)
                        dim.height += rowHeight + vgap
                        rowWidth = 0
                        rowHeight = 0
                    }
                    if (rowWidth != 0) rowWidth += hgap
                    rowWidth += d.width
                    rowHeight = Math.max(rowHeight, d.height)
                }
            }
            dim.width = Math.max(dim.width, rowWidth)
            dim.height += rowHeight + insets.top + insets.bottom + vgap * 2
            return dim
        }
    }
}
