package com.wrike.sprinter

import com.intellij.codeInsight.daemon.impl.analysis.JavaModuleGraphUtil
import com.intellij.execution.*
import com.intellij.execution.application.BaseJavaApplicationCommandLineState
import com.intellij.execution.configurations.CompositeParameterTargetedValue
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.JavaRunConfigurationModule
import com.intellij.execution.filters.ArgumentFileFilter
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.target.TargetProgressIndicator
import com.intellij.execution.util.JavaParametersUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.JdkUtil
import com.intellij.openapi.projectRoots.ex.JavaSdkUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.impl.light.LightJavaModule
import com.intellij.rt.testng.RemoteTestNGStarter
import com.intellij.util.PathUtil
import com.wrike.sprinter.frameworks.TestFrameworkId
import com.wrike.sprinter.frameworks.testng.rt.IDEARemoteSameJvmTestNGStarterKotlin
import com.wrike.sprinter.settings.getSharedJvmSettings
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class SharedJvmRunnableState(
    environment: ExecutionEnvironment,
    configuration: SharedJvmConfiguration,
    private val testFrameworkId: TestFrameworkId
) : BaseJavaApplicationCommandLineState<SharedJvmConfiguration>(environment, configuration) {
    private val log = Logger.getInstance(SharedJvmRunnableState::class.java)

    private lateinit var targetBoundServerSocket: TargetBoundServerSocket

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
        val serverSocket = targetBoundServerSocket.bind(remoteEnvironment)
        val process = KillableColoredProcessHandler.Silent(
            remoteEnvironment.createProcess(commandLine, EmptyProgressIndicator()),
            commandLine.getCommandPresentation(remoteEnvironment),
            commandLine.charset,
            targetedCommandLineBuilder.filesToDeleteOnTermination
        )

        environment.project.getService(SharedJvmExecutorService::class.java)
            .saveSharedJvmProcess(process, serverSocket, executor, testFrameworkId)

        val content = targetedCommandLineBuilder.getUserData(JdkUtil.COMMAND_LINE_CONTENT)
        content?.forEach { (key: String?, value: String?) ->
            addConsoleFilters(
                ArgumentFileFilter(key, value)
            )
        }
        ProcessTerminatedListener.attach(process)
        JavaRunConfigurationExtensionManager.instance.attachExtensionsToProcess(configuration, process, runnerSettings)
        return process
    }

    override fun createJavaParameters(): JavaParameters {
        val parameters = JavaParameters()

        parameters.mainClass = myConfiguration.runClass
        setupJavaParameters(parameters)

        val module = configuration.configurationModule
        ReadAction.run<CantRunException> {
            val jreHome = if (targetEnvironmentRequest == null && configuration.isAlternativeJrePathEnabled) {
                configuration.alternativeJrePath
            } else null
            if (module.module != null) {
                DumbService.getInstance(module.project).runWithAlternativeResolveEnabled<CantRunException> {
                    val classPathType = JavaParametersUtil.getClasspathType(
                        module, configuration.runClass, false, true
                    )
                    JavaParametersUtil.configureModule(module, parameters, classPathType, jreHome)
                }
            } else {
                JavaParametersUtil.configureProject(module.project, parameters, JavaParameters.JDK_AND_CLASSES_AND_TESTS, jreHome)
            }
        }

        setupModulePath(parameters, module)

        parameters.classPath.addFirst(PathUtil.getJarPathForClass(IDEARemoteSameJvmTestNGStarterKotlin::class.java))
        parameters.classPath.addFirst(PathUtil.getJarPathForClass(RemoteTestNGStarter::class.java))
        parameters.classPath.addFirst(PathUtil.getJarPathForClass(ULong::class.java))
        parameters.classPath.addFirst(JavaSdkUtil.getIdeaRtJarPath())
        createTemporaryFolderWithHotswapAgentProperties()?.let(parameters.classPath::addFirst)

        parameters.setShortenCommandLine(configuration.shortenCommandLine, configuration.project)

        createServerSocket(parameters)

        getHotswapAgentJavaArgumentsProvider(environment.project).addArguments(parameters)

        JavaRunConfigurationExtensionManager.instance.updateJavaParameters(configuration, parameters, runnerSettings, environment.executor)

        return parameters
    }

    private fun setupModulePath(parameters: JavaParameters, module: JavaRunConfigurationModule) {
        if (JavaSdkUtil.isJdkAtLeast(parameters.jdk, JavaSdkVersion.JDK_1_9)) {
            val dumbService = DumbService.getInstance(module.project)
            val mainModule = ReadAction.compute<PsiJavaModule?, Throwable> {
                dumbService.computeWithAlternativeResolveEnabled<PsiJavaModule?, Throwable> {
                    JavaModuleGraphUtil.findDescriptorByElement(module.findClass(parameters.mainClass))
                }
            }
            if (mainModule != null) {
                val inLibrary = mainModule is PsiCompiledElement || mainModule is LightJavaModule
                if (!inLibrary
                    || ReadAction.compute<PsiJavaModule?, Throwable> {
                        JavaModuleGraphUtil.findNonAutomaticDescriptorByModule(
                            module.module,
                            false
                        )
                    } != null
                ) {
                    parameters.moduleName = ReadAction.compute<String, Throwable> { mainModule.name }
                    dumbService.runReadActionInSmartMode {
                        JavaParametersUtil.putDependenciesOnModulePath(parameters, mainModule, false)
                    }
                }
            }
        }
    }

    private fun createServerSocket(parameters: JavaParameters) {
        try {
            targetBoundServerSocket = TargetBoundServerSocket.fromRequest(targetEnvironmentRequest)
            parameters.programParametersList.add(
                CompositeParameterTargetedValue("-socket").addTargetPart(
                    targetBoundServerSocket.localPort.toString(),
                    targetBoundServerSocket.hostPortPromise
                )
            )
        } catch (e: IOException) {
            log.error(e)
        }
    }

    private fun createTemporaryFolderWithHotswapAgentProperties(): File? {
        val hotswapProperties = getSharedJvmSettings(environment.project).hotswapProperties
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
}

