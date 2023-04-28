package com.wrike.sprinter.frameworks

import com.intellij.execution.process.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import java.io.IOException
import java.io.OutputStream

class FakelyTerminatedProcessHandler(
    private val process: OSProcessHandler
): ProcessHandler() {
    private val inputStream = FakelyClosedOutputStream(process.processInput)
    private val listeners = mutableListOf<OnlyOnTextAvailableListener>()

    override fun destroyProcessImpl() {
        inputStream.flush()
        inputStream.close()
        notifyProcessTerminated(0)
    }

    override fun detachProcessImpl() {
        inputStream.flush()
        inputStream.close()
        notifyProcessDetached()
    }

    override fun detachIsDefault(): Boolean = false

    override fun getExitCode(): Int? = if (isProcessTerminated) 0 else null

    override fun getProcessInput(): OutputStream = inputStream

    override fun addProcessListener(listener: ProcessListener) {
        super.addProcessListener(listener)
        val wrappedListener = OnlyOnTextAvailableListener(listener)
        listeners.add(wrappedListener)
        process.addProcessListener(wrappedListener)
    }

    override fun addProcessListener(listener: ProcessListener, parentDisposable: Disposable) {
        super.addProcessListener(listener, parentDisposable)
        val wrappedListener = OnlyOnTextAvailableListener(listener)
        listeners.add(wrappedListener)
        Disposer.register(parentDisposable) { listeners.remove(wrappedListener) }
        process.addProcessListener(wrappedListener, parentDisposable)
    }

    override fun removeProcessListener(listener: ProcessListener) {
        super.removeProcessListener(listener)
        val wrappedListener = listeners.find { it.delegateListener == listener }
        if (wrappedListener != null) {
            listeners.remove(wrappedListener)
            process.removeProcessListener(wrappedListener)
        }
    }
}

class FakelyClosedOutputStream(
    private val outputStream: OutputStream
): OutputStream() {
    private var isClosed: Boolean = false
    override fun write(b: Int) {
        if (isClosed) throw IOException()
        outputStream.write(b)
    }

    override fun write(b: ByteArray) {
        if (isClosed) throw IOException()
        outputStream.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        if (isClosed) throw IOException()
        outputStream.write(b, off, len)
    }

    override fun flush() {
        if (isClosed) throw IOException()
        outputStream.flush()
    }

    override fun close() {
        isClosed = true
    }
}

class OnlyOnTextAvailableListener(
    val delegateListener: ProcessListener
): ProcessAdapter() {
    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        delegateListener.onTextAvailable(event, outputType)
    }
}