package com.wrike.sprinter

import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.RunManager
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.ConfigurationFromContextImpl
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.junit.JavaRunConfigurationProducerBase
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.wrike.sprinter.frameworks.testFrameworkForRunningInSharedJVMExtensionPoint
import com.wrike.sprinter.settings.getModulesWithHotSwapAgentPluginsService

class SharedJvmConfigurationProducer: JavaRunConfigurationProducerBase<SharedJvmConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory {
        return getSharedJvmConfigurationTypeInstance()
    }

    override fun setupConfigurationFromContext(
        configuration: SharedJvmConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val psiElement = sourceElement.get()
        val testFrameworks = testFrameworkForRunningInSharedJVMExtensionPoint.extensionList
        val applicableTestFramework = testFrameworks.find { it.canRunTestsFor(psiElement) } ?: return false
        sourceElement.set(psiElement.containingFile)
        val module = ModuleUtil.findModuleForPsiElement(psiElement) ?: return false
        configuration.setModule(module)
        configuration.hotSwapAgentPluginsModules = getModulesWithHotSwapAgentPluginsService(context.project).getModules()
        configuration.testFrameworkId = applicableTestFramework.frameworkId
        configuration.name = "${module.name}-${applicableTestFramework.frameworkId}"
        return true
    }

    override fun isConfigurationFromContext(configuration: SharedJvmConfiguration, context: ConfigurationContext): Boolean {
        val applicableTestFramework = testFrameworkForRunningInSharedJVMExtensionPoint
            .findFirstSafe { it.frameworkId == configuration.testFrameworkId } ?: return false
        return context.psiLocation?.let {
            applicableTestFramework.canRunTestsFor(it) && configuration.modules.contains(context.module)
        } ?: false
    }

    fun getConfigurationFromConfigurationToExecute(configurationToExecute: JavaRunConfigurationBase): ConfigurationFromContext {
        val configuration = SharedJvmConfiguration(configurationToExecute.project)
        val module = configurationToExecute.modules[0]
        configuration.setModule(module)
        configuration.hotSwapAgentPluginsModules = getModulesWithHotSwapAgentPluginsService(configurationToExecute.project).getModules()
        configuration.name = module.name
        val runAndConfigSettings = RunManager.getInstance(configurationToExecute.project).createConfiguration(configuration, configurationFactory)
        return ConfigurationFromContextImpl(this, runAndConfigSettings, null)
    }
}