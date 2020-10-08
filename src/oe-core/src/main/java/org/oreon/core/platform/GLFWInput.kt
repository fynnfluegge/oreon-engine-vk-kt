package org.oreon.core.platform

import org.lwjgl.glfw.*
import org.oreon.core.math.Vec2f
import java.util.*

class GLFWInput : Input {
    private val pushedKeys: MutableSet<Int> = HashSet()
    private val keysHolding: MutableSet<Int> = HashSet()
    private val releasedKeys: MutableSet<Int> = HashSet()
    private val pushedButtons: MutableSet<Int> = HashSet()
    private val buttonsHolding: MutableSet<Int> = HashSet()
    private val releasedButtons: MutableSet<Int> = HashSet()
    override lateinit var cursorPosition: Vec2f
        private set
    override lateinit var lockedCursorPosition: Vec2f
    override var scrollOffset = 0f
    var isPause = false
    private var keyCallback: GLFWKeyCallback? = null
    private var cursorPosCallback: GLFWCursorPosCallback? = null
    private var mouseButtonCallback: GLFWMouseButtonCallback? = null
    private var scrollCallback: GLFWScrollCallback? = null
    private var framebufferSizeCallback: GLFWFramebufferSizeCallback? = null
    override fun create(window: Long) {
        GLFW.glfwSetFramebufferSizeCallback(window, object : GLFWFramebufferSizeCallback() {
            override fun invoke(window: Long, width: Int, height: Int) {
                // Todo
            }
        }.also { framebufferSizeCallback = it })
        GLFW.glfwSetKeyCallback(window, object : GLFWKeyCallback() {
            override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
                if (action == GLFW.GLFW_PRESS) {
                    if (!pushedKeys.contains(key)) {
                        pushedKeys.add(key)
                        keysHolding.add(key)
                    }
                }
                if (action == GLFW.GLFW_RELEASE) {
                    keysHolding.remove(Integer.valueOf(key))
                    releasedKeys.add(key)
                }
            }
        }.also { keyCallback = it })
        GLFW.glfwSetMouseButtonCallback(window, object : GLFWMouseButtonCallback() {
            override fun invoke(window: Long, button: Int, action: Int, mods: Int) {
                if ((button == 2 || button == 0) && action == GLFW.GLFW_PRESS) {
                    lockedCursorPosition = Vec2f(cursorPosition)
                    GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN)
                }
                if ((button == 2 || button == 0) && action == GLFW.GLFW_RELEASE) {
                    GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL)
                }
                if (action == GLFW.GLFW_PRESS) {
                    if (!pushedButtons.contains(button)) {
                        pushedButtons.add(button)
                        buttonsHolding.add(button)
                    }
                }
                if (action == GLFW.GLFW_RELEASE) {
                    releasedButtons.add(button)
                    buttonsHolding.remove(Integer.valueOf(button))
                }
            }
        }.also { mouseButtonCallback = it })
        GLFW.glfwSetCursorPosCallback(window, object : GLFWCursorPosCallback() {
            override fun invoke(window: Long, xpos: Double, ypos: Double) {
                cursorPosition.x = xpos.toFloat()
                cursorPosition.y = ypos.toFloat()
            }
        }.also { cursorPosCallback = it })
        GLFW.glfwSetScrollCallback(window, object : GLFWScrollCallback() {
            override fun invoke(window: Long, xoffset: Double, yoffset: Double) {
                scrollOffset = yoffset.toFloat()
            }
        }.also { scrollCallback = it })
    }

    override fun update() {
        scrollOffset = 0f
        pushedKeys.clear()
        releasedKeys.clear()
        pushedButtons.clear()
        releasedButtons.clear()
        GLFW.glfwPollEvents()
    }

    override fun shutdown() {
        keyCallback!!.free()
        cursorPosCallback!!.free()
        mouseButtonCallback!!.free()
        scrollCallback!!.free()
        framebufferSizeCallback!!.free()
    }

    override fun isKeyPushed(key: Int): Boolean {
        return pushedKeys.contains(key)
    }

    override fun isKeyReleased(key: Int): Boolean {
        return releasedKeys.contains(key)
    }

    override fun isKeyHolding(key: Int): Boolean {
        return keysHolding.contains(key)
    }

    override fun isButtonPushed(key: Int): Boolean {
        return pushedButtons.contains(key)
    }

    override fun isButtonReleased(key: Int): Boolean {
        return releasedButtons.contains(key)
    }

    override fun isButtonHolding(key: Int): Boolean {
        return buttonsHolding.contains(key)
    }

    fun setCursorPosition(cursorPosition: Vec2f, window: Long) {
        this.cursorPosition = cursorPosition
        GLFW.glfwSetCursorPos(window,
                cursorPosition.x.toDouble(),
                cursorPosition.y.toDouble())
    }

    fun getPushedKeys(): Set<Int> {
        return pushedKeys
    }

    fun getButtonsHolding(): Set<Int> {
        return buttonsHolding
    }

    fun getKeysHolding(): Set<Int> {
        return keysHolding
    }

    fun getPushedButtons(): Set<Int> {
        return pushedButtons
    }

    init {
        cursorPosition = Vec2f()
    }
}