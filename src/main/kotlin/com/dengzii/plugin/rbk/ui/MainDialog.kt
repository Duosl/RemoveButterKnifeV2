package com.dengzii.plugin.rbk.ui

import com.dengzii.plugin.rbk.Config.insertBindViewMethodIntoMethod
import com.dengzii.plugin.rbk.Config.insertCallBindViewMethodAfterCallMethod
import com.dengzii.plugin.rbk.Config.insertCallBindViewToFirstLine
import com.dengzii.plugin.rbk.Config.methodNameBindView
import com.dengzii.plugin.rbk.Config.priorityReplaceButterKnifeBind
import com.intellij.ui.util.minimumWidth
import com.intellij.ui.util.preferredHeight
import com.intellij.ui.util.preferredWidth
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*

class MainDialog(private val callback: Callback) : JDialog() {
    var insertToMethodTextField: JTextField? = null
    var undoWhenRemoveFailedCheckBox: JCheckBox? = null
    var bindViewMethodNameField: JTextField? = null
    var insertToMethodLabel: JLabel? = null
    var insertAfterTextField: JTextField? = null
    var OKButton: JButton? = null
    var contentPanel: JPanel? = null
    var priorityToSearchAndCheckBox: JCheckBox? = null
    var insertToTheFirstCheckBox: JCheckBox? = null

    init {
        contentPane = contentPanel
        defaultCloseOperation = DISPOSE_ON_CLOSE

        OKButton!!.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                ok()
            }
        })

        priorityToSearchAndCheckBox!!.addItemListener { e ->
            val isSelect = e.stateChange == ItemEvent.SELECTED
            priorityReplaceButterKnifeBind = isSelect

            insertToMethodLabel?.isVisible = !isSelect
            insertToMethodTextField?.isVisible = !isSelect

            contentPanel!!.minimumWidth = if (isSelect) 300 else 650
        }

        contentPanel!!.registerKeyboardAction(
            { _: ActionEvent? -> dispose() },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        )
        contentPanel!!.registerKeyboardAction(
            { _: ActionEvent? -> ok() },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        )
    }

    private fun ok() {
        methodNameBindView = bindViewMethodNameField!!.text
        insertBindViewMethodIntoMethod = getResult(insertToMethodTextField!!)
        insertCallBindViewMethodAfterCallMethod = getResult(insertAfterTextField!!)
        priorityReplaceButterKnifeBind = priorityToSearchAndCheckBox!!.isSelected
        insertCallBindViewToFirstLine = insertToTheFirstCheckBox!!.isSelected
        dispose()
        callback.ok()
    }

    private fun getResult(field: JTextField): AbstractList<String> {
        val s = field.text.replace(" ".toRegex(), "")
        return Arrays.asList(*s.split(",".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()) as AbstractList<String>
    }

    override fun pack() {
        super.pack()
        val screen = Toolkit.getDefaultToolkit().screenSize
        val w = width
        val h = height
        val x = screen.width / 2 - w / 2
        val y = screen.height / 2 - h / 2
        setLocation(x, y)
        preferredSize = Dimension(w, h)

        title = "Remove ButterKnife"
    }

    interface Callback {
        fun ok()
    }

    companion object {
        fun show_(callback: Callback) {
            val p = MainDialog(callback)
            p.pack()
            p.isVisible = true
        }
    }
}
