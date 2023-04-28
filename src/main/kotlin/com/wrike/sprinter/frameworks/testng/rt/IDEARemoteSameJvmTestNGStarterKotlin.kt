package com.wrike.sprinter.frameworks.testng.rt

import com.beust.jcommander.JCommander
import com.jetbrains.rd.util.printlnError
import org.testng.CommandLineArgs
import java.io.*
import java.net.InetAddress
import java.net.Socket
import kotlin.system.exitProcess

const val socketArgPrefix = "-socket"
const val cantRunMessage = "CantRunException"

class IDEARemoteSameJvmTestNGStarterKotlin {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            kotlinMain(args)
        }
    }
}

fun kotlinMain(args: Array<String>) {
    val port = args.find { it.startsWith(socketArgPrefix) }!!
        .substring(socketArgPrefix.length).toInt()
    while (true) {
        Socket(InetAddress.getByName("127.0.0.1"), port).use { socket ->
            val socketIS = DataInputStream(socket.getInputStream())
            val socketOS = DataOutputStream(socket.getOutputStream())
            val filenameWithTestSuitePath = socketIS.readUTF()
            val xmlSuiteFilenames = mutableListOf<String>()
            BufferedReader(FileReader(File(filenameWithTestSuitePath))).use { fileReader ->
                while (true) {
                    var line = fileReader.readLine()
                    while (line == null) line = fileReader.readLine()

                    if (line.startsWith(cantRunMessage) && !File(line).exists()) {
                        printlnError(line.substring(cantRunMessage.length))
                        while (true) {
                            line = fileReader.readLine()
                            if (line == null || line.equals("end")) break
                            printlnError(line)
                        }
                        exitProcess(1)
                    }
                    if (line.equals("end")) break
                    xmlSuiteFilenames.add(line)
                }
            }

            val testNG = IDEARemoteTestNGAdapted(null)
            val commandLineArgs = CommandLineArgs()
            JCommander(listOf(commandLineArgs)).parse(*xmlSuiteFilenames.toTypedArray())
            testNG.configure(commandLineArgs)
            testNG.run()

            socketOS.writeBoolean(true)
        }
    }
}