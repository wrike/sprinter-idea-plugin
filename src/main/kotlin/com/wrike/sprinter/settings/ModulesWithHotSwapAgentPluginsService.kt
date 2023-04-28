package com.wrike.sprinter.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module

interface ModulesWithHotSwapAgentPluginsService: Disposable {
    fun setModules(modules: List<Module>)
    fun getModules(): List<Module>
    fun getModuleNames(): Set<String>
    fun hasConfiguration(module: Module): Boolean
    fun rename(module: Module, oldName: String)
    fun delete(module: Module)
}

class ModulesWithHotSwapAgentPluginsServiceImpl(
    private val project: Project
): ModulesWithHotSwapAgentPluginsService {
    private val modulesWithHotSwapAgentPlugins = getModulesWithHotSwapAgentPluginsSettings(project)

    override fun setModules(modules: List<Module>) {
        modulesWithHotSwapAgentPlugins.moduleNames = modules.asSequence()
            .map(Module::getName)
            .toMutableSet()
    }

    override fun getModules(): List<Module> {
        if (modulesWithHotSwapAgentPlugins.moduleNames.isEmpty()) return emptyList()
        val moduleManager = ModuleManager.getInstance(project)
        val removedModuleNames = mutableSetOf<String>()
        val moduleList = modulesWithHotSwapAgentPlugins.moduleNames.asSequence()
            .map {
                val module = moduleManager.findModuleByName(it)
                if (module == null) {
                    removedModuleNames.add(it)
                }
                module
            }
            .filterNotNull()
            .toList()
        modulesWithHotSwapAgentPlugins.moduleNames.removeAll(removedModuleNames)
        return moduleList
    }

    override fun getModuleNames(): Set<String> {
        return modulesWithHotSwapAgentPlugins.moduleNames
    }

    override fun hasConfiguration(module: Module): Boolean {
        return modulesWithHotSwapAgentPlugins.moduleNames.contains(module.name)
    }

    override fun rename(module: Module, oldName: String) {
        modulesWithHotSwapAgentPlugins.moduleNames.remove(oldName)
        modulesWithHotSwapAgentPlugins.moduleNames.add(module.name)
    }

    override fun delete(module: Module) {
        modulesWithHotSwapAgentPlugins.moduleNames.remove(module.name)
    }

    override fun dispose() {}
}

fun getModulesWithHotSwapAgentPluginsService(project: Project): ModulesWithHotSwapAgentPluginsService {
    return project.getService(ModulesWithHotSwapAgentPluginsService::class.java)
}