package burp

import java.awt.Frame
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import kotlin.concurrent.thread

class BurpExtender: IBurpExtender {
    companion object {
        lateinit var callbacks: IBurpExtenderCallbacks
    }
    override fun registerExtenderCallbacks(callbacks: IBurpExtenderCallbacks) {
        Companion.callbacks = callbacks
        callbacks.setExtensionName("Cookie Extractor")
        callbacks.registerContextMenuFactory(ContextMenuFactory())
    }
}

class ContextMenuFactory(): IContextMenuFactory {
    override fun createMenuItems(invocation: IContextMenuInvocation): List<JMenuItem> {
        if (invocation.invocationContext == IContextMenuInvocation.CONTEXT_PROXY_HISTORY) {
            val cookieNames = mutableSetOf<String>()
            invocation.selectedMessages.forEach {
                val responseInfo = BurpExtender.callbacks.helpers.analyzeResponse(it.response ?: return@forEach)
                responseInfo.cookies.forEach { cookieNames.add(it.name) }
            }
            if(cookieNames.isEmpty()) {
                return listOf()
            }

            val menuItem = JMenu("Extract Cookie Values")
            cookieNames.forEach {
                val jMenuItem = JMenuItem(it)
                jMenuItem.addActionListener(MyActionListener(it))
                menuItem.add(jMenuItem)
            }
            return listOf(menuItem)
        }
        return listOf()
    }
}

class MyActionListener(val cookieName: String): ActionListener {
    override fun actionPerformed(e: ActionEvent?) {
        val progressDialog = ProgressDialog(getBurpFrame(), true)
        progressDialog.setLocationRelativeTo(getBurpFrame())
        SwingUtilities.invokeLater {
            progressDialog.isVisible = true
        }
        thread {
            val values = mutableListOf<String>()
            BurpExtender.callbacks.proxyHistory.forEach {
                val responseInfo = BurpExtender.callbacks.helpers.analyzeResponse(it.response ?: return@forEach)
                val cookie = responseInfo.cookies.filter{ it.name == cookieName }.firstOrNull() ?: return@forEach
                values.add(cookie.value)
            }
            SwingUtilities.invokeLater {
                progressDialog.isVisible = false
                val fileChooser = JFileChooser()
                fileChooser.dialogType = JFileChooser.SAVE_DIALOG
                fileChooser.dialogTitle = "Select file to save cookie values into"
                if (fileChooser.showOpenDialog(getBurpFrame()) != JFileChooser.APPROVE_OPTION) {
                    return@invokeLater
                }
                fileChooser.selectedFile.writeText(values.joinToString(System.getProperty("line.separator")))
            }
        }
    }
}

fun getBurpFrame(): JFrame? {
    for (f in Frame.getFrames()) {
        if (f.isVisible && f.title.startsWith("Burp Suite")) {
            return f as JFrame
        }
    }
    return null
}
