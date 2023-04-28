package com.wrike.sprinter.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class SharedJvmSettingsConfigurable(
    private val project: Project
): Configurable {
    private val sharedJvmSettings = getSharedJvmSettings(project)
    private val configurationsWithHotswapAgentService = getConfigurationsWithHotswapAgentService(project)
    private val modulesWithHotSwapAgentPluginsService = getModulesWithHotSwapAgentPluginsService(project)
    private var component: SharedJvmSettingsComponent? = null

    override fun getDisplayName(): String = "Shared JVM Settings"

    override fun createComponent(): JComponent? {
        component = SharedJvmSettingsComponent(project)
        return component?.settingsPanel
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return component?.preferredFocusedComponent
    }

    override fun isModified(): Boolean {
        return component?.run {
            sharedJvmSettings.additionalJvmParameters != additionalJvmParametersText
                    || sharedJvmSettings.hotswapAgentCMDArguments != hotswapAgentCMDArgumentsText
                    || sharedJvmSettings.hotswapProperties != hotswapPropertiesText
                    || configurationsWithHotswapAgentService.getConfigurationIds() != getConfigurationIds()
                    || modulesWithHotSwapAgentPluginsService.getModuleNames() != getModuleNames()
        } ?: false

    }

    override fun apply() {
        component?.run {
            sharedJvmSettings.additionalJvmParameters = additionalJvmParametersText
            sharedJvmSettings.hotswapAgentCMDArguments = hotswapAgentCMDArgumentsText
            sharedJvmSettings.hotswapProperties = hotswapPropertiesText
            configurationsWithHotswapAgentService.setConfigurations(getConfigurations())
            modulesWithHotSwapAgentPluginsService.setModules(getModules())
        }
    }

    override fun reset() {
        component?.run {
            additionalJvmParametersText = sharedJvmSettings.additionalJvmParameters
            hotswapAgentCMDArgumentsText = sharedJvmSettings.hotswapAgentCMDArguments
            hotswapPropertiesText = sharedJvmSettings.hotswapProperties
            setConfigurations(configurationsWithHotswapAgentService.getConfigurations())
            setModules(modulesWithHotSwapAgentPluginsService.getModules())
        }
    }

    override fun disposeUIResources() {
        component = null
    }
}