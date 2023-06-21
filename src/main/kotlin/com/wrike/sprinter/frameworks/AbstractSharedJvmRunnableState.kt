package com.wrike.sprinter.frameworks

import com.intellij.execution.*
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ParametersList
import com.intellij.execution.filters.ArgumentFileFilter
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.target.TargetProgressIndicator
import com.intellij.execution.testframework.TestSearchScope
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.projectRoots.JdkUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.PathUtil
import com.wrike.sprinter.SharedJvmConfiguration
import com.wrike.sprinter.SharedJvmExecutorService
import com.wrike.sprinter.getHotswapAgentJavaArgumentsProvider
import com.wrike.sprinter.settings.getSharedSprinterSettings
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

abstract class AbstractSharedJvmRunnableState<C: JavaTestConfigurationBase, F: TestFrameworkForRunningInSharedJVM>(
    environment: ExecutionEnvironment,
    protected val sharedJvmConfiguration: SharedJvmConfiguration,
    protected val initialTestConfiguration: C,
    protected val testFramework: F
): JavaTestFrameworkRunnableState<SharedJvmConfiguration>(environment) {
    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        val process = startProcess(executor)
        val console = createConsole(executor)
        console?.attachToProcess(process)
        return DefaultExecutionResult(console, process, *createActions(console, process, executor))
    }

    private fun startProcess(executor: Executor): OSProcessHandler {
        val remoteEnvironment = environment.getPreparedTargetEnvironment(this, TargetProgressIndicator.EMPTY)
        val targetedCommandLineBuilder = targetedCommandLine
        val commandLine = targetedCommandLineBuilder.build()
        resolveServerSocketPort(remoteEnvironment)
        val serverSocket = serverSocket!!
        val process = KillableColoredProcessHandler.Silent(
            remoteEnvironment.createProcess(commandLine, EmptyProgressIndicator()),
            commandLine.getCommandPresentation(remoteEnvironment),
            commandLine.charset,
            targetedCommandLineBuilder.filesToDeleteOnTermination
        )

        environment.project.getService(SharedJvmExecutorService::class.java)
            .saveSharedJvmProcess(process, serverSocket, executor, testFramework)

        val content = targetedCommandLineBuilder.getUserData(JdkUtil.COMMAND_LINE_CONTENT)
        content?.forEach { (key: String, value: String) ->
            addConsoleFilters(ArgumentFileFilter(key, value))
        }
        ProcessTerminatedListener.attach(process)
        JavaRunConfigurationExtensionManager.instance.attachExtensionsToProcess(configuration, process, runnerSettings)
        return process
    }

    override fun createJavaParameters(): JavaParameters {
        val parameters = super.createJavaParameters()

        parameters.mainClass = mainClassName

        createServerSocket(parameters)

        createTemporaryFolderWithHotswapAgentProperties()?.let(parameters.classPath::addFirst)
        getHotswapAgentJavaArgumentsProvider(environment.project).addArguments(parameters)
        JavaRunConfigurationExtensionManager.instance.updateJavaParameters(configuration, parameters, runnerSettings, environment.executor)

        return parameters
    }

    protected abstract val mainClassName: String

    override fun configureRTClasspath(parameters: JavaParameters, module: Module) {
        parameters.classPath.addFirst(PathUtil.getJarPathForClass(ULong::class.java))
    }

    private fun createTemporaryFolderWithHotswapAgentProperties(): File? {
        val hotswapProperties = getSharedSprinterSettings(environment.project).hotswapProperties
        if (hotswapProperties.isBlank()) return null
        val tempDir = FileUtil.createTempDirectory("hotswap", null, true)
        FileUtilRt.doIOOperation<Path, Throwable> {
            Files.writeString(
                tempDir.toPath().resolve("hotswap-agent.properties"),
                hotswapProperties,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE
            )
        }
        return tempDir
    }

    override fun getConfiguration(): SharedJvmConfiguration {
        return sharedJvmConfiguration
    }

    override fun getFrameworkName(): String = "SharedJvmRunner"
    override fun getFrameworkId(): String = "SharedJvmRunner"
    override fun getForkMode(): String = "none"

    override fun passTempFile(parameters: ParametersList, tempFilePath: String?) {
        throw IllegalStateException("Not expected to be called")
    }

    override fun getScope(): TestSearchScope? = null
    override fun passForkMode(forkMode: String, tempFile: File, parameters: JavaParameters) {
        throw IllegalStateException("Not expected to be called")
    }
}