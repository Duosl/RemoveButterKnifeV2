package com.dengzii.plugin.rbk.utils

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*

fun VirtualFile.directions(list: MutableList<VirtualFile>, ) {
    if (!valid()) return

    if (isDirectory) {
        children.forEach { it.directions(list) }
    } else {
        list.add(this)
    }
}
fun VirtualFile.valid(): Boolean {
    return isValid && isWritable && isInLocalFileSystem && exists()
}

fun PsiFile.getDeclaredClass(): List<PsiClass> {
    val ret = mutableListOf<PsiClass>()
    acceptElement {
        if (it is PsiClass) {
            ret.add(it)
        }
    }
    return ret
}

fun PsiClass.getInnerClass(): List<PsiClass> {
    val ret = mutableListOf<PsiClass>()
    acceptElement {
        if (it is PsiClass) {
            ret.add(it)
            ret.addAll(it.getInnerClass())
        }
    }
    return ret
}

fun PsiClass.isExtendsFrom(qualifiedClassName: String): Boolean {
    var superClass = this.superClass
    var ret = false
    while (superClass != null && !ret) {
        ret = superClass.qualifiedName == qualifiedClassName
        superClass = superClass.superClass
    }
    return ret
}

fun PsiClass.isExtendsFrom(type: PsiType): Boolean {
    var superClass = this.superClass
    var ret = false
    while (superClass != null && !ret) {
        ret = superClass.qualifiedName == type.canonicalText
        superClass = superClass.superClass
    }
    return ret
}

fun PsiCodeBlock.addLast(element: PsiElement) {
    addAfter(element, lastBodyElement)
}

fun PsiCodeBlock.addFirst(element: PsiElement) {
    addBefore(element, firstBodyElement)
}


inline fun PsiElement.acceptElement(crossinline visitor: (PsiElement) -> Unit) {
    acceptChildren(object : PsiElementVisitor() {
        override fun visitElement(element: PsiElement) {
            super.visitElement(element)
            visitor.invoke(element)
        }
    })
}

inline fun PsiElement.acceptExpression(crossinline visitor: (PsiElement) -> Unit) {
    acceptChildren(object : PsiElementVisitor() {
        override fun visitElement(element: PsiElement) {
            super.visitElement(element)
            if (element is PsiExpression) {
                visitor.invoke(element)
            }
        }
    })
}

fun PsiCallExpression.getParameterTypes(): Array<PsiParameter> {
    val method = resolveMethod() ?: return arrayOf()
    return method.parameterList.parameters
}

fun PsiCallExpression.getParameterExpressions(): Array<PsiExpression> {
    return argumentList?.expressions ?: arrayOf()
}

fun PsiExpressionStatement.getLineNumber(): Int {
    val project: Project = project
    val document: Document? = PsiDocumentManager.getInstance(project).getDocument(containingFile)
    if (document != null) {
        return document.getLineNumber(textOffset)
    }
    return -1 // 返回-1或其他错误码表示获取行号失败
}

fun PsiExpressionStatement.getParentMethodName(): String {
    var parent: PsiElement? = parent
    while (parent != null) {
        if (parent is PsiMethod) {
            return parent.name
        }
        parent = parent.parent
    }
    return "Not Found"
}

fun PsiExpression.getParameterType(): String? {
    return this.type?.canonicalText
}
fun PsiExpression.getParameterName(): String? {
    if (this is PsiReferenceExpression) {
        val element = resolve()
        if (element is PsiVariable) {
            return element.name
        }
    }
    return null
}

fun PsiReferenceExpression.deleteVariableDefined(psiClass: PsiClass) {
    Logger.info("deleteVariableAndAllUsages: $this")
    psiClass.allFields.filter { it.name == this.referenceName }.getOrNull(0)?.delete()
}

// fun PsiReferenceExpression.deleteVariableAndAllUsages(psiClass: PsiClass) {
//     Logger.info("deleteVariableAndAllUsages: $this")
//     val psiVariableField: PsiVariable = psiClass.allFields.filter { it.name == this.referenceName }[0]
//     val references = psiVariableField.searchReferencesOrMethodReferences()
//     Logger.info("deleteVariableAndAllUsages: psiVariableField: references: size: ${references.size}")
//     references.forEach {
//         var parent = it.element.parent
//         // 找到父节点，直到遇到if (unbind != null)语句
//         while (parent != null && parent !is PsiIfStatement) {
//             parent = parent.parent
//         }
//         if (parent is PsiIfStatement) {
//             Logger.info("deleteVariableAndAllUsages: ${parent.text}")
//             if (parent.text.contains("unbind != null")) {
//                 Logger.info("deleteVariableAndAllUsages: if block: ${parent.text}")
//                 parent.delete()
//             }
//         }
//         // 再查找父节点，直到遇到try语句，并判断 try block 里面是否为空，为空则删除 try block
//         while (parent != null && parent !is PsiTryStatement) {
//             parent = parent.parent
//         }
//         if (parent is PsiTryStatement) {
//             Logger.info("deleteVariableAndAllUsages: try block: ${parent.text}")
//             val children = (parent as PsiTryStatement).tryBlock?.children
//             if (children?.size == 0) {
//                 parent.delete()
//             }
//         }
//
//         it.element.delete()
//     }
//     psiVariableField.delete()
// }