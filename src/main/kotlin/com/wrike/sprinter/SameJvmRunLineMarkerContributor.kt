package com.wrike.sprinter

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.psi.*
import com.wrike.sprinter.frameworks.testFrameworkForRunningInSharedJVMExtensionPoint

class SameJvmRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        val canRunTestForElement = testFrameworkForRunningInSharedJVMExtensionPoint.extensionList.any {
            it.canRunTestsFor(element)
        }
        return if (canRunTestForElement) {
            Info(null, null, ActionManager.getInstance().getAction("RunTestsInExistingJvm"))
        } else null
    }
}

