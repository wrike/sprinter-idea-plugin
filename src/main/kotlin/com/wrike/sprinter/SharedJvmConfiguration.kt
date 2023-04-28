package com.wrike.sprinter

import com.intellij.execution.Executor
import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.ShortenCommandLine
import com.intellij.execution.application.ApplicationConfiguration.onAlternativeJreChanged
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces
import com.intellij.util.PathUtil
import com.wrike.sprinter.frameworks.TestFrameworkId
import com.wrike.sprinter.frameworks.testFrameworkForRunningInSharedJVMExtensionPoint
import java.util.*

class SharedJvmConfiguration internal constructor(
    name: String?,
    project: Project,
    factory: ConfigurationFactory,
): JavaRunConfigurationBase(name, JavaRunConfigurationModule(project, true), factory) {
    var testFrameworkId: TestFrameworkId
        get() = options.testFrameworkId!!
        set(value) {
            options.testFrameworkId = value
        }
    val runClassFQN: String by lazy(LazyThreadSafetyMode.NONE) {
        testFrameworkForRunningInSharedJVMExtensionPoint.extensionList.find {
            it.frameworkId == testFrameworkId
        }!!.getRunClassFQN()
    }
    var hotSwapAgentPluginsModules = listOf<Module>()

    constructor(project: Project) : this(null, project, getSharedJvmConfigurationTypeInstance())

    override fun getOptions(): SharedJvmConfigurationOptions {
        return super.getOptions() as SharedJvmConfigurationOptions
    }

    override fun restartSingleton(environment: ExecutionEnvironment): RunConfiguration.RestartSingletonResult {
        return RunConfiguration.RestartSingletonResult.RESTART
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return SharedJvmSettingEditor(this)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return SharedJvmRunnableState(environment, this, testFrameworkId)
    }

    override fun getValidModules(): MutableCollection<Module> {
        return ModuleManager.getInstance(project).modules.toMutableList()
    }

    override fun getModules(): Array<Module> {
        return if (hotSwapAgentPluginsModules.isEmpty()) {
            super.getModules()
        } else {
            super.getModules() + hotSwapAgentPluginsModules
        }
    }

    override fun setProgramParameters(value: String?) {
        options.programParameters = value
    }

    override fun getProgramParameters(): String? {
        return options.programParameters
    }

    override fun setWorkingDirectory(value: String?) {
        val normalizedValue = if (isEmptyOrSpaces(value)) null else value?.trim()
        val systemIndependentName = PathUtil.toSystemIndependentName(normalizedValue)
        options.workingDirectory = if (Objects.equals(systemIndependentName, project.basePath)) {
            null
        } else {
            systemIndependentName
        }
    }

    override fun getWorkingDirectory(): String? {
        return options.workingDirectory
    }

    override fun setEnvs(envs: MutableMap<String, String>) {
        options.env = envs
    }

    override fun getEnvs(): MutableMap<String, String> {
        return options.env
    }

    override fun setPassParentEnvs(passParentEnvs: Boolean) {
        options.isPassParentEnv = passParentEnvs
    }

    override fun isPassParentEnvs(): Boolean {
        return options.isPassParentEnv
    }

    override fun setAlternativeJrePathEnabled(enabled: Boolean) {
        val changed = enabled == options.isAlternativeJrePathEnabled
        options.isAlternativeJrePathEnabled = enabled
        onAlternativeJreChanged(changed, project)
    }

    override fun isAlternativeJrePathEnabled(): Boolean {
        return options.isAlternativeJrePathEnabled
    }

    override fun setAlternativeJrePath(value: String?) {
        val changed = !Objects.equals(value, options.alternativeJrePath)
        options.alternativeJrePath = value
        onAlternativeJreChanged(changed, project)
    }

    override fun getAlternativeJrePath(): String? {
        return options.alternativeJrePath
    }

    override fun setVMParameters(value: String?) {
        options.vmParameters = value
    }

    override fun getVMParameters(): String? {
        return options.vmParameters
    }

    override fun getRunClass(): String {
        return runClassFQN
    }

    override fun getPackage(): String? {
        return null
    }

    override fun getShortenCommandLine(): ShortenCommandLine? {
        return options.shortenClasspath
    }

    override fun setShortenCommandLine(mode: ShortenCommandLine?) {
        options.shortenClasspath = mode
    }
}