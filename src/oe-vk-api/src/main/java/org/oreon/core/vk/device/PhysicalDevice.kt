package org.oreon.core.vk.device

import mu.KotlinLogging
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.oreon.core.vk.device.DeviceCapabilities.checkPhysicalDeviceFeatures
import org.oreon.core.vk.device.DeviceCapabilities.checkPhysicalDeviceProperties
import org.oreon.core.vk.device.DeviceCapabilities.getPhysicalDeviceExtensionNamesSupport
import org.oreon.core.vk.queue.QueueFamilies
import org.oreon.core.vk.util.VkUtil

class PhysicalDevice(vkInstance: VkInstance?, surface: Long) {

    private val logger = KotlinLogging.logger {}

    var handle: VkPhysicalDevice
    var properties: VkPhysicalDeviceProperties
    var features: VkPhysicalDeviceFeatures
    var memoryProperties: VkPhysicalDeviceMemoryProperties
    var queueFamilies: QueueFamilies
    var swapChainCapabilities: SurfaceProperties
    var supportedExtensionNames: List<String>
    fun checkDeviceExtensionsSupport(ppEnabledExtensionNames: PointerBuffer) {
        for (i in 0 until ppEnabledExtensionNames.limit()) {
            if (!supportedExtensionNames.contains(ppEnabledExtensionNames.stringUTF8)) {
                throw AssertionError("Extension " + ppEnabledExtensionNames.stringUTF8 + " not supported")
            }
        }
        ppEnabledExtensionNames.flip()
    }

    fun checkDeviceFormatAndColorSpaceSupport(format: Int, colorSpace: Int) {
        swapChainCapabilities.checkVkSurfaceFormatKHRSupport(format, colorSpace)
    }

    fun checkDevicePresentationModeSupport(presentMode: Int): Boolean {
        return swapChainCapabilities.checkPresentationModeSupport(presentMode)
    }

    val deviceMinImageCount4TripleBuffering: Int
        get() = swapChainCapabilities.minImageCount4TripleBuffering

    init {
        val pPhysicalDeviceCount = MemoryUtil.memAllocInt(1)
        var err = VK10.vkEnumeratePhysicalDevices(vkInstance, pPhysicalDeviceCount, null)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to get number of physical devices: " + VkUtil.translateVulkanResult(err))
        }
        logger.info("Available Physical Devices: " + pPhysicalDeviceCount[0])
        val pPhysicalDevices = MemoryUtil.memAllocPointer(pPhysicalDeviceCount[0])
        err = VK10.vkEnumeratePhysicalDevices(vkInstance, pPhysicalDeviceCount, pPhysicalDevices)
        val physicalDevice = pPhysicalDevices[0]
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to get physical devices: " + VkUtil.translateVulkanResult(err))
        }
        MemoryUtil.memFree(pPhysicalDeviceCount)
        MemoryUtil.memFree(pPhysicalDevices)
        handle = VkPhysicalDevice(physicalDevice, vkInstance)
        queueFamilies = QueueFamilies(handle, surface)
        swapChainCapabilities = SurfaceProperties(handle, surface)
        supportedExtensionNames = getPhysicalDeviceExtensionNamesSupport(handle)
        memoryProperties = VkPhysicalDeviceMemoryProperties.calloc()
        VK10.vkGetPhysicalDeviceMemoryProperties(handle, memoryProperties)
        properties = checkPhysicalDeviceProperties(handle)
        features = checkPhysicalDeviceFeatures(handle)

//        log.info(properties.apiVersion());
//        log.info(properties.driverVersion());
//        log.info(properties.vendorID());
//        log.info(properties.deviceID());
//        log.info(properties.deviceType());
//        log.info(properties.deviceNameString());
    }
}