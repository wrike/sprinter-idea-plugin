package com.wrike.sprinter.frameworks.testng

import com.intellij.execution.Executor
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.testIntegration.JavaTestFramework
import com.intellij.testIntegration.TestFramework
import com.theoryinpractice.testng.TestNGFramework
import com.theoryinpractice.testng.configuration.TestNGConfiguration
import com.wrike.sprinter.SharedJvmConfiguration
import com.wrike.sprinter.frameworks.TestFrameworkForRunningInSharedJVM
import com.wrike.sprinter.frameworks.TestFrameworkId
import java.net.ServerSocket

class TestNGForRunningInSharedJVM: TestFrameworkForRunningInSharedJVM {
    private val originalTestFramework: JavaTestFramework? = TestFramework.EXTENSION_NAME.findExtension(TestNGFramework::class.java)

    override val frameworkId: TestFrameworkId = "TestNG"

    override fun createRunnableState(sharedJvmConfiguration: SharedJvmConfiguration,
                                     testConfiguration: JavaTestConfigurationBase,
                                     environment: ExecutionEnvironment
    ) = SharedJvmTestNGRunnableState(
        environment,
        sharedJvmConfiguration,
        testConfiguration as TestNGConfiguration,
        this)


    override fun canRunTestsFor(testConfiguration: JavaTestConfigurationBase): Boolean {
        return originalTestFramework?.isMyConfigurationType(testConfiguration.type) ?: false
    }

    override fun canRunTestsFor(element: PsiElement): Boolean {
        if (element !is PsiIdentifier
            && element !is PsiClass
            && element !is PsiMethod
        ) return false
        if (isTestClass(element) || isTestMethod(element)) return true
        return when (val parent = element.parent) {
            is PsiClass -> isTestClass(parent)
            is PsiMethod -> isTestMethod(parent)
            else -> false
        }
    }

    private fun isTestClass(clazz: PsiClass): Boolean {
        return originalTestFramework?.isTestClass(clazz) ?: false
    }

    private fun isTestClass(element: PsiElement): Boolean {
        return element is PsiClass && isTestClass(element)
    }

    private fun isTestMethod(method: PsiMethod): Boolean {
        return originalTestFramework?.isTestMethod(method, false) ?: false
    }

    private fun isTestMethod(element: PsiElement): Boolean {
        return element is PsiMethod && isTestMethod(element)
    }

    override fun wrapSharedJvmProcess(process: OSProcessHandler, serverSocket: ServerSocket, executor: Executor) =
        TestNGSharedJvmProcess(process, serverSocket, executor, this)
}