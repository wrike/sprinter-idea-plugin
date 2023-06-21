package com.wrike.sprinter.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project

interface ModulesWithHotSwapAgentPluginsService: Disposable {
    fun setModules(modules: List<Module>)
    fun getModules(): List<Module>
    fun getModuleNames(): Set<String>
    fun hasModule(module: Module): Boolean
    fun rename(module: Module, oldName: String)
    fun delete(module: Module)
}

class ModulesWithHotSwapAgentPluginsServiceImpl(
    private val project: Project
): ModulesWithHotSwapAgentPluginsService {
    private val sharedSettings = getSharedSprinterSettings(project)

    override fun setModules(modules: List<Module>) {
        sharedSettings.modulesWithHAPlugins = modules.asSequence()
            .map(Module::getName)
            .toMutableSet()
    }

    override fun getModules(): List<Module> {
        if (sharedSettings.modulesWithHAPlugins.isEmpty()) return emptyList()
        val moduleManager = ModuleManager.getInstance(project)
        val removedModuleNames = mutableSetOf<String>()
        val moduleList = sharedSettings.modulesWithHAPlugins.asSequence()
            .map {
                val module = moduleManager.findModuleByName(it)
                if (module == null) {
                    removedModuleNames.add(it)
                }
                module
            }
            .filterNotNull()
            .toList()
        sharedSettings.modulesWithHAPlugins.removeAll(removedModuleNames)
        return moduleList
    }

    override fun getModuleNames(): Set<String> {
        return sharedSettings.modulesWithHAPlugins
    }

    override fun hasModule(module: Module): Boolean {
        return sharedSettings.modulesWithHAPlugins.contains(module.name)
    }

    override fun rename(module: Module, oldName: String) {
        sharedSettings.modulesWithHAPlugins.remove(oldName)
        sharedSettings.modulesWithHAPlugins.add(module.name)
    }

    override fun delete(module: Module) {
        sharedSettings.modulesWithHAPlugins.remove(module.name)
    }

    override fun dispose() {}
}

fun getModulesWithHotSwapAgentPluginsService(project: Project): ModulesWithHotSwapAgentPluginsService {
    return project.getService(ModulesWithHotSwapAgentPluginsService::class.java)
}