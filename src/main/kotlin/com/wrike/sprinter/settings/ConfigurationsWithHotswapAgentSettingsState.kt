package com.wrike.sprinter.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.wrike.sprinter.settings.ConfigurationsWithHotswapAgentSettingsState",
    storages = [Storage("ConfigurationsWithHotswapAgentSettingsState.xml")]
)
class ConfigurationsWithHotswapAgentSettingsState: PersistentStateComponent<ConfigurationsWithHotswapAgentSettingsState> {
    var configurationIds = mutableSetOf<String>()

    override fun getState(): ConfigurationsWithHotswapAgentSettingsState {
        return this
    }

    override fun loadState(state: ConfigurationsWithHotswapAgentSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}

fun getConfigurationsWithHotswapAgentSettings(project: Project): ConfigurationsWithHotswapAgentSettingsState {
    return project.getService(ConfigurationsWithHotswapAgentSettingsState::class.java)
}