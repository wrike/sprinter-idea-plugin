# The Sprinter Plugin for IntelliJ IDEA
## What problem does it try to solve?

Tests may take some time to initialize before the actual run. 
Depending on a project and the tools you are using, initialization time can be significant — from a few seconds to several minutes.

During local development, you may run tests a lot, and every time you run a test, you have to wait for test initialization. 
This plugin simply allows you to run tests reusing the JVM and its already initialized state, thus excluding test initialization time for sequential runs.

This plugin utilizes the power of hotswapping, so it's recommended to use it with DCEVM. You can see how to configure the plugin for working with DCEVM in the related section called [DCEVM configuration](#dcevm-configuration) 

It is tested with [Spring Framework](https://spring.io), but it should work with any tool that may take some time to initialize itself and can be easily reused afterward, such as [Testcontainers](https://testcontainers.com), for example.

## Supported technologies

Supported test frameworks:
* TestNG
* JUnit

Supported languages:
* Java
* Kotlin

Known issues:
* The plugin will not work if you are using Gradle and if IntelliJ IDEA is configured to run tests using Gradle. 
In this case IDEA will run a test using this command: `gradle test`. 
Unfortunately, I didn't find a way to override test executor when using this command to support plugins functionality.
The workaround is simple – just run test by IntelliJ IDEA's runner instead of gradle's one.

## How does it work?

First of all, you should choose a test you want to run. You will see a new option added in a dropdown menu with run options (see screenshot below).

!["Run in Launched JVM" new option](imgs/test_run_option.png)

Just for example’s sake, let's assume that you are using Spring Framework in your project.

When you click it, IntelliJ IDEA will build your project, start up the JVM process in debug mode with a test executor in it, and send a command to the test executor to start tests you selected. This test executor is a part of the plugin, so you don't have to worry about it.

The test executor will run your tests, which will initialize Spring context before the actual run. Spring is caching its context in a static variable so you don't have to do anything additional to cache it.

When the tests are completed, the JVM is still up and running with the test executor, waiting for the next command to run the tests. When you try to run the tests again, the IDE will perform the hotswap to update the code and send the command to run the following tests. This way, new tests will not waste your time with initialization because Spring context is already there.

You can also look at this work schema:

![Work Schema](imgs/work_schema.png)

## Installation

You can find the Sprinter plugin in the JetBrains Marketplace [here](https://plugins.jetbrains.com/plugin/21623-sprinter). After installing it to your IDE, you are ready to go.

It's recommended to use DCEVM together with this plugin. Get more information here: [DCEVM configuration](#dcevm-configuration).

## DCEVM configuration

DCEVM is a Java HotSpot patch that enables many more cases for hotswapping compared to the default implementation. By default, hotswapping in Java works only with method bodies, which is not enough for comfort development, of course.

It's also recommended to install Hotswap Agent together with DCEVM. It's a Java agent that makes it possible to update the application state when a hotswap is performed. For example, it can update Spring context when the configuration is changed.

You will need to install DCEVM (with Hotswap Agent, if you want). You can follow the [official instructions](http://hotswapagent.org/mydoc_quickstart-jdk17.html).

After that, you should select the installed DCEVM as a project SDK in IntelliJ IDEA, go to the plugin settings (Settings → Tools → Sprinter Settings), and change the configured JVM from Plain to DCEVM and select the Hotswap Agent jar location (if you also installed the Hotswap Agent plugin).

And that's it — you are ready to go!

You may also notice that there are more settings related to DCEVM, but they are not mandatory to fill in.

## More Information
To learn more about the plugins watch the recording of the presentation [here](https://youtu.be/BTnDNLgAbEM) or in russian [here](https://youtu.be/J6CogMdi24E?si=a3yzJ3mGBsPpxNf6).