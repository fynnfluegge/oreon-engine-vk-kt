package org.oreon.core

import org.oreon.core.context.BaseContext
import org.oreon.core.context.Config
import org.oreon.core.scenegraph.Camera
import org.oreon.core.scenegraph.Scenegraph

abstract class RenderEngine {

    open lateinit var sceneGraph: Scenegraph
    lateinit var config: Config
	lateinit var camera: Camera

    open fun init() {
        sceneGraph = Scenegraph()
        config = BaseContext.config
        camera = BaseContext.camera
        camera.init()
    }

    abstract fun render()

    open fun update() {
        camera.update()
        sceneGraph.update()
        sceneGraph.updateLights()
    }

    open fun shutdown() {

        // important to shutdown scenegraph before render-engine, since
        // thread safety of instancing clusters.
        // scenegraph sets isRunning to false, render-engine signals all
        // waiting threads to shutdown
        sceneGraph.shutdown()
    }
}