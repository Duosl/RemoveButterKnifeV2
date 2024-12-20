package com.dengzii.plugin.rbk.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.awt.ComposePanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

class SimpleDialog(project: Project): DialogWrapper(project) {

    init {
        title = "Remove ButterKnife V2"
        super.init()
    }

    override fun createCenterPanel(): JComponent {
        return ComposePanel().apply {
            setBounds(0, 0, 800, 500)
            setContent {
                MaterialTheme {
                    Entrance()
                }
            }
        }
    }
}