package com.dengzii.plugin.rbk

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference

/**
 *
 * @author https://github.com/dengzii
 */
class BindInfo(
    val viewClass: String,
    val idResExpr: String,
    var filedName: String = genMappingField(idResExpr),
    var type: BindType,
    // 是事件绑定
    var isEventBind: Boolean = false,
    var enable: Boolean = true,
    var optional: Boolean = false,
    var bindAnnotation: PsiAnnotation? = null,
    var bindMethod: PsiMethod? = null
) {

    var refactorSuccess: Boolean = false
    var bindView: PsiReference? = null

    companion object {
        fun genMappingField(idResExpr: String): String {
            val builder = StringBuilder(Config.fieldNamePrefix)
            val id = idResExpr.substring(idResExpr.lastIndexOf((".")) + 1, idResExpr.length)
            if (id.contains("_")) {
                val split = id.lowercase().split("_".toRegex()).toTypedArray()
                for (s in split) {
                    if (s.isNotEmpty()) {
                        val c = s.substring(0, 1).uppercase()
                        builder.append(c).append(s.substring(1))
                    }
                }
            } else {
                val c = id.substring(0, 1).uppercase()
                builder.append(c).append(id.substring(1))
            }
            return builder.toString()
        }
    }

    override fun toString(): String {
        return "BindInfo(viewClass='$viewClass', idResExpr='$idResExpr', filedName='$filedName', type=$type, isEventBind=$isEventBind, enable=$enable, optional=$optional, bindAnnotation=$bindAnnotation, bindMethod=$bindMethod, refactorSuccess=$refactorSuccess, bindView=$bindView)"
    }


}