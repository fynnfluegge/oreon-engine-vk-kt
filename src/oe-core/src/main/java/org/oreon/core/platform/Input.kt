package org.oreon.core.platform

import org.oreon.core.math.Vec2f

interface Input {
    fun create(windowId: Long)
    fun update()
    fun shutdown()
    fun isKeyPushed(key: Int): Boolean
    fun isKeyHolding(key: Int): Boolean
    fun isKeyReleased(key: Int): Boolean
    fun isButtonPushed(key: Int): Boolean
    fun isButtonHolding(key: Int): Boolean
    fun isButtonReleased(key: Int): Boolean
    val scrollOffset: Float
    val cursorPosition: Vec2f
    val lockedCursorPosition: Vec2f
}