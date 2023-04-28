package com.wrike.sprinter

import com.intellij.execution.application.JvmMainMethodRunConfigurationOptions
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.util.xmlb.annotations.OptionTag
import com.wrike.sprinter.frameworks.TestFrameworkId

class SharedJvmConfigurationType: SimpleConfigurationType(
    "SharedJvmForTests",
    "SharedJvmForTests",
    null,
    NotNullLazyValue.createValue { AllIcons.RunConfigurations.Application }) {
    override fun isEditableInDumbMode(): Boolean = true

    override fun getOptionsClass(): Class<out BaseState> {
        return SharedJvmConfigurationOptions::class.java
    }

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return SharedJvmConfiguration(project)
    }
}

class SharedJvmConfigurationOptions: JvmMainMethodRunConfigurationOptions() {
    @get:OptionTag(nameAttribute = "TEST_FRAMEWORK_ID")
    var testFrameworkId: TestFrameworkId? by string()
}

fun getSharedJvmConfigurationTypeInstance(): SharedJvmConfigurationType {
    return ConfigurationTypeUtil.findConfigurationType(SharedJvmConfigurationType::class.java)
}