package org.oreon.core.vk.platform

import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11
import org.oreon.core.context.BaseContext
import org.oreon.core.platform.Window

class VkWindow : Window(BaseContext.config.displayTitle,
        BaseContext.config.windowWidth, BaseContext.config.windowHeight) {
    override fun create() {
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GL11.GL_TRUE)
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API)
        id = GLFW.glfwCreateWindow(width, height, title, 0, 0)
        if (id == 0L) {
            throw RuntimeException("Failed to create window")
        }
        setIcon("textures/logo/oreon_lwjgl_icon32.png")
    }

    override fun show() {
        GLFW.glfwShowWindow(id)
    }

    override fun draw() {}
    override fun shutdown() {
        GLFW.glfwDestroyWindow(id)
    }

    override val isCloseRequested: Boolean
        get() = GLFW.glfwWindowShouldClose(id)

    override fun resize(x: Int, y: Int) {
        // TODO Auto-generated method stub
    }
}