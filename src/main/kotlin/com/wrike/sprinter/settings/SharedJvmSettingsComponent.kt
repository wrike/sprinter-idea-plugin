package com.wrike.sprinter.settings

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ChooseRunConfigurationPopup
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.panel
import javax.swing.*

class SharedJvmSettingsComponent(
    private val project: Project
) {
    private val additionalJvmParameters = RawCommandLineEditor()
    private val hotswapAgentCMDArguments = RawCommandLineEditor()
    private val hotswapProperties = JBTextArea()
    private val configurationsModel = CollectionListModel<RunnerAndConfigurationSettings>()
    private val modulesModel = CollectionListModel<Module>()

    val settingsPanel = panel {
        row {
            cell(additionalJvmParameters)
                .label("Additional JVM Parameters: ", LabelPosition.TOP)
                .align(AlignX.FILL)
        }
        row {
            cell(hotswapAgentCMDArguments)
                .label("Hotswap agent CMD arguments: ", LabelPosition.TOP)
                .align(AlignX.FILL)
        }
        row {
            cell(hotswapProperties)
                .label("Hotswap properties: ", LabelPosition.TOP)
                .align(AlignX.FILL)
        }
        row {
            val configurationList = JBList(configurationsModel)
            configurationList.selectionMode = ListSelectionModel.SINGLE_SELECTION
            configurationList.cellRenderer = ConfigurationListCellRenderer()

            cell(
                ToolbarDecorator.createDecorator(configurationList)
                    .setAddIcon(LayeredIcon.ADD_WITH_DROPDOWN)
                    .setAddAction {
                        val wrappers = mutableListOf<ChooseRunConfigurationPopup.ItemWrapper<*>>()
                        val executor = DefaultDebugExecutor.getDebugExecutorInstance()
                        val allSettings = ChooseRunConfigurationPopup.createSettingsList(project, { executor }, false)
                        val existing = HashSet<RunnerAndConfigurationSettings>(configurationsModel.items)
                        for (setting in allSettings) {
                            val settingValue = setting.value
                            if (settingValue is RunnerAndConfigurationSettings
                                && !settingValue.isTemporary
                                && ProgramRunner.getRunner(executor.id, settingValue.configuration) != null
                                && !existing.contains(settingValue)) {
                                wrappers.add(setting)
                            }
                        }
                        val popup = JBPopupFactory.getInstance()
                            .createPopupChooserBuilder(wrappers)
                            .setRenderer(SimpleListCellRenderer.create { label, value, _ ->
                                label.icon = value.icon
                                label.text = value.text
                            })
                            .setItemChosenCallback { at ->
                                val settings = at.value
                                if (settings is RunnerAndConfigurationSettings) {
                                    configurationsModel.add(settings)
                                }
                            }
                            .createPopup()
                        popup.show(it.preferredPopupPoint)
                    }
                    .disableUpDownActions()
                    .createPanel()
            ).label("Configurations to attach hotSwap Agent to:", LabelPosition.TOP)
                .align(AlignX.FILL)
        }
        row {
            val modulesList = JBList(modulesModel)
            modulesList.selectionMode = ListSelectionModel.SINGLE_SELECTION
            modulesList.cellRenderer = ModulesListCellRenderer()

            cell(
                ToolbarDecorator.createDecorator(modulesList)
                    .setAddIcon(LayeredIcon.ADD_WITH_DROPDOWN)
                    .setAddAction {
                        val allModules = ModuleManager.getInstance(project).sortedModules
                        val modulesInConfiguration = modulesModel.items.toSet()
                        val modulesToShow = mutableListOf<Module>()
                        for (module in allModules) {
                            if (!modulesInConfiguration.contains(module)) {
                                modulesToShow.add(module)
                            }
                        }
                        val popup = JBPopupFactory.getInstance()
                            .createPopupChooserBuilder(modulesToShow)
                            .setRenderer(SimpleListCellRenderer.create { label, value, _ ->
                                label.icon = ModuleType.get(value).icon
                                label.text = value.name
                            })
                            .setItemChosenCallback(modulesModel::add)
                            .createPopup()
                        popup.show(it.preferredPopupPoint)
                    }
                    .disableUpDownActions()
                    .createPanel()
            ).label("Modules with custom hotSwap agent plugins:", LabelPosition.TOP)
                .align(AlignX.FILL)
        }
    }

    val preferredFocusedComponent = additionalJvmParameters

    var additionalJvmParametersText: String
        get() = additionalJvmParameters.text
        set(value) {
            additionalJvmParameters.text = value
        }

    var hotswapAgentCMDArgumentsText: String
        get() = hotswapAgentCMDArguments.text
        set(value) {
            hotswapAgentCMDArguments.text = value
        }

    var hotswapPropertiesText: String
        get() = hotswapProperties.text
        set(value) {
            hotswapProperties.text = value
        }

    fun getConfigurations(): List<RunnerAndConfigurationSettings> {
        return configurationsModel.items
    }

    fun getModules(): List<Module> {
        return modulesModel.items
    }

    fun getConfigurationIds(): Set<String> {
        return configurationsModel.items.asSequence()
            .map(RunnerAndConfigurationSettings::getUniqueID)
            .toSet()
    }

    fun getModuleNames(): Set<String> {
        return modulesModel.items.asSequence()
            .map(Module::getName)
            .toSet()
    }

    fun setConfigurations(configurations: List<RunnerAndConfigurationSettings>) {
        configurationsModel.replaceAll(configurations)
    }

    fun setModules(modules: List<Module>) {
        modulesModel.replaceAll(modules)
    }
}


private class ConfigurationListCellRenderer: ColoredListCellRenderer<RunnerAndConfigurationSettings>() {
    override fun customizeCellRenderer(
        list: JList<out RunnerAndConfigurationSettings>,
        value: RunnerAndConfigurationSettings?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        if (value == null) return
        icon = value.configuration.icon
        append(value.configuration.name)
    }
}

private class ModulesListCellRenderer: ColoredListCellRenderer<Module>() {
    override fun customizeCellRenderer(
        list: JList<out Module>,
        value: Module?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        if (value == null) return
        icon = ModuleType.get(value).icon
        append(value.name)
    }
}