package org.oreon.core.platform

import org.oreon.core.util.ResourceLoader.loadImageToByteBuffer
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.glfw.GLFW

abstract class Window(var title: String, var width: Int, var height: Int) {
    var id: Long = 0
    abstract fun create()
    abstract fun show()
    abstract fun draw()
    abstract fun shutdown()
    abstract val isCloseRequested: Boolean
    abstract fun resize(x: Int, y: Int)
    fun setIcon(path: String?) {
        val bufferedImage = loadImageToByteBuffer(path)
        val image = GLFWImage.malloc()
        image[32, 32] = bufferedImage
        val images = GLFWImage.malloc(1)
        images.put(0, image)
        GLFW.glfwSetWindowIcon(id, images)
    }
}