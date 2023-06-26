package com.wrike.sprinter.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.wrike.sprinter.settings.LocalSprinterSettingsState",
    storages = [Storage("sprinter/LocalSettings.xml", roamingType = RoamingType.DISABLED)]
)
class LocalSprinterSettingsState : PersistentStateComponent<LocalSprinterSettingsState> {
    var usedJvmType: UsedJvmType = UsedJvmType.Plain
    var hotswapAgentLocation: String = ""

    override fun getState(): LocalSprinterSettingsState {
        return this
    }

    override fun loadState(state: LocalSprinterSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}

enum class UsedJvmType {
    Plain,
    DCEVM
}


@State(
    name = "com.wrike.sprinter.settings.SharedSprinterSettingsState",
    storages = [Storage("sprinter/SharedSettings.xml")]
)
class SharedSprinterSettingsState : PersistentStateComponent<SharedSprinterSettingsState> {
    var passEnvironmentVariablesFromOriginalConfig = false
    var passSystemPropsFromOriginalConfig = false
    var passCMDArgsFromOriginalConfig = false
    var additionalJvmParameters = "-XX:+AllowEnhancedClassRedefinition -XX:-MaxFDLimit"
    var hotswapProperties = ""
    var hotswapAgentCMDArguments = ""
    var configurationsToAttachHA = mutableSetOf<String>()
    var modulesWithHAPlugins = mutableSetOf<String>()

    override fun getState(): SharedSprinterSettingsState {
        return this
    }

    override fun loadState(state: SharedSprinterSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}

fun getSharedSprinterSettings(project: Project): SharedSprinterSettingsState = project.getService(SharedSprinterSettingsState::class.java)
fun getLocalSprinterSettings(project: Project): LocalSprinterSettingsState = project.getService(LocalSprinterSettingsState::class.java)