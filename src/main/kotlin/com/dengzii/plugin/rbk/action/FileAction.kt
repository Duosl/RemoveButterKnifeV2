package com.dengzii.plugin.rbk.action

import com.dengzii.plugin.rbk.Config
import com.dengzii.plugin.rbk.Constants
import com.dengzii.plugin.rbk.ui.MainDialog
import com.dengzii.plugin.rbk.ui.SimpleDialog
import com.dengzii.plugin.rbk.utils.ButterKnifeUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

/**
 *
 * @author https://github.com/dengzii
 */
class FileAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {

        val project = e.project
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)

        if (isNull(project, psiFile, editor)) {
            return
        }
        Config.PsiTypes.init(project!!)

        // MainDialog.show_(object : MainDialog.Callback {
        //     override fun ok() {
        //         ButterKnifeUtils.runRemoveButterKnifeTask(project, psiFile!!)
        //     }
        // })
        SimpleDialog(project).show()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = true
        if (isNull(project, psiFile, editor)) {
            e.presentation.isEnabled = false
            return
        }
        val language = psiFile!!.language
        if (!language.`is`(Constants.langJava) && !language.`is`(Constants.langKotlin)) {
            e.presentation.isEnabled = false
        }
    }

    private fun isNull(vararg objects: Any?): Boolean {
        for (o in objects) {
            if (o == null) {
                return true
            }
        }
        return false
    }
}