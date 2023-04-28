package com.wrike.sprinter.frameworks.testng

import com.intellij.execution.Executor
import com.intellij.execution.process.OSProcessHandler
import com.intellij.testIntegration.JavaTestFramework
import com.intellij.testIntegration.TestFramework
import com.theoryinpractice.testng.TestNGFramework
import com.wrike.sprinter.frameworks.TestFrameworkForRunningInSharedJVM
import com.wrike.sprinter.frameworks.TestFrameworkId
import com.wrike.sprinter.frameworks.testng.rt.IDEARemoteSameJvmTestNGStarterKotlin
import java.net.ServerSocket

class TestNGForRunningInSharedJVM: TestFrameworkForRunningInSharedJVM {
    override val frameworkId: TestFrameworkId = "TestNG"
    override val originalTestFramework: JavaTestFramework? = TestFramework.EXTENSION_NAME.findExtension(TestNGFramework::class.java)

    override fun getRunClassFQN(): String = IDEARemoteSameJvmTestNGStarterKotlin::class.qualifiedName!!

    override fun wrapSharedJvmProcess(process: OSProcessHandler, serverSocket: ServerSocket, executor: Executor) =
        TestNGSharedJvmProcess(process, serverSocket, executor, this)
}