package com.dengzii.plugin.rbk.utils

import com.dengzii.plugin.rbk.BindInfo
import com.dengzii.plugin.rbk.BindType
import com.dengzii.plugin.rbk.Constants
import com.dengzii.plugin.rbk.gen.CodeWriter
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiArrayInitializerMemberValueImpl
import com.intellij.psi.impl.source.tree.java.PsiIdentifierImpl
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl
import org.jetbrains.kotlin.idea.KotlinFileType

object ButterKnifeUtils {

    fun getButterKnifeViewBindInfo(psiClass: PsiClass): List<BindInfo> {
        val ret = mutableListOf<BindInfo>()

        val fields = psiClass.fields.filter {
            it.modifierList.let { modifier ->
                modifier != null && !modifier.hasModifierProperty(PsiModifier.PRIVATE)
                        && !modifier.hasModifierProperty(PsiModifier.FINAL)
            }
        }
        for (field in fields) {
            var optional = false
            // each non-private fields, find fields annotated with `BindXxx`
            for (annotation in field.annotations) {
                val annotationTypeName = annotation.qualifiedName
                if (annotationTypeName == Constants.ButterKnifeOptional) {
                    optional = true
                    continue
                }
                if (annotationTypeName !in Constants.ButterKnifeBindFieldAnnotation) {
                    continue
                }
                val parameter = annotation.parameterList.attributes
                if (parameter.size != 1) {
                    continue
                }
                val viewIdExpr = (parameter[0].detachedValue as? PsiReferenceExpressionImpl)?.element
                if (viewIdExpr == null) {
                    System.err.println("$parameter is null")
                    continue
                }
                val info = BindInfo(
                    viewClass = field.type.canonicalText,
                    idResExpr = viewIdExpr.text,
                    filedName = (field.nameIdentifier as PsiIdentifierImpl).text,
                    bindAnnotation = annotation,
                    type = BindType.get(annotation)
                )
                info.optional = optional
                ret.add(info)
                break
            }
        }

        // method
        val methods = psiClass.methods.filter {
            it.modifierList.let { modifier ->
                !modifier.hasModifierProperty(PsiModifier.PRIVATE)
            }
        }
        for (method in methods) {
            for (annotation in method.annotations) {
                val annotationTypeName = annotation.qualifiedName
                if (annotationTypeName !in Constants.ButterKnifeBindMethodAnnotation) {
                    continue
                }
                val annotationParams = annotation.parameterList.attributes
                for (param in annotationParams) {
                    val value = param.detachedValue ?: continue
                    val viewIdExprs = when (value) {
                        is PsiArrayInitializerMemberValueImpl -> value.initializers.map { it.text }
                        is PsiReferenceExpressionImpl -> listOf(value.element.text)
                        else -> listOf()
                    }
                    viewIdExprs.forEach {
                        val info = BindInfo(
                            viewClass = "android.view.View",
                            idResExpr = it,
                            bindAnnotation = annotation,
                            type = BindType.get(annotation),
                            isEventBind = true,
                            bindMethod = method
                        )
                        ret.add(info)
                    }
                }
            }
        }
        return ret
    }

    fun isImportedButterKnife(psiFile: PsiFile): Boolean {
        var ret = false
        psiFile.acceptElement {
            if (it is PsiImportList && !ret) {
                ret = it.allImportStatements.filter { i ->
                    i.text.contains("butterknife.")
                }.isNotEmpty()
            }
        }
        return ret
    }

    fun removeButterKnifeImports(psiFile: PsiFile) {
        Logger.info("removeButterKnifeImports start ...")
        (psiFile as PsiJavaFile).importList?.importStatements
            ?.filter { it.text.contains("butterknife.") || it.text.endsWith(".R2;") }
            ?.map {
                Logger.info("removeButterKnifeImports: ${it.text}")
                it.delete()
            }
        // psiClass.acceptElement { element ->
        //     Logger.i("removeButterKnifeImports: ${element.text}")
        //     if (element is PsiImportList) {
        //         element.allImportStatements
        //             // .filter { it.importReference?.canonicalText?.contains("butterknife.") ?: false }
        //             .map {
        //                 Logger.i("removeButterKnifeImports: ${element.text}")
        //                 it.delete()
        //             }
        //     }
        // }
    }

