package com.dengzii.plugin.rbk.gen

import com.dengzii.plugin.rbk.BindInfo
import com.dengzii.plugin.rbk.BindType
import com.dengzii.plugin.rbk.Config
import com.dengzii.plugin.rbk.Constants
import com.dengzii.plugin.rbk.utils.*
import com.intellij.lang.Language
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.CodeFormatterFacade
import com.intellij.psi.impl.source.tree.java.PsiIdentifierImpl

/**
 *
 * @author https://github.com/dengzii
 */
class JavaCase : BaseCase() {

    private lateinit var factory: PsiElementFactory
    private lateinit var psiClass: PsiClass

    override fun dispose(psiClass: PsiClass, bindInfos: List<BindInfo>) {
        if (psiClass.language != Constants.langJava) {
            Logger.error("[${psiClass.name}]: Only support java file")
            next(psiClass, bindInfos)
            return
        }
        if (!this::factory.isInitialized) {
            factory = JavaPsiFacade.getElementFactory(psiClass.project)
        }
        if (!this::psiClass.isInitialized) {
            this.psiClass = psiClass
        }
        // generate bind resource id to field method
        val methodBody = insertBindResourceMethod(psiClass).body!!

        // add bind view statement to method body
        for (bindInfo in bindInfos) {
            if (!bindInfo.enable) {
                continue
            }
            if (bindInfo.isEventBind) {
                // 事件绑定不在这里处理
                continue
            }
            insertViewDeclareField(bindInfo, psiClass)
            insertBindResourceStatement(bindInfo, methodBody)
        }

        // 插入 click 事件
        for (bindInfo in bindInfos) {
            if (bindInfo.isEventBind) {
                insertBindEvent(bindInfo, methodBody)
            }
        }

        // 在代码中找到所有的 ButterKnife.bind(xx) 并替换为 bindView()
        findAndReplaceButterKnifeBindStatement(psiClass)
        // 移除 ButterKnife 和 R2 导包
        ButterKnifeUtils.removeButterKnifeImports(psiClass.containingFile)
    }

    /**
     * Insert `setXxxListener` code to bind method.
     * When set listener code is inserted, the event bind annotation is deleted.
     *
     * @param bindInfo the event bind info.
     * @param bindMethodBody the bind method body.
     */
    private fun insertBindEvent(bindInfo: BindInfo, bindMethodBody: PsiCodeBlock): Boolean {

        val eventMethodParams = bindInfo.bindMethod?.parameterList?.parameters
        if (eventMethodParams != null && eventMethodParams.size > 1) {
            return false
        }
        val type = eventMethodParams?.getOrNull(0)?.type?.let {
            if (it == Config.PsiTypes.androidView) "" else "(${it.canonicalText})"
        }
        val castParam = if (type == null) "" else "${type}v"
        val bindViewStatement = "bindSource.findViewById(${bindInfo.idResExpr.replace("R2.id", "R.id")})"
        val psiStatement = when (bindInfo.type) {
            BindType.OnClick -> {
                val statement = """
                    ${bindViewStatement}.setOnClickListener(v -> ${bindInfo.bindMethod!!.name}(${castParam}));
                """.trimIndent()
                factory.createStatementFromText(statement, null)
            }

            BindType.OnLongClick -> {
                val statement = """
                    ${bindViewStatement}.setOnLongClickListener(v -> {${bindInfo.bindMethod!!.name}(${castParam});});
                """.trimIndent()
                factory.createStatementFromText(statement, null)
            }

            else -> {
                // TODO add more event listener support.
                Logger.info("bindInfo type: ${bindInfo.type} not supported yet.\n " + bindInfo.toString())
                NotificationUtils.showError("[${psiClass.name}]: ${bindInfo.type} not supported yet. Please handle it manually! ")
                null
            }
        }
        psiStatement ?: return false
        bindMethodBody.addLast(psiStatement)
        bindInfo.bindAnnotation?.delete()
        bindInfo.bindMethod?.modifierList?.setModifierProperty(PsiModifier.PRIVATE, true)
        return true
    }

    /**
     * Search ButterKnife bind statement in each class method.
     *
     * @param psiClass the class.
     */
    private fun findAndReplaceButterKnifeBindStatement(psiClass: PsiClass) {
        var replace = false
        out@ for (method in psiClass.allMethods) {
            if (!Config.priorityReplaceButterKnifeBind) {
                // 如果未开启全局搜索（当前文件内），则只搜索 insertBindViewMethodIntoMethod 定义的几个特定方法
                if (method.name !in Config.insertBindViewMethodIntoMethod) {
                    continue
                }
            }
            val elements = method.body?.children ?: continue
            for (element in elements) {
                if (element !is PsiExpressionStatement) continue
                replace = findAndReplaceButterKnifeBindStatementImpl(element, psiClass)
                if (replace) {
                    Logger.info("findAndReplaceButterKnifeBindStatement: success.")
                    break@out
                }
            }
        }
        if (!replace) {
            NotificationUtils.showWarning("Can't find ButterKnife.bind() in class ${psiClass.name}")
        }
    }

