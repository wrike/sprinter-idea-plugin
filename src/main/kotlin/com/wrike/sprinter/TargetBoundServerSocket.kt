package com.wrike.sprinter

import com.intellij.execution.ExecutionException
import com.intellij.execution.target.TargetEnvironment
import com.intellij.execution.target.TargetEnvironmentRequest
import com.intellij.execution.target.local.LocalTargetEnvironment
import com.intellij.util.net.NetUtils
import org.jetbrains.concurrency.AsyncPromise
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket

class TargetBoundServerSocket private constructor(
    private val localPortBinding: TargetEnvironment.LocalPortBinding?,
    val localPort: Int
) {
    companion object {
        fun fromRequest(targetEnvironmentRequest: TargetEnvironmentRequest?): TargetBoundServerSocket {
            val serverPort = NetUtils.findAvailableSocketPort()
            return if (targetEnvironmentRequest != null) {
                val localPortBinding = TargetEnvironment.LocalPortBinding(serverPort, null)
                targetEnvironmentRequest.localPortBindings.add(localPortBinding)
                TargetBoundServerSocket(localPortBinding)
            } else {
                TargetBoundServerSocket(serverPort)
            }
        }
    }

    constructor(localPort: Int) : this(null, localPort)
    constructor(localPortBinding: TargetEnvironment.LocalPortBinding) : this(localPortBinding, localPortBinding.local)

    val hostPortPromise = AsyncPromise<String>()

    fun bind(environment: TargetEnvironment): ServerSocket {
        val hostPort: String
        val serverSocket: ServerSocket
        try {
            val serverHost: String
            if (environment is LocalTargetEnvironment) {
                serverHost = "127.0.0.1"
                hostPort = localPort.toString()
            } else {
                val resolvedPortBinding = environment.localPortBindings[localPortBinding]!!
                serverHost = resolvedPortBinding.localEndpoint.host
                val targetHostPort = resolvedPortBinding.targetEndpoint
                hostPort = targetHostPort.host + ":" + targetHostPort.port
            }
            serverSocket = ServerSocket(localPort, 0, InetAddress.getByName(serverHost))
        } catch (e: IOException) {
            throw ExecutionException(e)
        }
        hostPortPromise.setResult(hostPort)
        return serverSocket
    }
}