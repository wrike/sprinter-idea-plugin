package com.wrike.sprinter.frameworks.testng

import com.intellij.execution.testframework.SearchForTestsTask
import com.intellij.openapi.diagnostic.Logger
import com.theoryinpractice.testng.configuration.SearchingForTestsTask
import com.theoryinpractice.testng.configuration.TestNGConfiguration
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.ServerSocket

class DontCloseServerSocketOnFinishSearchForTestNGTestsTask(
    serverSocket: ServerSocket,
    configuration: TestNGConfiguration,
    private val tempFile: File
): SearchingForTestsTask(serverSocket, configuration, tempFile) {
    private val log = Logger.getInstance(SearchForTestsTask::class.java)

    override fun finish() {
        sendInfoAboutTestsAndWaitUntilTestsAreFinished()
    }

    private fun sendInfoAboutTestsAndWaitUntilTestsAreFinished() {
        try {
            if (mySocket == null || mySocket.isClosed) return
            val socketOS = DataOutputStream(mySocket.getOutputStream())
            val socketIS = DataInputStream(mySocket.getInputStream())
            socketOS.writeUTF(tempFile.absolutePath)
            socketIS.readBoolean()
        } catch (e: Throwable) {
            log.info(e)
        } finally {
            try {
                mySocket.close()
            } catch (e: Throwable) {
                log.info(e)
            }
        }
    }
}