package org.oreon.demo.vk.oreonworlds

import com.sun.management.OperatingSystemMXBean
import org.oreon.common.ui.UIScreen
import org.oreon.core.CoreEngine
import org.oreon.core.math.Vec4f
import org.oreon.core.vk.image.VkImageView
import org.oreon.vk.components.ui.*
import java.lang.management.ManagementFactory
import java.nio.LongBuffer

class VkSystemMonitor : VkGUI() {
    private var bean: OperatingSystemMXBean? = null
    override fun init(imageView: VkImageView, waitSemaphores: LongBuffer?){
        super.init(imageView, waitSemaphores)
        bean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        val screen0 = UIScreen()
        screen0.elements.add(VkColorPanel(Vec4f(0f, 0f, 0f, 0.5f), 0, 215, 325, 225,
                panelMeshBuffer, guiOverlayFbo))
        screen0.elements.add(VkStaticTextPanel("FPS:", 20, 45, 40, 40,
                fontsImageBundle.imageView, fontsImageBundle.sampler, guiOverlayFbo))
        screen0.elements.add(VkStaticTextPanel("CPU:", 20, 90, 40, 40,
                fontsImageBundle.imageView, fontsImageBundle.sampler, guiOverlayFbo))
        screen0.elements.add(VkDynamicTextPanel("000", 120, 45, 40, 40,
                fontsImageBundle.imageView, fontsImageBundle.sampler, guiOverlayFbo))
        screen0.elements.add(VkDynamicTextPanel("000", 120, 90, 40, 40,
                fontsImageBundle.imageView, fontsImageBundle.sampler, guiOverlayFbo))
        screen0.elements.add(VkTexturePanel("textures/logo/Vulkan_Logo.png", 0, 220, 310, 130,
                panelMeshBuffer, guiOverlayFbo))
        screens.add(screen0)
    }

    override fun update() {
        screens[0].elements[3].update(Integer.toString(CoreEngine.getFps()))
        var cpuLoad = java.lang.Double.toString(bean!!.systemCpuLoad)
        cpuLoad = if (cpuLoad.length == 3) {
            cpuLoad.substring(2, 3)
        } else {
            cpuLoad.substring(2, 4)
        }
        screens[0].elements[4].update("$cpuLoad%")
    }
}