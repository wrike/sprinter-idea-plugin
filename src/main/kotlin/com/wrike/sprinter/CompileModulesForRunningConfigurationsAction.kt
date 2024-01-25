package com.wrike.sprinter

import com.intellij.compiler.actions.CompileActionBase
import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.util.JavaParametersUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTaskManager

class CompileModulesForRunningConfigurationsAction : CompileActionBase() {
    init {
        templatePresentation.text = "Compile Modules For Running Configurations"
    }

    override fun doAction(context: DataContext, project: Project) {
        val testsIncludedToConfigurations = getMapOfTestsIncludedToConfigurationsToCompile(project)
        if (testsIncludedToConfigurations.isEmpty()) {
            return
        }
        val projectTaskManager = ProjectTaskManager.getInstance(project)
        for ((testsIncluded, configurations) in testsIncludedToConfigurations.entries) {
            val configurationsToCompile = configurations.asSequence()
                .flatMap { it.modules.asSequence() }
                .toSet()
            if (configurationsToCompile.isNotEmpty()) {
                val modulesBuildTask = projectTaskManager.createModulesBuildTask(
                    configurationsToCompile.toTypedArray(),
                    true,
                    true,
                    false,
                    testsIncluded
                )
                projectTaskManager.run(modulesBuildTask)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val project = e.project
        presentation.isEnabled = project != null
                && getRunningDescriptorsForCompilation(ExecutionManagerImpl.getInstance(project)).isNotEmpty()
    }

    private fun getRunningDescriptorsForCompilation(executionManager: ExecutionManagerImpl): List<RunContentDescriptor> {
        return executionManager.getRunningDescriptors {
            it.configuration is JavaRunConfigurationBase
        }
    }

    private fun getMapOfTestsIncludedToConfigurationsToCompile(project: Project): Map<Boolean, List<ModuleBasedConfiguration<*, *>>> {
        val executionManager = ExecutionManagerImpl.getInstance(project)
        val runningDescriptors = getRunningDescriptorsForCompilation(executionManager)
        return runningDescriptors.asSequence()
            .flatMap { descriptor ->
                executionManager.getConfigurations(descriptor).asSequence()
                    .map { runnerAndConfigSettings ->
                        runnerAndConfigSettings.configuration as JavaRunConfigurationBase
                    }
            }
            .groupBy { configuration ->
                JavaParametersUtil.getClasspathType(
                    configuration.configurationModule,
                    configuration.runClass!!,
                    false,
                    true
                ) and JavaParameters.TESTS_ONLY != 0
            }

    }
}