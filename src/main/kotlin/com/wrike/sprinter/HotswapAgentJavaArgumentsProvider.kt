package com.wrike.sprinter

import com.intellij.execution.configurations.JavaParameters
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import com.wrike.sprinter.settings.getModulesWithHotSwapAgentPluginsService
import com.wrike.sprinter.settings.getSharedJvmSettings

interface HotswapAgentJavaArgumentsProvider {
    fun addArguments(params: JavaParameters)
}

class HotswapAgentJavaArgumentsProviderImpl(project: Project) : HotswapAgentJavaArgumentsProvider {
    private val sharedJvmSettings = getSharedJvmSettings(project)
    private val modulesWithHotSwapAgentPluginsService = getModulesWithHotSwapAgentPluginsService(project)

    override fun addArguments(params: JavaParameters) {
        params.vmParametersList.addParametersString(sharedJvmSettings.additionalJvmParameters)
        val hotswapAgentArg = "-javaagent:${params.jdkPath}/lib/hotswap/hotswap-agent.jar".let {
            if (sharedJvmSettings.hotswapAgentCMDArguments.isNotBlank()) {
                "$it=${sharedJvmSettings.hotswapAgentCMDArguments}"
            } else it
        }
        params.vmParametersList.addAll(
            "-XX:+AllowEnhancedClassRedefinition",
            "-XX:HotswapAgent=external",
            hotswapAgentArg
        )

        modulesWithHotSwapAgentPluginsService.getModules().forEach {
            OrderEnumerator.orderEntries(it).recursively().withoutSdk().classes()
                .collectPaths(params.classPath)
        }
    }
}

fun getHotswapAgentJavaArgumentsProvider(project: Project): HotswapAgentJavaArgumentsProvider {
    return project.getService(HotswapAgentJavaArgumentsProvider::class.java)
}