    private fun findAndReplaceButterKnifeBindStatementImpl(element: PsiExpressionStatement, psiClass: PsiClass): Boolean {
        val elementExpression = element.expression
        if (elementExpression is PsiMethodCallExpression) {
            return replaceButterKnifeBind(element, elementExpression)
        } else if (elementExpression is PsiAssignmentExpression && elementExpression.rExpression is PsiMethodCallExpression) {
            var replace = false
            // 这段代码有先后顺序， 不然先删除了 unbind = ButterKnife.bind(); 就不能执行第一步的替换了
            // 1. 先执行 unbind = ButterKnife.bind(); => bindView(xxx); 的转换
            val lExpression = elementExpression.lExpression
            val rExpression = elementExpression.rExpression
            if (rExpression is PsiMethodCallExpression) {
                replace = replaceButterKnifeBind(element, rExpression)
            }
            // 2. 移除 unbind 的所有引用，再删除 unBind 的声明
            if (replace && lExpression is PsiReferenceExpression) {
                lExpression.deleteVariableDefined(psiClass)
            }
            return replace
        }
        return false
    }

    /**
     * 判断 ${expression} 是是否 ButterKnife.bind(this)
     * 如果是，则整行替换为 bindView()
     *
     * @param parentElement 父元素，即 expression 所在行
     * @param expression 待判断的表达式
     * @return 替换成功返回 true, 否则返回 false
     */
    private fun replaceButterKnifeBind(
        parentElement: PsiExpressionStatement,
        expression: PsiMethodCallExpression
    ): Boolean {
        if (ButterKnifeUtils.isButterKnifeBindMethod(expression)) {
            val text = parentElement.text
            val parentMethodName = parentElement.getParentMethodName()
            val viewParamName = if (psiClass.isExtendsFrom(Config.PsiTypes.androidActivity)) {
                "getWindow().getDecorView()"
            } else {
                ButterKnifeUtils.getBindMethodViewParamName(expression)
            }
            parentElement.replace(factory.createStatementFromText("bindView(${viewParamName});", null))
            Logger.info("replaceButterKnifeBind: success. \"$text\" in method $parentMethodName()." )
            return true
        }
        return false
    }

    /**
     * Insert view field declaration.
     * Its insert only when the field name does not exist.
     *
     * @param bindInfo the bind info.
     * @param psiClass the PsiClass.
     */
    private fun insertViewDeclareField(bindInfo: BindInfo, psiClass: PsiClass): PsiField {
        var psiField = psiClass.findFieldByName(bindInfo.filedName, false)
        if (psiField != null) {
            if (Config.addPrivateModifier) {
                psiField.modifierList?.setModifierProperty(PsiModifier.PRIVATE, true)
            }
        }

        if (psiField == null) {
            psiField = factory.createFieldFromText("", null)
        }
        // remove line break
        psiField.acceptChildren(object : PsiElementVisitor() {
            override fun visitWhiteSpace(space: PsiWhiteSpace) {
                super.visitWhiteSpace(space)
                if (space.text.contains("\n")) {
                    space.delete()
                }
            }
        })
        return psiField
    }

    /**
     * 生成 bindView(View bindSource) 方法签名
     */
    private fun insertBindResourceMethod(psiClass: PsiClass): PsiMethod {
        var ret: PsiMethod? = psiClass.findMethodsByName(Config.methodNameBindView, false).firstOrNull()
        if (ret == null) {
            ret = factory.createMethod(Config.methodNameBindView, PsiTypes.voidType())
            ret.modifierList.setModifierProperty(PsiModifier.PRIVATE, true)
            ret = psiClass.add(ret) as PsiMethod
        }
        val paramBindView = ret.parameterList.parameters.filter {
            (it.nameIdentifier as PsiIdentifierImpl).text == paramNameBindSourceView
        }
        if (paramBindView.isEmpty()) {
            ret.parameterList.add(factory.createParameter(paramNameBindSourceView, Config.PsiTypes.androidView))
        }
        return ret
    }

    /**
     * bindView 方法体
     * view = bindSource.findViewById(R.id.xxx);
     */
    private fun insertBindResourceStatement(bindInfo: BindInfo, bindMethodBody: PsiCodeBlock) {
        val statementTemplate = Config.resBindStatement.getOrElse(bindInfo.type) { "" }
        if (statementTemplate.isBlank()) {
            //throw IllegalStateException("Unable to bind resource to field: unknown resource type.")
            return
        }
        val resourceExpression = statementTemplate
            .replace("%{SOURCE}", paramNameBindSourceView)
            .replace("%{RES_ID}", bindInfo.idResExpr.replace("R2.id", "R.id"))
            .replace("%{THEME}", "${paramNameBindSourceView}.getContext().getTheme()")

        val bindStatement = "%s = %s;".format(bindInfo.filedName, resourceExpression)
        if (bindMethodBody.text.contains(bindStatement)) {
            println("bind statement already exist.")
            return
        }
        val bindPsiStatement = factory.createStatementFromText(bindStatement, null)
        bindMethodBody.addLast(bindPsiStatement)
        bindInfo.bindAnnotation?.delete()
    }

    companion object {
        private const val paramNameBindSourceView = "bindSource"
        private val codeFormatter = CodeFormatterFacade(
            CodeStyleSettings.getDefaults(),
            Language.findLanguageByID("JAVA"), false
        )
    }
}