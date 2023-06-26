package com.wrike.sprinter.settings

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ChooseRunConfigurationPopup
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.jbTextField
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.layout.enteredTextSatisfies
import com.intellij.ui.layout.selectedValueMatches
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.ListSelectionModel

fun createSharedJvmSettingPanel(project: Project): DialogPanel = panel {
    val localSettings = getLocalSprinterSettings(project)
    val sharedSettings = getSharedSprinterSettings(project)
    groupRowsRange("Inherit Configuration Values From Test Configuration", indent = false) {
        row {
            checkBox("Environment variables")
                .align(Align.FILL)
                .bindSelected(sharedSettings::passEnvironmentVariablesFromOriginalConfig)
        }
        row {
            checkBox("System properties")
                .align(Align.FILL)
                .bindSelected(sharedSettings::passSystemPropsFromOriginalConfig)
        }
        row {
            checkBox("Command line arguments")
                .align(Align.FILL)
                .bindSelected(sharedSettings::passCMDArgsFromOriginalConfig)
        }
    }
    lateinit var usedJvmTypePicker: Cell<ComboBox<UsedJvmType>>
    row {
        usedJvmTypePicker = comboBox(UsedJvmType.values().toList())
            .label("Configured JVM type: ", LabelPosition.TOP)
            .align(Align.FILL)
            .bindItem(localSettings::usedJvmType.toNullableProperty())
    }
    groupRowsRange("DCEVM Settings", indent = false) {
        row {
            cell(RawCommandLineEditor())
                .label("Additional JVM Parameters: ", LabelPosition.TOP)
                .align(AlignX.FILL)
                .focused()
                .bind(
                    RawCommandLineEditor::getText,
                    RawCommandLineEditor::setText,
                    MutableProperty(
                        { sharedSettings.additionalJvmParameters },
                        { sharedSettings.additionalJvmParameters = it }
                    )
                )
        }
        row {
            textArea()
                .label("Hotswap properties: ", LabelPosition.TOP)
                .align(AlignX.FILL)
                .bindText(sharedSettings::hotswapProperties)
        }
        row {
            val configurationsWithHAPluginsPicker = ConfigurationsWithHotswapAgentPluginsPicker(project)
            cell(configurationsWithHAPluginsPicker.component)
                .label("Configurations for which DCEVM will be configured:", LabelPosition.TOP)
                .align(AlignX.FILL)
                .onIsModified(configurationsWithHAPluginsPicker::isModified)
                .onReset(configurationsWithHAPluginsPicker::onReset)
                .onApply(configurationsWithHAPluginsPicker::onApply)
        }
        groupRowsRange("Hotswap Agent", indent = false) {
            lateinit var haLocationComponent: TextFieldWithBrowseButton
            row {
                val fileChooserDescriptor = FileChooserDescriptor(
                    false,
                    false,
                    true,
                    false,
                    false,
                    false
                ).withShowFileSystemRoots(true)
                haLocationComponent = textFieldWithBrowseButton(
                    "Select Hotswap Agent Location",
                    project,
                    fileChooserDescriptor
                ).label("Hotswap agent location: ", LabelPosition.TOP)
                    .align(Align.FILL)
                    .bindText(localSettings::hotswapAgentLocation)
                    .component
            }
            panel {
                row {
                    cell(RawCommandLineEditor())
                        .label("Hotswap agent CMD arguments: ", LabelPosition.TOP)
                        .align(AlignX.FILL)
                        .bind(
                            RawCommandLineEditor::getText,
                            RawCommandLineEditor::setText,
                            MutableProperty(
                                { sharedSettings.hotswapAgentCMDArguments },
                                { sharedSettings.hotswapAgentCMDArguments = it }
                            )
                        )
                }
                row {
                    val modulesWithCustomHAPluginsPicker = ModulesWithCustomHAPluginsPicker(project)
                    cell(modulesWithCustomHAPluginsPicker.component)
                        .label("Modules with custom hotswap agent plugins:", LabelPosition.TOP)
                        .align(AlignX.FILL)
                        .onIsModified(modulesWithCustomHAPluginsPicker::isModified)
                        .onApply(modulesWithCustomHAPluginsPicker::onApply)
                        .onReset(modulesWithCustomHAPluginsPicker::onReset)
                }
            }.enabledIf(haLocationComponent.jbTextField.enteredTextSatisfies {
                it.isNotBlank()
            })
        }
    }.visibleIf(usedJvmTypePicker.component.selectedValueMatches { it == UsedJvmType.DCEVM })
}

