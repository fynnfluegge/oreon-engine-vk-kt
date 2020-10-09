package org.oreon.core.vk.device

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.oreon.core.vk.util.VkUtil
import java.nio.IntBuffer

class SurfaceProperties(physicalDevice: VkPhysicalDevice?, surface: Long) {

    val surfaceCapabilities: VkSurfaceCapabilitiesKHR = VkSurfaceCapabilitiesKHR.calloc()
    private val surfaceFormats: VkSurfaceFormatKHR.Buffer
    private val presentModes: IntBuffer

    fun checkVkSurfaceFormatKHRSupport(format: Int, colorSpace: Int) {
        if (surfaceFormats[0].format() == VK10.VK_FORMAT_UNDEFINED) {
            // surface has no format restrictions
            return
        }
        for (i in 0 until surfaceFormats.limit()) {
            if (surfaceFormats[i].format() == format
                    && surfaceFormats[i].colorSpace() == colorSpace) {
                return
            }
        }
        throw AssertionError("Desired format and colorspace not supported")
    }

    fun checkPresentationModeSupport(presentMode: Int): Boolean {
        for (i in 0 until presentModes.limit()) {
            if (presentModes[i] == presentMode) {
                return true
            }
        }
        return false
    }

    val minImageCount4TripleBuffering: Int
        get() {
            var minImageCount = surfaceCapabilities.minImageCount() + 1
            if (surfaceCapabilities.maxImageCount() > 0 && minImageCount > surfaceCapabilities.maxImageCount()) {
                minImageCount = surfaceCapabilities.maxImageCount()
            }
            return minImageCount
        }

    init {
        var err = KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, surfaceCapabilities)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to get physical device surface capabilities: " + VkUtil.translateVulkanResult(err))
        }
        val pFormatCount = MemoryUtil.memAllocInt(1)
        err = KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, pFormatCount, null)
        val formatCount = pFormatCount[0]
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to query number of physical device surface formats: " + VkUtil.translateVulkanResult(err))
        }
        surfaceFormats = VkSurfaceFormatKHR.calloc(formatCount)
        err = KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, pFormatCount, surfaceFormats)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to query physical device surface formats: " + VkUtil.translateVulkanResult(err))
        }
        val pPresentModeCount = MemoryUtil.memAllocInt(1)
        err = KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, pPresentModeCount, null)
        val presentModeCount = pPresentModeCount[0]
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to get number of physical device surface presentation modes: " + VkUtil.translateVulkanResult(err))
        }
        presentModes = MemoryUtil.memAllocInt(presentModeCount)
        err = KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, pPresentModeCount, presentModes)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to get physical device surface presentation modes: " + VkUtil.translateVulkanResult(err))
        }
        MemoryUtil.memFree(pPresentModeCount)
        MemoryUtil.memFree(pFormatCount)
    }
}