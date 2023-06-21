package com.wrike.sprinter.settings

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

interface ConfigurationsToAttachHAService: Disposable {
    fun getConfigurations(): List<RunnerAndConfigurationSettings>
    fun getConfigurationIds(): Set<String>
    fun setConfigurations(configurations: List<RunnerAndConfigurationSettings>)
    fun hasConfiguration(configuration: RunnerAndConfigurationSettings): Boolean
    fun hasConfiguration(configurationId: String): Boolean
    fun update(previousConfigurationId: String, configuration: RunnerAndConfigurationSettings)
    fun delete(configurationId: String)
}

class ConfigurationsToAttachHAServiceImpl(
    private val project: Project
) : ConfigurationsToAttachHAService {
    private val sharedSettings = getSharedSprinterSettings(project)

    override fun getConfigurations(): List<RunnerAndConfigurationSettings> {
        if (sharedSettings.configurationsToAttachHA.isEmpty()) return emptyList()
        val runManager = RunManagerImpl.getInstanceImpl(project)
        val removedConfigurationIds = mutableSetOf<String>()
        val result = sharedSettings.configurationsToAttachHA.asSequence()
            .map {
                val configuration = runManager.getConfigurationById(it)
                if (configuration == null) {
                    removedConfigurationIds.add(it)
                }
                configuration
            }
            .filterNotNull()
            .toList()
        sharedSettings.configurationsToAttachHA.removeAll(removedConfigurationIds)
        return result
    }

    override fun getConfigurationIds(): Set<String> {
        return sharedSettings.configurationsToAttachHA
    }

    override fun setConfigurations(configurations: List<RunnerAndConfigurationSettings>) {
        sharedSettings.configurationsToAttachHA.clear()
        sharedSettings.configurationsToAttachHA.addAll(
            configurations.asSequence()
                .map(RunnerAndConfigurationSettings::getUniqueID)
                .toSet()
        )
    }

    override fun hasConfiguration(configuration: RunnerAndConfigurationSettings): Boolean {
        return sharedSettings.configurationsToAttachHA.contains(configuration.uniqueID)
    }

    override fun hasConfiguration(configurationId: String): Boolean {
        return sharedSettings.configurationsToAttachHA.contains(configurationId)
    }

    override fun update(previousConfigurationId: String, configuration: RunnerAndConfigurationSettings) {
        if (sharedSettings.configurationsToAttachHA.remove(previousConfigurationId)) {
            sharedSettings.configurationsToAttachHA.add(configuration.uniqueID)
        }
    }

    override fun delete(configurationId: String) {
        sharedSettings.configurationsToAttachHA.remove(configurationId)
    }

    override fun dispose() {}
}

fun getConfigurationsWithHotswapAgentService(project: Project): ConfigurationsToAttachHAService {
    return project.getService(ConfigurationsToAttachHAService::class.java)
}