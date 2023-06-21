package com.wrike.sprinter.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel

class SharedJvmSettingsConfigurable(
    private val project: Project
): BoundConfigurable("Shared JVM Settings") {
    override fun createPanel(): DialogPanel = createSharedJvmSettingPanel(project)
}