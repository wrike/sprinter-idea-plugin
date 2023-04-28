package com.wrike.sprinter.settings

import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project

class ConfigurationChangeListener(project: Project): RunManagerListener {
    private val configurationsWithHotswapAgentService = getConfigurationsWithHotswapAgentService(project)
        override fun runConfigurationRemoved(settings: RunnerAndConfigurationSettings) {
            configurationsWithHotswapAgentService.delete(settings.uniqueID)
        }

        override fun runConfigurationChanged(settings: RunnerAndConfigurationSettings, existingId: String?) {
            if (existingId != null) {
                configurationsWithHotswapAgentService.update(existingId, settings)
            }
        }
}