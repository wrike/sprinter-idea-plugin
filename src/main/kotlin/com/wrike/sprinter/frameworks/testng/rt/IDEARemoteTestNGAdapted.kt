package com.wrike.sprinter.frameworks.testng.rt

import com.intellij.rt.testng.*
import org.testng.CommandLineArgs
import org.testng.TestNG
import org.testng.xml.XmlInclude
import org.testng.xml.XmlSuite

class IDEARemoteTestNGAdapted(
    private val param: String? = null
) : TestNG() {
    private fun calculateAllSuites(suites: List<XmlSuite>, outSuites: MutableList<XmlSuite>) {
        suites.forEach {
            outSuites.add(it)
            calculateAllSuites(it.childSuites, outSuites)
        }
    }

    public override fun configure(cla: CommandLineArgs?) {
        super.configure(cla)
    }

    override fun run() {
        try {
            initializeSuitesAndJarFile()

            val suites = mutableListOf<XmlSuite>()
            calculateAllSuites(m_suites, suites)
            if (suites.size > 0) {
                for (suite in suites) {
                    val tests = suite.tests
                    for (test in tests) {
                        try {
                            if (param != null) {
                                for (aClass in test.xmlClasses) {
                                    val includes: MutableList<XmlInclude> = ArrayList()
                                    for (include in aClass.includedMethods) {
                                        includes.add(XmlInclude(include.name, listOf(param.toInt()), 0))
                                    }
                                    aClass.includedMethods = includes
                                }
                            }
                        } catch (e: NumberFormatException) {
                            System.err.println("Invocation number: expected integer but found: $param")
                        }
                    }
                }

                attachListeners(IDEATestNGRemoteListener())
                super.run()
            } else {
                println("##teamcity[enteredTheMatrix]")
                System.err.println("Nothing found to run")
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun attachListeners(listener: IDEATestNGRemoteListener) {
        addListener(IDEATestNGSuiteListener(listener))
        addListener(IDEATestNGTestListener(listener))
        addListener(IDEATestNGInvokedMethodListener(listener))
        val configListener = IDEATestNGConfigurationListener(listener)
        addListener(configListener)
        configListener.setIgnoreStarted()
    }
}