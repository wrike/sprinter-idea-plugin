package com.wrike.sprinter.settings

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

interface ConfigurationsWithHotswapAgentService: Disposable {
    fun getConfigurations(): List<RunnerAndConfigurationSettings>
    fun getConfigurationIds(): Set<String>
    fun setConfigurations(configurations: List<RunnerAndConfigurationSettings>)
    fun hasConfiguration(configuration: RunnerAndConfigurationSettings): Boolean
    fun hasConfiguration(configurationId: String): Boolean
    fun update(previousConfigurationId: String, configuration: RunnerAndConfigurationSettings)
    fun delete(configurationId: String)
}

class ConfigurationsWithHotswapAgentServiceImpl(
    private val project: Project
) : ConfigurationsWithHotswapAgentService {
    private val configurationsWithHotswapAgent = getConfigurationsWithHotswapAgentSettings(project)

    override fun getConfigurations(): List<RunnerAndConfigurationSettings> {
        if (configurationsWithHotswapAgent.configurationIds.isEmpty()) return emptyList()
        val runManager = RunManagerImpl.getInstanceImpl(project)
        val removedConfigurationIds = mutableSetOf<String>()
        val result = configurationsWithHotswapAgent.configurationIds.asSequence()
            .map {
                val configuration = runManager.getConfigurationById(it)
                if (configuration == null) {
                    removedConfigurationIds.add(it)
                }
                configuration
            }
            .filterNotNull()
            .toList()
        configurationsWithHotswapAgent.configurationIds.removeAll(removedConfigurationIds)
        return result
    }

    override fun getConfigurationIds(): Set<String> {
        return configurationsWithHotswapAgent.configurationIds
    }

    override fun setConfigurations(configurations: List<RunnerAndConfigurationSettings>) {
        configurationsWithHotswapAgent.configurationIds.clear()
        configurationsWithHotswapAgent.configurationIds.addAll(
            configurations.asSequence()
                .map(RunnerAndConfigurationSettings::getUniqueID)
                .toSet()
        )
    }

    override fun hasConfiguration(configuration: RunnerAndConfigurationSettings): Boolean {
        return configurationsWithHotswapAgent.configurationIds.contains(configuration.uniqueID)
    }

    override fun hasConfiguration(configurationId: String): Boolean {
        return configurationsWithHotswapAgent.configurationIds.contains(configurationId)
    }

    override fun update(previousConfigurationId: String, configuration: RunnerAndConfigurationSettings) {
        if (configurationsWithHotswapAgent.configurationIds.remove(previousConfigurationId)) {
            configurationsWithHotswapAgent.configurationIds.add(configuration.uniqueID)
        }
    }

    override fun delete(configurationId: String) {
        configurationsWithHotswapAgent.configurationIds.remove(configurationId)
    }

    override fun dispose() {}
}

fun getConfigurationsWithHotswapAgentService(project: Project): ConfigurationsWithHotswapAgentService {
    return project.getService(ConfigurationsWithHotswapAgentService::class.java)
}