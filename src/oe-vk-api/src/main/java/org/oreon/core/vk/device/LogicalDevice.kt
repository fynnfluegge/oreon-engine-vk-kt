package org.oreon.core.vk.device

import lombok.Getter
import lombok.extern.log4j.Log4j
import mu.KotlinLogging
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.oreon.core.vk.command.CommandPool
import org.oreon.core.vk.descriptor.DescriptorPool
import org.oreon.core.vk.util.VkUtil
import java.util.*

class LogicalDevice(physicalDevice: PhysicalDevice, priority: Float) {

    private val logger = KotlinLogging.logger {}
    val handle: VkDevice
    val graphicsQueue: VkQueue
    var computeQueue: VkQueue? = null
    var transferQueue: VkQueue? = null
    private var graphicsQueueFamilyIndex = 0
    private var computeQueueFamilyIndex = 0
    private var transferQueueFamilyIndex = 0
    private val descriptorPools: HashMap<Long, DescriptorPool>
    private val graphicsCommandPools: HashMap<Long?, CommandPool>
    private val computeCommandPools: HashMap<Long?, CommandPool?>
    private val transferCommandPools: HashMap<Long?, CommandPool?>
    fun getDeviceQueue(queueFamilyIndex: Int, queueIndex: Int): VkQueue {
        val pQueue = MemoryUtil.memAllocPointer(1)
        VK10.vkGetDeviceQueue(handle, queueFamilyIndex, queueIndex, pQueue)
        val queue = pQueue[0]
        MemoryUtil.memFree(pQueue)
        return VkQueue(queue, handle)
    }

    fun destroy() {
        for (key in descriptorPools.keys) {
            descriptorPools[key]!!.destroy()
        }
        for ((_, value) in graphicsCommandPools) {
            value.destroy()
        }
        if (graphicsQueueFamilyIndex != computeQueueFamilyIndex) {
            for ((_, value) in computeCommandPools) {
                value!!.destroy()
            }
        }
        if (graphicsQueueFamilyIndex != transferQueueFamilyIndex) {
            for ((_, value) in transferCommandPools) {
                value!!.destroy()
            }
        }
        VK10.vkDestroyDevice(handle, null)
    }

    fun addDescriptorPool(threadId: Long, descriptorPool: DescriptorPool) {
        descriptorPools[threadId] = descriptorPool
    }

    fun getDescriptorPool(threadId: Long): DescriptorPool? {
        return descriptorPools[threadId]
    }

    fun getGraphicsCommandPool(threadId: Long): CommandPool? {
        return graphicsCommandPools[threadId]
    }

    fun getComputeCommandPool(threadId: Long): CommandPool? {
        return computeCommandPools[threadId]
    }

    fun getTransferCommandPool(threadId: Long): CommandPool? {
        return transferCommandPools[threadId]
    }

