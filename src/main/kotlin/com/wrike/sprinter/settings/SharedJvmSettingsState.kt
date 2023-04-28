package com.wrike.sprinter.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.wrike.sprinter.settings.SharedJvmSettingsState",
    storages = [Storage("SharedJvmSettingsPlugin.xml")]
)
class SharedJvmSettingsState : PersistentStateComponent<SharedJvmSettingsState> {
    var additionalJvmParameters = "-XX:-MaxFDLimit"
    var hotswapProperties = ""
    var hotswapAgentCMDArguments = ""

    override fun getState(): SharedJvmSettingsState {
        return this
    }

    override fun loadState(state: SharedJvmSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}

fun getSharedJvmSettings(project: Project): SharedJvmSettingsState {
    return project.getService(SharedJvmSettingsState::class.java)
}