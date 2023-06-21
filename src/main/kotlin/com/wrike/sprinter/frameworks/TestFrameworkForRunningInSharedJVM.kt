package com.wrike.sprinter.frameworks

import com.intellij.execution.Executor
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import com.wrike.sprinter.SharedJvmConfiguration
import java.net.ServerSocket

val testFrameworkForRunningInSharedJVMExtensionPoint = ExtensionPointName.create<TestFrameworkForRunningInSharedJVM>(
    "com.wrike.sprinter.testFrameworkForRunningInSharedJVM"
)

typealias TestFrameworkId = String

interface TestFrameworkForRunningInSharedJVM {
    val frameworkId: TestFrameworkId

    fun createRunnableState(sharedJvmConfiguration: SharedJvmConfiguration,
                            testConfiguration: JavaTestConfigurationBase,
                            environment: ExecutionEnvironment): AbstractSharedJvmRunnableState<*, *>

    fun wrapSharedJvmProcess(process: OSProcessHandler,
                             serverSocket: ServerSocket,
                             executor: Executor): AbstractSharedJvmProcess

    fun canRunTestsFor(testConfiguration: JavaTestConfigurationBase): Boolean

    fun canRunTestsFor(element: PsiElement): Boolean
}