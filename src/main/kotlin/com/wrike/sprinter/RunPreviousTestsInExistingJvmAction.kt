package com.wrike.sprinter

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.RunConfiguration

class RunPreviousTestsInExistingJvmAction: RunTestsInExistingJvmAction() {
    override fun getConfigurationToRun(
        context: ConfigurationContext,
        sharedJvmExecutorService: SharedJvmExecutorService
    ): RunConfiguration? {
        return sharedJvmExecutorService.getLastExecutedConfiguration()
    }
}