private class ConfigurationsWithHotswapAgentPluginsPicker(private val project: Project) {
    val configurationsWithHAPluginsService = getConfigurationsWithHotswapAgentService(project)
    val component: JComponent
    val model = CollectionListModel<RunnerAndConfigurationSettings>()

    init {
        val configurationList = JBList(model)
        configurationList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        configurationList.cellRenderer = ConfigurationListCellRenderer()
        component = ToolbarDecorator.createDecorator(configurationList)
            .setAddIcon(LayeredIcon.ADD_WITH_DROPDOWN)
            .setAddAction {
                addConfiguration(it)
            }
            .disableUpDownActions()
            .createPanel()
    }

    fun isModified(): Boolean = model.items.any {
        !configurationsWithHAPluginsService.hasConfiguration(it)
    }

    fun onApply() {
        configurationsWithHAPluginsService.setConfigurations(model.items)
    }

    fun onReset() {
        model.replaceAll(configurationsWithHAPluginsService.getConfigurations())
    }

    private fun addConfiguration(it: AnActionButton) {
        val wrappers = mutableListOf<ChooseRunConfigurationPopup.ItemWrapper<*>>()
        val executor = DefaultDebugExecutor.getDebugExecutorInstance()
        val allSettings = ChooseRunConfigurationPopup.createSettingsList(project, { executor }, false)
        val existing = HashSet<RunnerAndConfigurationSettings>(model.items)
        for (setting in allSettings) {
            val settingValue = setting.value
            if (settingValue is RunnerAndConfigurationSettings
                && !settingValue.isTemporary
                && ProgramRunner.getRunner(executor.id, settingValue.configuration) != null
                && !existing.contains(settingValue)
            ) {
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
                    model.add(settings)
                }
            }
            .createPopup()
        popup.show(it.preferredPopupPoint)
    }
}

private class ModulesWithCustomHAPluginsPicker(private val project: Project) {
    val modulesWithCustomHAPluginsService = getModulesWithHotSwapAgentPluginsService(project)
    val component: JComponent
    val model = CollectionListModel<Module>()

    init {
        val modulesList = JBList(model)
        modulesList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        modulesList.cellRenderer = ModulesListCellRenderer()

        component = ToolbarDecorator.createDecorator(modulesList)
            .setAddIcon(LayeredIcon.ADD_WITH_DROPDOWN)
            .setAddAction {
                addModule(it)
            }
            .disableUpDownActions()
            .createPanel()
    }

    fun isModified(): Boolean = model.items.any {
        !modulesWithCustomHAPluginsService.hasModule(it)
    }

    fun onApply() {
        modulesWithCustomHAPluginsService.setModules(model.items)
    }

    fun onReset() {
        model.replaceAll(modulesWithCustomHAPluginsService.getModules())
    }

    private fun addModule(it: AnActionButton) {
        val allModules = ModuleManager.getInstance(project).sortedModules
        val modulesInConfiguration = model.items.toSet()
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
            .setItemChosenCallback(model::add)
            .createPopup()
        popup.show(it.preferredPopupPoint)
    }
}

private class ConfigurationListCellRenderer : ColoredListCellRenderer<RunnerAndConfigurationSettings>() {
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

private class ModulesListCellRenderer : ColoredListCellRenderer<Module>() {
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