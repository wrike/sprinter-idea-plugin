package com.wrike.sprinter

import com.intellij.execution.application.JavaSettingsEditorBase
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.ModuleClasspathCombo
import com.intellij.execution.ui.SettingsEditorFragment

class SharedJvmSettingEditor(
    configuration: SharedJvmConfiguration
): JavaSettingsEditorBase<SharedJvmConfiguration>(configuration) {
    override fun customizeFragments(
        fragments: MutableList<SettingsEditorFragment<SharedJvmConfiguration, *>>?,
        moduleClasspath: SettingsEditorFragment<SharedJvmConfiguration, ModuleClasspathCombo>?,
        commonParameterFragments: CommonParameterFragments<SharedJvmConfiguration>?
    ) {}
}
