package com.wrike.sprinter.frameworks.junit

import com.intellij.execution.Executor
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.junit.JUnitTestFramework
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.testIntegration.TestFramework
import com.wrike.sprinter.SharedJvmConfiguration
import com.wrike.sprinter.frameworks.TestFrameworkForRunningInSharedJVM
import com.wrike.sprinter.frameworks.TestFrameworkId
import java.net.ServerSocket

class JUnitForRunningInSharedJvm : TestFrameworkForRunningInSharedJVM {
    private val originalTestFrameworks = TestFramework.EXTENSION_NAME.point.extensions.asSequence()
        .filterIsInstance<JUnitTestFramework>()
        .toList()

    override val frameworkId: TestFrameworkId
        get() = "JUnit"

    override fun createRunnableState(
        sharedJvmConfiguration: SharedJvmConfiguration,
        testConfiguration: JavaTestConfigurationBase,
        environment: ExecutionEnvironment
    ) = SharedJvmJUnitRunnableState(
        environment,
        sharedJvmConfiguration,
        testConfiguration as JUnitConfiguration,
        this
    )

    override fun canRunTestsFor(testConfiguration: JavaTestConfigurationBase): Boolean {
        return originalTestFrameworks.any { it.isMyConfigurationType(testConfiguration.type) }
    }

    override fun canRunTestsFor(element: PsiElement): Boolean {
        if (element !is PsiIdentifier
            && element !is PsiClass
            && element !is PsiMethod
        ) return false
        return originalTestFrameworks.any {
            if (isTestClass(it, element) || isTestMethod(it, element)) {
                true
            } else when (val parent = element.parent) {
                is PsiClass -> isTestClass(it, parent)
                is PsiMethod -> isTestMethod(it, parent)
                else -> false
            }
        }
    }


    private fun isTestClass(framework: TestFramework, clazz: PsiClass): Boolean {
        return framework.isTestClass(clazz)
    }

    private fun isTestClass(framework: TestFramework, element: PsiElement): Boolean {
        return element is PsiClass && isTestClass(framework, element)
    }

    private fun isTestMethod(framework: TestFramework, method: PsiMethod): Boolean {
        return framework.isTestMethod(method, false)
    }

    private fun isTestMethod(framework: TestFramework, element: PsiElement): Boolean {
        return element is PsiMethod && isTestMethod(framework, element)
    }

    override fun wrapSharedJvmProcess(process: OSProcessHandler, serverSocket: ServerSocket, executor: Executor) =
        JUnitSharedJvmProcess(process, serverSocket, executor, this)
}