    init {
        descriptorPools = HashMap()
        graphicsCommandPools = HashMap()
        computeCommandPools = HashMap()
        transferCommandPools = HashMap()
        val pQueuePriorities = MemoryUtil.memAllocFloat(1).put(priority)
        pQueuePriorities.flip()
        var createInfoCount = 3
        graphicsQueueFamilyIndex = try {
            physicalDevice.queueFamilies.graphicsAndPresentationQueueFamily.index
        } catch (e1: Exception) {
            throw AssertionError("no graphics and presentation queue available on device: " + physicalDevice.properties.deviceNameString())
        }
        computeQueueFamilyIndex = try {
            physicalDevice.queueFamilies.computeExclusiveQueueFamily.index
        } catch (e: Exception) {
            logger.info("No compute exclusive queue available on device: " + physicalDevice.properties.deviceNameString())
            createInfoCount--
            try {
                physicalDevice.queueFamilies.computeQueueFamily.index
            } catch (e1: Exception) {
                throw AssertionError("no compute queue available on device: " + physicalDevice.properties.deviceNameString())
            }
        }
        transferQueueFamilyIndex = try {
            physicalDevice.queueFamilies.transferExclusiveQueueFamily.index
        } catch (e: Exception) {
            logger.info("No transfer exclusive queue available on device: " + physicalDevice.properties.deviceNameString())
            createInfoCount--
            try {
                physicalDevice.queueFamilies.transferQueueFamily.index
            } catch (e1: Exception) {
                throw AssertionError("no transfer queue available on device: " + physicalDevice.properties.deviceNameString())
            }
        }
        val queueCreateInfos = VkDeviceQueueCreateInfo.calloc(createInfoCount)
        val graphicsQueueCreateInfo = VkDeviceQueueCreateInfo.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .queueFamilyIndex(graphicsQueueFamilyIndex)
                .pQueuePriorities(pQueuePriorities)
        var computeQueueCreateInfo = VkDeviceQueueCreateInfo.calloc(1)
        var transferQueueCreateInfo = VkDeviceQueueCreateInfo.calloc(1)
        queueCreateInfos.put(graphicsQueueCreateInfo)
        if (graphicsQueueFamilyIndex != computeQueueFamilyIndex) {
            computeQueueCreateInfo = VkDeviceQueueCreateInfo.calloc(1)
                    .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(computeQueueFamilyIndex)
                    .pQueuePriorities(pQueuePriorities)
            queueCreateInfos.put(computeQueueCreateInfo)
        }
        if (graphicsQueueFamilyIndex != transferQueueFamilyIndex) {
            transferQueueCreateInfo = VkDeviceQueueCreateInfo.calloc(1)
                    .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(transferQueueFamilyIndex)
                    .pQueuePriorities(pQueuePriorities)
            queueCreateInfos.put(transferQueueCreateInfo)
        }
        queueCreateInfos.flip()
        val extensions = MemoryUtil.memAllocPointer(1)
        val VK_KHR_SWAPCHAIN_EXTENSION = MemoryUtil.memUTF8(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME)
        extensions.put(VK_KHR_SWAPCHAIN_EXTENSION)
        extensions.flip()
        physicalDevice.checkDeviceExtensionsSupport(extensions)
        val physicalDeviceFeatures = VkPhysicalDeviceFeatures.calloc()
                .tessellationShader(true)
                .geometryShader(true)
                .shaderClipDistance(true)
                .samplerAnisotropy(true)
                .shaderStorageImageExtendedFormats(true)
                .fillModeNonSolid(true)
                .shaderStorageImageMultisample(true)
        val deviceCreateInfo = VkDeviceCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pNext(VK10.VK_NULL_HANDLE)
                .pQueueCreateInfos(queueCreateInfos)
                .ppEnabledExtensionNames(extensions)
                .pEnabledFeatures(physicalDeviceFeatures)
        val pDevice = MemoryUtil.memAllocPointer(1)
        val err = VK10.vkCreateDevice(physicalDevice.handle, deviceCreateInfo, null, pDevice)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create device: " + VkUtil.translateVulkanResult(err))
        }
        handle = VkDevice(pDevice[0], physicalDevice.handle, deviceCreateInfo)

        // create Queues and CommandPools
        graphicsQueue = getDeviceQueue(graphicsQueueFamilyIndex, 0)
        graphicsCommandPools[Thread.currentThread().id] = CommandPool(handle, graphicsQueueFamilyIndex)
        if (graphicsQueueFamilyIndex == computeQueueFamilyIndex) {
            computeQueue = graphicsQueue
            computeCommandPools[Thread.currentThread().id] = graphicsCommandPools[Thread.currentThread().id]
        } else {
            computeQueue = getDeviceQueue(computeQueueFamilyIndex, 0)
            computeCommandPools[Thread.currentThread().id] = CommandPool(handle, computeQueueFamilyIndex)
        }
        if (graphicsQueueFamilyIndex == transferQueueFamilyIndex) {
            transferQueue = graphicsQueue
            transferCommandPools[Thread.currentThread().id] = graphicsCommandPools[Thread.currentThread().id]
        } else {
            transferQueue = getDeviceQueue(transferQueueFamilyIndex, 0)
            transferCommandPools[Thread.currentThread().id] = CommandPool(handle, transferQueueFamilyIndex)
        }
        deviceCreateInfo.free()
        queueCreateInfos.free()
        graphicsQueueCreateInfo.free()
        computeQueueCreateInfo.free()
        transferQueueCreateInfo.free()
        MemoryUtil.memFree(pDevice)
        MemoryUtil.memFree(pQueuePriorities)
        MemoryUtil.memFree(VK_KHR_SWAPCHAIN_EXTENSION)
        MemoryUtil.memFree(extensions)
    }
}