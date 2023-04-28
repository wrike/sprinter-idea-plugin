package com.wrike.sprinter.frameworks

import com.intellij.execution.Executor
import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.testframework.JavaAwareTestConsoleProperties
import com.intellij.execution.testframework.SearchForTestsTask
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.TestProxyRoot
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
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
): Disposable {
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
        createTestConsole(configuration, fakeProcessHandler, consoleAttacher)
        fakeProcessHandler.startNotify()
        val searchTask = createTestSearchTask(configuration)
        val progressIndicator = BackgroundableProcessIndicator(searchTask)
        ProgressManager.getInstance().runProcess({ searchTask.run(progressIndicator) }, progressIndicator)
        searchTask.finish()
        fakeProcessHandler.destroyProcess()
    }

    protected abstract fun createTestSearchTask(configuration: JavaTestConfigurationBase): SearchForTestsTask

    protected open fun createTestConsole(
        configuration: JavaTestConfigurationBase,
        processHandler: ProcessHandler,
        consoleAttacher: ContentManager
    ) {
        val consoleProperties = createConsoleProperties(configuration, executor)
        consoleProperties.setIfUndefined(TestConsoleProperties.HIDE_PASSED_TESTS, false)
        consoleProperties.isIdBasedTestTree = false
        val testConsole = UIUtil.invokeAndWaitIfNeeded<BaseTestsOutputConsoleView> {
            SMTestRunnerConnectionUtil.createConsole(consoleProperties)
        }
        val resultsViewer = (testConsole as SMTRunnerConsoleView).resultsViewer
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
            previousConsoleView = newConsoleTab
        }
    }

    protected abstract fun createConsoleProperties(configuration: JavaTestConfigurationBase, executor: Executor): JavaAwareTestConsoleProperties<out JavaTestConfigurationBase>
    protected abstract fun customizeConsoleTab(consoleTab: Content)

    override fun dispose() {
        process.destroyProcess()
    }
}