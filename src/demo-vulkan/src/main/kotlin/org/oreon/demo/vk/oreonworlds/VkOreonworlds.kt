package org.oreon.demo.vk.oreonworlds

import org.oreon.core.context.BaseContext
import org.oreon.core.vk.context.VkContext
import org.oreon.core.vk.scenegraph.VkCamera
import org.oreon.vk.components.atmosphere.Atmosphere
import org.oreon.vk.components.water.Water
import org.oreon.vk.engine.VkDeferredEngine

object VkOreonworlds {
    @JvmStatic
    fun main(args: Array<String>) {
        VkContext.create()
        val renderEngine = VkDeferredEngine()
        renderEngine.setGui(VkSystemMonitor())
        renderEngine.init()
        renderEngine.sceneGraph?.setWater(Water())
        renderEngine.sceneGraph?.addObject(Atmosphere())
//        renderEngine.getSceneGraph().setTerrain(new Planet());

        VkContext.setRenderEngine(renderEngine)
        VkContext.getCoreEngine().start()
    }
}