package com.wrike.sprinter.frameworks.junit.rt

import com.intellij.rt.junit.IdeaTestRunner
import com.wrike.sprinter.frameworks.testng.rt.socketArgPrefix
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket


class SameJvmJUnitStarter {
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
        Socket("127.0.0.1", port).use { socket ->
            val socketIS = DataInputStream(socket.getInputStream())
            val socketOS = DataOutputStream(socket.getOutputStream())
            val testArguments = socketIS.readUTF().lines()
            val filteredTestArguments = testArguments.filter { !it.startsWith("-") }

            val testRunner = createTestRunner(testArguments)

            IdeaTestRunner.Repeater.startRunnerWithArgs(
                testRunner,
                filteredTestArguments.toTypedArray(),
                ArrayList(),
                null,
                1,
                true
            )

            socketOS.writeBoolean(true)
        }
    }
}

private const val junitVersionArgName = "-junit"
private const val junit3RunnerClass = "com.intellij.junit3.JUnit3IdeaTestRunner"
private const val junit4RunnerClass = "com.intellij.junit4.JUnit4IdeaTestRunner"
private const val junit5RunnerClass = "com.intellij.junit5.JUnit5IdeaTestRunner"
fun createTestRunner(args: List<String>): IdeaTestRunner<*> {
    val junitVersion = args.find { it.startsWith(junitVersionArgName) }
        ?.run { substring(junitVersionArgName.length).toInt() }
        ?: throw IllegalStateException("Junit version is not found")

    if (junitVersion == 5) {
        return Class.forName(junit5RunnerClass)
            .getDeclaredConstructor()
            .newInstance() as IdeaTestRunner<*>
    }

    val resolvedClassName = when (junitVersion) {
        3 -> try {
            Class.forName("org.junit.runner.Computer")
            junit4RunnerClass
        } catch (e: ClassNotFoundException) {
            junit3RunnerClass
        }
        4 -> try {
            Class.forName("org.junit.Test")
            junit4RunnerClass
        } catch (e: ClassNotFoundException) {
            junit3RunnerClass
        }
        else -> throw IllegalStateException("Unexpected junit version: $junitVersion")
    }

    return Class.forName(resolvedClassName)
        .getDeclaredConstructor()
        .newInstance() as IdeaTestRunner<*>
}