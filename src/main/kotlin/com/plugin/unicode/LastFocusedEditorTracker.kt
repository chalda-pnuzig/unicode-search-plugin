package com.plugin.unicode

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEventMulticasterEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.lang.ref.WeakReference

/**
 * Remembers the last code editor that had keyboard focus in this project,
 * including editors in split panes and in detached (popped-out) windows.
 *
 * FileEditorManager.selectedTextEditor alone is not enough: once the user
 * clicks in the tool window it can point to the main window's first split
 * instead of the editor the user was actually typing in.
 */
@Service(Service.Level.PROJECT)
class LastFocusedEditorTracker(private val project: Project) : Disposable {

    @Volatile
    private var lastEditor: WeakReference<Editor>? = null

    init {
        val multicaster = EditorFactory.getInstance().eventMulticaster
        if (multicaster is EditorEventMulticasterEx) {
            multicaster.addFocusChangeListener(object : FocusChangeListener {
                override fun focusGained(editor: Editor) {
                    if (editor.project == project && editor.editorKind == EditorKind.MAIN_EDITOR) {
                        lastEditor = WeakReference(editor)
                    }
                }

                override fun focusLost(editor: Editor) {}
            }, this)
        }
    }

    /** The editor to insert into: the last focused one if still open, otherwise the selected one. */
    fun getTargetEditor(): Editor? {
        val editor = lastEditor?.get()
        if (editor != null && !editor.isDisposed) {
            return editor
        }
        return FileEditorManager.getInstance(project).selectedTextEditor
    }

    override fun dispose() {
        lastEditor = null
    }

    companion object {
        fun getInstance(project: Project): LastFocusedEditorTracker = project.service()
    }
}
