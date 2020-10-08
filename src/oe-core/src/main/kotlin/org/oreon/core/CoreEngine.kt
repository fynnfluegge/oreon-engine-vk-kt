package org.oreon.core

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.oreon.core.context.BaseContext
import org.oreon.core.platform.Input
import org.oreon.core.platform.Window
import org.oreon.core.util.Constants

class CoreEngine {

    private var isRunning = false
    private lateinit var window: Window
    private lateinit var input: Input
    private lateinit var renderEngine: RenderEngine
    private lateinit var errorCallback: GLFWErrorCallback

    private fun init() {
        GLFW.glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err).also { errorCallback = it })
        renderEngine = BaseContext.renderEngine
        window = BaseContext.window
        input = BaseContext.input
        input.create(window.id)
        window.show()
    }

    fun start() {
        init()
        if (isRunning) return
        run()
    }

    fun run() {
        isRunning = true
        var frames = 0
        var frameCounter: Long = 0
        var lastTime = System.nanoTime()
        var unprocessedTime = 0.0

        // Rendering Loop
        while (isRunning) {
            var render = false
            val startTime = System.nanoTime()
            val passedTime = startTime - lastTime
            lastTime = startTime
            unprocessedTime += passedTime / Constants.NANOSECOND.toDouble()
            frameCounter += passedTime
            while (unprocessedTime > frameTime) {
                render = true
                unprocessedTime -= frameTime.toDouble()
                if (BaseContext.window.isCloseRequested) {
                    stop()
                }
                if (frameCounter >= Constants.NANOSECOND) {
                    setFps(frames)
                    currentFrameTime = 1.0f / fps
                    frames = 0
                    frameCounter = 0
                }
            }
            if (render) {
                update()
                render()
                frames++
            } else {
//				try {
//					Thread.sleep(10);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
            }
        }
        shutdown()
    }

    private fun stop() {
        if (!isRunning) return
        isRunning = false
    }

    private fun render() {
        renderEngine.render()
        window.draw()
    }

    private fun update() {
        input.update()
        renderEngine.update()
    }

    private fun shutdown() {
        window.shutdown()
        input.shutdown()
        renderEngine.shutdown()
        errorCallback.free()
        GLFW.glfwTerminate()
    }

    companion object {
        private var fps = 0
        private const val framerate = 1000f
        const val frameTime = 1.0f / framerate
        var currentFrameTime = 0f

        fun getFps(): Int {
            return fps
        }

        fun setFps(fps: Int) {
            Companion.fps = fps
        }
    }
}