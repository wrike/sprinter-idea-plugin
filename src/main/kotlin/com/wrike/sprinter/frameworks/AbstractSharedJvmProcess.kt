package com.wrike.sprinter.frameworks

import com.intellij.execution.Executor
import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.testframework.JavaAwareTestConsoleProperties
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.TestProxyRoot
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.layout.LayoutViewOptions
import com.intellij.execution.ui.layout.ViewContext
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.impl.ContentImpl
import com.intellij.util.ui.UIUtil
import java.net.ServerSocket

abstract class AbstractSharedJvmProcess(
    protected val process: OSProcessHandler,
    protected val serverSocket: ServerSocket,
    protected val executor: Executor
) : Disposable {
    private var previousConsoleView: Content? = null

    init {
        process.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                serverSocket.close()
            }
        })
    }

    abstract fun supportsConfiguration(configuration: JavaTestConfigurationBase): Boolean

    fun executeConfiguration(configuration: JavaTestConfigurationBase, consoleAttacher: ContentManager) {
        val fakeProcessHandler = FakelyTerminatedProcessHandler(process)
        val console = createTestConsole(configuration, fakeProcessHandler, consoleAttacher)
        fakeProcessHandler.startNotify()
        runTests(configuration)
        ApplicationManager.getApplication().invokeLater {
            console.performWhenNoDeferredOutput {
                fakeProcessHandler.destroyProcess()
            }
        }
    }

    protected abstract fun runTests(configuration: JavaTestConfigurationBase)

    protected open fun createTestConsole(
        configuration: JavaTestConfigurationBase,
        processHandler: ProcessHandler,
        consoleAttacher: ContentManager
    ): ConsoleView {
        val consoleProperties = createConsoleProperties(configuration, executor)
        consoleProperties.setIfUndefined(TestConsoleProperties.HIDE_PASSED_TESTS, false)
        val testConsole = UIUtil.invokeAndWaitIfNeeded<SMTRunnerConsoleView> {
            SMTestRunnerConnectionUtil.createConsole(consoleProperties)
        }
        val resultsViewer = testConsole.resultsViewer
        val consoleView = JavaRunConfigurationExtensionManager.instance.decorateExecutionConsole(
            configuration,
            null,
            testConsole,
            executor
        )
        Disposer.register(this, consoleView)

        consoleView.attachToProcess(processHandler)
        val root = resultsViewer.root
        if (root is TestProxyRoot) {
            root.setHandler(processHandler)
        }
        val rerunFailedTestsAction = consoleProperties.createRerunFailedTestsAction(testConsole)!!
        rerunFailedTestsAction.setModelProvider { resultsViewer }

        UIUtil.invokeLaterIfNeeded {
            val newConsoleTab = ContentImpl(consoleView.component, configuration.actionName ?: "", true)
            customizeConsoleTab(newConsoleTab)
            newConsoleTab.putUserData(ToolWindow.SHOW_CONTENT_ICON, java.lang.Boolean.TRUE)
            previousConsoleView?.let { consoleAttacher.removeContent(it, true) }
            consoleAttacher.addContent(newConsoleTab)
            consoleAttacher.setSelectedContent(newConsoleTab, true)
            makeFocusOnProcessStartup(newConsoleTab)
            previousConsoleView = newConsoleTab
        }
        return testConsole
    }

    private fun makeFocusOnProcessStartup(newConsoleTab: ContentImpl) {
        val viewContext = ViewContext.CONTEXT_KEY.getData(DataManager.getInstance().getDataContext(newConsoleTab.component))
        viewContext?.runnerLayoutUi?.options?.setToFocus(newConsoleTab, LayoutViewOptions.STARTUP)
    }

    protected abstract fun createConsoleProperties(
        configuration: JavaTestConfigurationBase,
        executor: Executor
    ): JavaAwareTestConsoleProperties<out JavaTestConfigurationBase>

    protected abstract fun customizeConsoleTab(consoleTab: Content)

    override fun dispose() {
        process.destroyProcess()
    }
}