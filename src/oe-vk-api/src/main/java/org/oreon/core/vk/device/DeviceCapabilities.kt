package org.oreon.core.vk.device

import mu.KotlinLogging
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.oreon.core.vk.util.VkUtil
import java.nio.IntBuffer
import java.util.*

object DeviceCapabilities {

    private val logger = KotlinLogging.logger {}

    fun checkInstanceExtensionSupport(ppEnabledExtensionNames: PointerBuffer) {
        val extensionCount = MemoryUtil.memAllocInt(1)
        var err = VK10.vkEnumerateInstanceExtensionProperties("", extensionCount, null)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError(VkUtil.translateVulkanResult(err))
        }
        val extensions = VkExtensionProperties.calloc(extensionCount[0])
        err = VK10.vkEnumerateInstanceExtensionProperties("", extensionCount, extensions)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError(VkUtil.translateVulkanResult(err))
        }
        val availableExtensions: MutableList<String> = ArrayList()
        for (extension in extensions) {
            availableExtensions.add(extension.extensionNameString())
        }
        for (i in 0 until ppEnabledExtensionNames.limit()) {
            if (!availableExtensions.contains(ppEnabledExtensionNames.getStringUTF8(i))) {
                throw AssertionError("Extension " + ppEnabledExtensionNames.getStringUTF8(i) + " not supported")
            }
        }
        MemoryUtil.memFree(extensionCount)
        extensions.free()
    }

    fun getPhysicalDeviceExtensionNamesSupport(physicalDevice: VkPhysicalDevice?): List<String> {
        val extensionCount = MemoryUtil.memAllocInt(1)
        var err = VK10.vkEnumerateDeviceExtensionProperties(physicalDevice, "", extensionCount, null)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError(VkUtil.translateVulkanResult(err))
        }
        val extensions = VkExtensionProperties.calloc(extensionCount[0])
        err = VK10.vkEnumerateDeviceExtensionProperties(physicalDevice, "", extensionCount, extensions)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError(VkUtil.translateVulkanResult(err))
        }
        val extensionNames: MutableList<String> = ArrayList()
        for (extension in extensions) {
            extensionNames.add(extension.extensionNameString())
        }
        return extensionNames
    }

    fun checkValidationLayerSupport(ppEnabledLayerNames: PointerBuffer) {
        val layerCount = MemoryUtil.memAllocInt(1)
        var err = VK10.vkEnumerateInstanceLayerProperties(layerCount, null)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError(VkUtil.translateVulkanResult(err))
        }
        val layers = VkLayerProperties.calloc(layerCount[0])
        err = VK10.vkEnumerateInstanceLayerProperties(layerCount, layers)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError(VkUtil.translateVulkanResult(err))
        }
        val availableLayers: MutableList<String> = ArrayList()
        for (layer in layers) {
            availableLayers.add(layer.layerNameString())
        }
        for (i in 0 until ppEnabledLayerNames.limit()) {
            if (!availableLayers.contains(ppEnabledLayerNames.stringUTF8)) {
                throw AssertionError("Extension " + ppEnabledLayerNames.stringUTF8 + " not supported")
            }
        }
        ppEnabledLayerNames.flip()
        MemoryUtil.memFree(layerCount)
        layers.free()
    }

    fun checkPhysicalDeviceProperties(physicalDevice: VkPhysicalDevice?): VkPhysicalDeviceProperties {
        val properties = VkPhysicalDeviceProperties.create()
        VK10.vkGetPhysicalDeviceProperties(physicalDevice, properties)
        logger.info("Device: " + properties.deviceNameString())
        return properties
    }

    fun checkPhysicalDeviceFeatures(physicalDevice: VkPhysicalDevice?): VkPhysicalDeviceFeatures {
        val features = VkPhysicalDeviceFeatures.create()
        VK10.vkGetPhysicalDeviceFeatures(physicalDevice, features)
        return features
    }

    fun getVkPhysicalDeviceFormatProperties(physicalDevice: VkPhysicalDevice?,
                                            format: Int): VkFormatProperties {
        val formatProperties = VkFormatProperties.create()
        VK10.vkGetPhysicalDeviceFormatProperties(physicalDevice, format, formatProperties)
        return formatProperties
    }

    fun getMemoryTypeIndex(memoryProperties: VkPhysicalDeviceMemoryProperties,
                           memoryTypeBits: Int,
                           properties: Int,
                           memoryTypeIndex: IntBuffer): Boolean {
        for (i in 0 until memoryProperties.memoryTypeCount()) {
            if (memoryTypeBits and (1 shl i) != 0 &&
                    memoryProperties.memoryTypes(i).propertyFlags() and properties == properties) {
                memoryTypeIndex.put(0, i)
                return true
            }
        }
        return false
    }
}