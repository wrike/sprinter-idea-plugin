package com.wrike.sprinter.settings

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.util.Function

class ModuleChangeListener(project: Project) : ModuleListener {
    private val modulesWithHotSwapAgentPluginsService = getModulesWithHotSwapAgentPluginsService(project)

    override fun beforeModuleRemoved(project: Project, module: Module) {
        modulesWithHotSwapAgentPluginsService.delete(module)
    }

    override fun modulesRenamed(project: Project, modules: MutableList<out Module>, oldNameProvider: Function<in Module, String>) {
        for (module in modules) {
            modulesWithHotSwapAgentPluginsService.rename(module, oldNameProvider.`fun`(module))
        }
    }
}