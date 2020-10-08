package org.oreon.core.context

import org.oreon.core.CoreEngine
import org.oreon.core.RenderEngine
import org.oreon.core.platform.GLFWInput
import org.oreon.core.platform.Window
import org.oreon.core.scenegraph.Camera

abstract class BaseContext {

    companion object {

        lateinit var config: Config

        lateinit var input: GLFWInput

        lateinit var coreEngine: CoreEngine

        lateinit var renderEngine: RenderEngine

        lateinit var camera: Camera

        lateinit var window: Window

        fun init() {
            config = Config()
            input = GLFWInput()
            coreEngine = CoreEngine()
        }
    }
}