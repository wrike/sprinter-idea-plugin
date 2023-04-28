package com.wrike.sprinter

import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

open class RunTestsInExistingJvmAction : AnAction({ "Run in Launched JVM" }, AllIcons.RunConfigurations.TestState.Run_run) {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val context = ConfigurationContext.getFromContext(e.dataContext, e.place)
        val sharedJvmExecutorService = project.getService(SharedJvmExecutorService::class.java) ?: return
        val testsConfiguration = getConfigurationToRun(context, sharedJvmExecutorService)
        if (testsConfiguration != null
            && testsConfiguration is JavaTestConfigurationBase
            && sharedJvmExecutorService.isSupported(testsConfiguration)
        ) {
            sharedJvmExecutorService.executeConfiguration(testsConfiguration, context)
        }
    }

    protected open fun getConfigurationToRun(
        context: ConfigurationContext,
        sharedJvmExecutorService: SharedJvmExecutorService
    ): RunConfiguration? {
        return when (context.place) {
            ActionPlaces.EDITOR_GUTTER_POPUP,
            ActionPlaces.EDITOR_POPUP,
            ActionPlaces.KEYBOARD_SHORTCUT,
            ActionPlaces.MOUSE_SHORTCUT -> context.configuration?.configuration
            else -> null
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val sharedJvmExecutorService = project.getService(SharedJvmExecutorService::class.java) ?: return
        val context = ConfigurationContext.getFromContext(e.dataContext, e.place)
        val configurationToRun = getConfigurationToRun(context, sharedJvmExecutorService)
        val isVisible = configurationToRun != null
                && configurationToRun is JavaTestConfigurationBase
                && sharedJvmExecutorService.isSupported(configurationToRun)
        e.presentation.isVisible = isVisible
    }
}