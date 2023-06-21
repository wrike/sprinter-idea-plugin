package com.wrike.sprinter.frameworks.testng

import com.intellij.execution.Executor
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.testframework.SearchForTestsTask
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.content.Content
import com.theoryinpractice.testng.configuration.TestNGConfiguration
import com.theoryinpractice.testng.model.TestNGConsoleProperties
import com.wrike.sprinter.frameworks.AbstractSharedJvmProcess
import com.wrike.sprinter.frameworks.TestFrameworkForRunningInSharedJVM
import icons.TestngIcons
import java.net.ServerSocket

class TestNGSharedJvmProcess(
    process: OSProcessHandler,
    serverSocket: ServerSocket,
    executor: Executor,
    private val testFramework: TestFrameworkForRunningInSharedJVM
): AbstractSharedJvmProcess(process, serverSocket, executor) {
    override fun supportsConfiguration(configuration: JavaTestConfigurationBase) =
        testFramework.canRunTestsFor(configuration)

    override fun runTests(configuration: JavaTestConfigurationBase) {
        val searchTask = createTestSearchTask(configuration)
        val progressIndicator = BackgroundableProcessIndicator(searchTask)
        ProgressManager.getInstance().runProcess({ searchTask.run(progressIndicator) }, progressIndicator)
        searchTask.finish()
    }

    private fun createTestSearchTask(configuration: JavaTestConfigurationBase): SearchForTestsTask {
        val tempFile = FileUtil.createTempFile("idea_sharedjvm_testng", ".tmp", true)
        return DontCloseServerSocketOnFinishSearchForTestNGTestsTask(serverSocket, configuration as TestNGConfiguration, tempFile)
    }

    override fun createConsoleProperties(
        configuration: JavaTestConfigurationBase,
        executor: Executor
    ) = TestNGConsoleProperties(configuration as TestNGConfiguration, executor)

    override fun customizeConsoleTab(consoleTab: Content) {
        consoleTab.icon = TestngIcons.TestNG
    }
}