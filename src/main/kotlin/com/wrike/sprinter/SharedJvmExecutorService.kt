package com.wrike.sprinter

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.debugger.ui.HotSwapStatusListener
import com.intellij.debugger.ui.HotSwapUIImpl
import com.intellij.execution.Executor
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskManager
import com.intellij.ui.content.ContentManager
import com.intellij.util.concurrency.AppExecutorUtil
import com.wrike.sprinter.frameworks.AbstractSharedJvmProcess
import com.wrike.sprinter.frameworks.TestFrameworkId
import com.wrike.sprinter.frameworks.testFrameworkForRunningInSharedJVMExtensionPoint
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

interface SharedJvmExecutorService : Disposable {
    fun saveSharedJvmProcess(
        process: OSProcessHandler,
        serverSocket: ServerSocket,
        executor: Executor,
        testFrameworkId: TestFrameworkId
    )

    fun isSharedJvmProcessExecuted(): Boolean
    fun isSupported(configuration: JavaTestConfigurationBase): Boolean
    fun executeConfiguration(configuration: JavaTestConfigurationBase, context: ConfigurationContext)
    fun getLastExecutedConfiguration(): JavaTestConfigurationBase?
}

private val hotswapStatusListenerKey: Key<HotSwapStatusListener> = run {
    // hack to make it possible to rebuild only required modules and not the whole project before the hotswap
    val hotswapCallBackKeyField = HotSwapUIImpl::class.java.getDeclaredField("HOT_SWAP_CALLBACK_KEY")
    hotswapCallBackKeyField.isAccessible = true
    hotswapCallBackKeyField.get(null) as Key<HotSwapStatusListener>
}

