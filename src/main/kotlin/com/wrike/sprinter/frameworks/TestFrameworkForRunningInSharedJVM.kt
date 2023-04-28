package com.wrike.sprinter.frameworks

import com.intellij.execution.Executor
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.testIntegration.JavaTestFramework
import java.net.ServerSocket

val testFrameworkForRunningInSharedJVMExtensionPoint = ExtensionPointName.create<TestFrameworkForRunningInSharedJVM>(
    "com.wrike.sprinter.testFrameworkForRunningInSharedJVM"
)

typealias TestFrameworkId = String

interface TestFrameworkForRunningInSharedJVM {
    val frameworkId: TestFrameworkId
    val originalTestFramework: JavaTestFramework?
    fun getRunClassFQN(): String

    fun wrapSharedJvmProcess(process: OSProcessHandler,
                             serverSocket: ServerSocket,
                             executor: Executor): AbstractSharedJvmProcess

    fun canRunTestsFor(testConfiguration: JavaTestConfigurationBase): Boolean {
        return originalTestFramework?.isMyConfigurationType(testConfiguration.type) ?: false
    }

    fun canRunTestsFor(element: PsiElement): Boolean {
        if (element !is PsiIdentifier) return false
        if (isTestClass(element) && isTestMethod(element)) return true
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
}