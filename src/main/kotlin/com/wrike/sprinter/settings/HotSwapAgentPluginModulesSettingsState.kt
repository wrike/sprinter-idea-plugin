package com.wrike.sprinter.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.wrike.sprinter.settings.HotSwapAgentPluginModulesSettingsState",
    storages = [Storage("HotSwapAgentPluginModulesSettingsState.xml")]
)
class ModulesWithHotSwapAgentPluginsSettingsState: PersistentStateComponent<ModulesWithHotSwapAgentPluginsSettingsState> {
    var moduleNames = mutableSetOf<String>()

    override fun getState(): ModulesWithHotSwapAgentPluginsSettingsState {
        return this
    }

    override fun loadState(state: ModulesWithHotSwapAgentPluginsSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}

fun getModulesWithHotSwapAgentPluginsSettings(project: Project): ModulesWithHotSwapAgentPluginsSettingsState {
    return project.getService(ModulesWithHotSwapAgentPluginsSettingsState::class.java)
}