class SharedJvmExecutorServiceImpl(
    private val project: Project
) : SharedJvmExecutorService {
    private var sharedJvmProcess: AbstractSharedJvmProcess? = null
    private var lastExecutedConfiguration: JavaTestConfigurationBase? = null

    override fun saveSharedJvmProcess(process: OSProcessHandler,
                                      serverSocket: ServerSocket,
                                      executor: Executor,
                                      testFrameworkId: TestFrameworkId) {
        check(sharedJvmProcess == null)
        val applicableTestFramework = testFrameworkForRunningInSharedJVMExtensionPoint.extensionList.find {
            it.frameworkId == testFrameworkId
        } ?: throw IllegalStateException("Test framework with id($testFrameworkId) is not found")
        sharedJvmProcess = applicableTestFramework.wrapSharedJvmProcess(process, serverSocket, executor).also {
            process.addProcessListener(object: ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    sharedJvmProcess = null
                }
            }, this)
        }
    }

    override fun isSharedJvmProcessExecuted(): Boolean = sharedJvmProcess != null

    override fun getLastExecutedConfiguration(): JavaTestConfigurationBase? = lastExecutedConfiguration

    override fun isSupported(configuration: JavaTestConfigurationBase): Boolean {
        return testFrameworkForRunningInSharedJVMExtensionPoint.extensionList.any {
            it.canRunTestsFor(configuration)
        }
    }

    override fun executeConfiguration(configuration: JavaTestConfigurationBase, context: ConfigurationContext) {
        if (isSharedJvmProcessExecuted()) {
            val (canRunConfigurationWithoutRestart, allRunningDescriptors) = canRunConfigurationInCurrentJvm(
                project,
                configuration
            )

            if (canRunConfigurationWithoutRestart && sharedJvmProcess!!.supportsConfiguration(configuration)) {
                findExecutorAndExecuteConfiguration(configuration, project)
            } else if (userApprovesStopForCurrentSharedJvm(project)) {
                destroyAllDescriptorsAndDo(allRunningDescriptors) {
                    ApplicationManager.getApplication().runReadAction {
                        startupProcessAndExecuteConfiguration(configuration, context, project)
                    }
                }
            }
        } else {
            startupProcessAndExecuteConfiguration(configuration, context, project)
        }
    }

    private fun canRunConfigurationInCurrentJvm(
        project: Project,
        testsConfiguration: JavaTestConfigurationBase
    ): Pair<Boolean, List<RunContentDescriptor>> {
        val executionManager = ExecutionManagerImpl.getInstance(project)
        val testConfigModules = testsConfiguration.modules.toList()
        val descriptorsWithAppropriateModule = executionManager.getRunningDescriptors { settings ->
            val configuration = settings.configuration
            configuration is SharedJvmConfiguration && configuration.modules.toSet().containsAll(testConfigModules)
        }
        val allDescriptors = executionManager.getRunningDescriptors { settings -> settings.configuration is SharedJvmConfiguration }
        val areRunningDescriptorsAppropriate = descriptorsWithAppropriateModule == allDescriptors
        return Pair(areRunningDescriptorsAppropriate, allDescriptors)
    }

    private fun destroyAllDescriptorsAndDo(descriptors: Collection<RunContentDescriptor>,
                                           action: () -> Unit) {
        val terminatedCounter = AtomicInteger(0)
        descriptors.forEach {
            it.processHandler!!.addProcessListener(object : ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    if (terminatedCounter.incrementAndGet() == descriptors.size) {
                        action()
                    }
                }
            })
            ExecutionManagerImpl.stopProcess(it)
        }
    }

    private fun startupProcessAndExecuteConfiguration(
        configuration: JavaTestConfigurationBase,
        context: ConfigurationContext,
        project: Project,
    ) {
        val configurationProducer = RunConfigurationProducer.getInstance(SharedJvmConfigurationProducer::class.java)
        val sharedJvmConfiguration = configurationProducer.findOrCreateConfigurationFromContext(context) ?: configurationProducer.getConfigurationFromConfigurationToExecute(configuration)
        val runManager = RunManager.getInstance(project)
        val executionEnvironment = ExecutionUtil.createEnvironment(
            DefaultDebugExecutor.getDebugExecutorInstance(),
            sharedJvmConfiguration.configurationSettings
        )
            ?.activeTarget()
            ?.dataContext(context.defaultDataContext)
            ?.build() ?: return
        sharedJvmConfiguration.onFirstRun(context) {
            runManager.setTemporaryConfiguration(sharedJvmConfiguration.configurationSettings)
            if (runManager.shouldSetRunConfigurationFromContext()) {
                runManager.selectedConfiguration = sharedJvmConfiguration.configurationSettings
            }
            ProgramRunnerUtil.executeConfigurationAsync(executionEnvironment, true, true) {
                val debuggerSession = DebuggerManagerEx.getInstanceEx(project).context.debuggerSession
                if (debuggerSession == null || debuggerSession.isStopped) return@executeConfigurationAsync
                val contentManager = debuggerSession.xDebugSession?.ui?.contentManager ?: return@executeConfigurationAsync
                executeConfiguration(configuration, contentManager)
            }
        }
    }

    private fun executeConfiguration(
        configuration: JavaTestConfigurationBase,
        consoleAttacher: ContentManager,
    ) {
        AppExecutorUtil.getAppExecutorService().execute {
            sendExecuteConfigurationSignal(configuration, consoleAttacher)
        }
    }

    private fun findExecutorAndExecuteConfiguration(
        configuration: JavaTestConfigurationBase,
        project: Project,
    ) {
        val debuggerManager = DebuggerManagerEx.getInstanceEx(project)
        val debugSession = debuggerManager.context.debuggerSession
        if (debugSession != null && !debugSession.isStopped) {
            val contentManager = debugSession.xDebugSession?.ui?.contentManager ?: return
            val projectTaskManager = ProjectTaskManager.getInstance(project)
            val buildTask = projectTaskManager.createModulesBuildTask(configuration.modules, true, true, false)
            val buildContext = ProjectTaskContext(buildTask)
                .withUserData(HotSwapUIImpl.SKIP_HOT_SWAP_KEY, false)
                .withUserData(hotswapStatusListenerKey, object: HotSwapStatusListener {
                    override fun onSuccess(sessions: MutableList<DebuggerSession>?) {
                        executeConfiguration(configuration, contentManager)
                    }
                })
            projectTaskManager.run(buildContext, buildTask)
        } else {
            val contentManager = ToolWindowManager.getInstance(project)
                .getToolWindow(ToolWindowId.RUN)?.contentManager ?: return
            executeConfiguration(configuration, contentManager)
        }
    }

    private fun userApprovesStopForCurrentSharedJvm(project: Project): Boolean {
        return MessageDialogBuilder.yesNo(
            "Do you really want to restart jvm?",
            ("Test module or test framework is different from previous runs, " +
                    "so you have to restart JVM to be able to run this tests.").trimIndent(),
            Messages.getQuestionIcon()
        ).ask(project)
    }

    private fun sendExecuteConfigurationSignal(configuration: JavaTestConfigurationBase, consoleAttacher: ContentManager) {
        sharedJvmProcess?.executeConfiguration(configuration, consoleAttacher)
        lastExecutedConfiguration = configuration
    }

    override fun dispose() {}
}