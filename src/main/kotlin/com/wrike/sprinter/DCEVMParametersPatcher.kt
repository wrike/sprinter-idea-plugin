package com.wrike.sprinter

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.runners.JavaProgramPatcher
import com.wrike.sprinter.settings.getConfigurationsWithHotswapAgentService

class DCEVMParametersPatcher: JavaProgramPatcher() {
    override fun patchJavaParameters(executor: Executor, configuration: RunProfile, params: JavaParameters) {
        if (configuration !is RunConfigurationBase<*> || !isApplicable(configuration)) return
        getHotswapAgentJavaArgumentsProvider(configuration.project).addArguments(params)
    }

    private fun isApplicable(configuration: RunConfigurationBase<*>): Boolean {
        val configurationsWithHotswapAgentService = getConfigurationsWithHotswapAgentService(configuration.project)
        val configurationId = RunnerAndConfigurationSettingsImpl.getUniqueIdFor(configuration)
        return configurationsWithHotswapAgentService.hasConfiguration(configurationId)
    }
}