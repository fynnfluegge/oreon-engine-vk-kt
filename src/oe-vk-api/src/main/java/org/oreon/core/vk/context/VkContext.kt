package org.oreon.core.vk.context

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.oreon.core.CoreEngine
import org.oreon.core.RenderEngine
import org.oreon.core.context.BaseContext
import org.oreon.core.vk.context.DeviceManager.DeviceType
import org.oreon.core.vk.descriptor.DescriptorPool
import org.oreon.core.vk.device.LogicalDevice
import org.oreon.core.vk.device.PhysicalDevice
import org.oreon.core.vk.device.VkDeviceBundle
import org.oreon.core.vk.platform.VkWindow
import org.oreon.core.vk.scenegraph.VkCamera
import org.oreon.core.vk.util.VkUtil
import java.nio.ByteBuffer

object VkContext : BaseContext() {

    lateinit var enabledLayers: Array<ByteBuffer>

    lateinit var vkInstance: VulkanInstance

    lateinit var resources: VkResources

    lateinit var deviceManager: DeviceManager

    var surface: Long = 0

    fun create() {

        init()
        BaseContext.Companion.window = VkWindow()
        resources = VkResources()
        BaseContext.Companion.camera = VkCamera()
        deviceManager = DeviceManager()
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }
        if (!GLFWVulkan.glfwVulkanSupported()) {
            throw AssertionError("GLFW failed to find the Vulkan loader")
        }
        val layers = arrayOf(
                MemoryUtil.memUTF8("VK_LAYER_LUNARG_standard_validation") //            	memUTF8("VK_LAYER_LUNARG_assistant_layer")
        )
        enabledLayers = layers
        vkInstance = VulkanInstance(
                VkUtil.getValidationLayerNames(
                        config.vkValidation,
                        layers))
        window!!.create()
        val pSurface = MemoryUtil.memAllocLong(1)
        val err = GLFWVulkan.glfwCreateWindowSurface(vkInstance.handle, window.id, null, pSurface)
        surface = pSurface[0]
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create surface: " + VkUtil.translateVulkanResult(err))
        }
        val physicalDevice = PhysicalDevice(vkInstance!!.handle, surface)
        val logicalDevice = LogicalDevice(physicalDevice, 0f)
        val majorDevice = VkDeviceBundle(physicalDevice, logicalDevice)
        VkContext.deviceManager?.addDevice(DeviceType.MAJOR_GRAPHICS_DEVICE, majorDevice)
        val descriptorPool = DescriptorPool(
                majorDevice.logicalDevice.handle, 4)
        descriptorPool.addPoolSize(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 33)
        descriptorPool.addPoolSize(VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE, 61)
        descriptorPool.addPoolSize(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, 2)
        descriptorPool.addPoolSize(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 12)
        descriptorPool.create()
        majorDevice.logicalDevice.addDescriptorPool(Thread.currentThread().id, descriptorPool)
    }

    fun getCamera() : VkCamera {
        return camera as VkCamera;
    }

    fun getCoreEngine() : CoreEngine {
        return coreEngine;
    }

    fun getRenderEngine() : RenderEngine {
        return renderEngine;
    }

    fun setRenderEngine(x : RenderEngine) {
        renderEngine = x;
    }
}