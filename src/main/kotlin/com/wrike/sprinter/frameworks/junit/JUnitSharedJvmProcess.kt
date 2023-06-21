package com.wrike.sprinter.frameworks.junit

import com.intellij.execution.Executor
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.junit2.ui.properties.JUnitConsoleProperties
import com.intellij.execution.process.OSProcessHandler
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.content.Content
import com.wrike.sprinter.frameworks.AbstractSharedJvmProcess
import com.wrike.sprinter.frameworks.TestFrameworkForRunningInSharedJVM
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket

class JUnitSharedJvmProcess(
    process: OSProcessHandler,
    serverSocket: ServerSocket,
    executor: Executor,
    private val testFramework: TestFrameworkForRunningInSharedJVM
): AbstractSharedJvmProcess(process, serverSocket, executor) {
    private val log = Logger.getInstance(JUnitSharedJvmProcess::class.java)

    override fun supportsConfiguration(configuration: JavaTestConfigurationBase): Boolean =
        testFramework.canRunTestsFor(configuration)

    override fun runTests(configuration: JavaTestConfigurationBase) {
        val junitConfiguration = configuration as JUnitConfiguration
        val testObject = junitConfiguration.testObject
        val programParameters = testObject.javaParameters.programParametersList

        serverSocket.accept().use { socket ->
            try {
                val socketOS = DataOutputStream(socket.getOutputStream())
                val socketIS = DataInputStream(socket.getInputStream())
                socketOS.writeUTF(programParameters.parameters.joinToString(System.lineSeparator()))
                socketIS.readBoolean()
            } catch (e: Throwable) {
                log.warn(e)
            }
        }
    }

    override fun createConsoleProperties(
        configuration: JavaTestConfigurationBase,
        executor: Executor
    ) = object: JUnitConsoleProperties(configuration as JUnitConfiguration, executor) {
        override fun isIdBasedTestTree() = false
        override fun isUndefined() = false
    }

    override fun customizeConsoleTab(consoleTab: Content) {
        consoleTab.icon = AllIcons.RunConfigurations.Junit
    }
}