    fun runRemoveButterKnifeTask(project: Project, psiFile: PsiFile) {
        val task = object : Task.Backgroundable(project, "RemoveButterKnife") {
            override fun run(indicator: ProgressIndicator) {
                if (!isImportedButterKnife(psiFile)) {
                    Logger.warn("[${psiFile.name}]: The current file does not use the ButterKnife.")
                    NotificationUtils.showWarning("[${psiFile.name}]: The current file does not use the ButterKnife.", "Remove ButterKnife")
                    return
                }
                doRemoveButterKnife(psiFile)
                indicator.text = psiFile.name
                NotificationUtils.showInfo("Refactor complete, one files are affected", "Remove ButterKnife")
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    fun runRemoveButterKnifeTask(project: Project, virtualFile: VirtualFile) {
        val task = object : Task.Backgroundable(project, "RemoveButterKnife") {
            override fun run(indicator: ProgressIndicator) {

                indicator.text = "Find all Java file..."
                val allFile = mutableListOf<VirtualFile>()
                virtualFile.directions(allFile)
                val psiFiles = allFile.filter {
                    it.fileType is JavaFileType || it.fileType is KotlinFileType
                }.mapNotNull {
                    PsiManager.getInstance(project).findFile(it)
                }
                Logger.info("Find ${psiFiles.size} java files")

                val errorList = mutableListOf<String>()
                val allFileCount = psiFiles.size
                var successCount = 0
                var skinCount = 0
                psiFiles.forEachIndexed { index, psiFile ->
                    try {
                        if (!isImportedButterKnife(psiFile)) {
                            Logger.warn("[${psiFile.name}]: The current file does not use the ButterKnife.")
                            skinCount++
                        }
                        doRemoveButterKnife(psiFile)
                        successCount++
                    } catch (e: Throwable) {
                        Logger.error("Remove ButterKnife failed, file: ${psiFile.name}, error: ${e.message}")
                        errorList.add(psiFile.name)
                    }
                    indicator.fraction = (index + 1 / allFileCount).toDouble()
                    indicator.text = "[${index + 1}/${allFileCount}] ${psiFile.name}"
                }
                NotificationUtils.showInfo(
                    "Refactor complete, $allFileCount files are affected. success: ${successCount}, skin: ${skinCount}, error: ${errorList.size}.",
                    "Remove ButterKnife"
                )
                if (errorList.size > 0) {
                    NotificationUtils.showError("Fails files: \n ${errorList.joinToString("\n")}")
                }
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    /**
     * 移除 psiFile 中 ButterKnife 相关代码
     */
    private fun doRemoveButterKnife(psiFile: PsiFile) {
        val psiClass = psiFile.getDeclaredClass().firstOrNull() ?: return

        val bindInfos = getButterKnifeViewBindInfo(psiClass)
        if (bindInfos.isEmpty()) {
            Logger.warn("[${psiFile.name}]: No usage butterknife.bindXXX found in current file.")
            return
        }
        Logger.info("[${psiFile.name}]: All bind count: ${bindInfos.size}, event bind count: ${bindInfos.filter { it.isEventBind }.size}")
        CodeWriter.run(psiClass, bindInfos)
    }

    fun isButterKnifeBindMethod(expression: PsiMethodCallExpression?) : Boolean {
        if (expression == null) return false

        // 检查方法名是否为bind，并且是否是ButterKnife类的静态方法
        val method: PsiMethod? = expression.resolveMethod()
        return method != null
                && "bind" == method.name
                && Constants.CLASS_BUTTTER_KNIFE == method.containingClass?.qualifiedName
    }

    fun getBindMethodViewParamName(expression: PsiMethodCallExpression): String {
        expression.argumentList.expressions.forEach {
            val type = it.getParameterType()
            if (type == Constants.AndroidView) {
                return it.getParameterName() ?: "rootView"
            }
        }
        return "rootView"
    }
}