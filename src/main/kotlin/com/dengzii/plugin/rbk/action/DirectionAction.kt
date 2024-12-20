package com.dengzii.plugin.rbk.action

import com.dengzii.plugin.rbk.Config
import com.dengzii.plugin.rbk.ui.MainDialog
import com.dengzii.plugin.rbk.utils.ButterKnifeUtils
import com.dengzii.plugin.rbk.utils.valid
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class DirectionAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        Config.PsiTypes.init(project)
        MainDialog.show_(object : MainDialog.Callback {
            override fun ok() {
                ButterKnifeUtils.runRemoveButterKnifeTask(project, virtualFile)
            }
        })
    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

        if (e.project == null || virtualFile == null) {
            e.presentation.isEnabled = false
            return
        }
        virtualFile.let {
            if (!it.valid() || !virtualFile.isDirectory || virtualFile.children.isEmpty()) {
                e.presentation.isEnabled = false
                return
            }
        }
        e.presentation.isEnabledAndVisible = true
    }

}