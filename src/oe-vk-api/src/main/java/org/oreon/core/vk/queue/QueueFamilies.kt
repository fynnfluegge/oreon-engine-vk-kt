package org.oreon.core.vk.queue

import mu.KotlinLogging
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.KHRSurface
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkQueueFamilyProperties
import org.oreon.core.vk.util.VkUtil
import java.util.*

class QueueFamilies(physicalDevice: VkPhysicalDevice?, surface: Long) {

    private val logger = KotlinLogging.logger {}

    private val queueFamilies: MutableList<QueueFamily>

    @get:Throws(Exception::class)
    val graphicsQueueFamily: QueueFamily
        get() {
            for (queueFamily in queueFamilies) {
                if (queueFamily.flags and VK10.VK_QUEUE_GRAPHICS_BIT != 0) return queueFamily
            }
            throw Exception("No Queue with graphics support found")
        }

    @get:Throws(Exception::class)
    val computeQueueFamily: QueueFamily
        get() {
            for (queueFamily in queueFamilies) {
                if (queueFamily.flags and VK10.VK_QUEUE_COMPUTE_BIT != 0) return queueFamily
            }
            throw Exception("No Queue with compute support found")
        }

    @get:Throws(Exception::class)
    val transferQueueFamily: QueueFamily
        get() {
            for (queueFamily in queueFamilies) {
                if (queueFamily.flags and VK10.VK_QUEUE_TRANSFER_BIT != 0) return queueFamily
            }
            throw Exception("No Queue with transfer support found")
        }

    @get:Throws(Exception::class)
    val sparseBindingQueueFamily: QueueFamily
        get() {
            for (queueFamily in queueFamilies) {
                if (queueFamily.flags and VK10.VK_QUEUE_SPARSE_BINDING_BIT != 0) return queueFamily
            }
            throw Exception("No Queue with sparse binding support found")
        }

    @get:Throws(Exception::class)
    val presentationQueueFamily: QueueFamily
        get() {
            for (queueFamily in queueFamilies) {
                if (queueFamily.presentFlag == VK10.VK_TRUE) return queueFamily
            }
            throw Exception("No Queue with presentation support found")
        }

    @get:Throws(Exception::class)
    val graphicsAndPresentationQueueFamily: QueueFamily
        get() {
            for (queueFamily in queueFamilies) {
                if (queueFamily.flags and VK10.VK_QUEUE_GRAPHICS_BIT != 0
                        && queueFamily.presentFlag == VK10.VK_TRUE) return queueFamily
            }
            throw Exception("No Queue with both graphics and presentation support found")
        }

    @get:Throws(Exception::class)
    val computeExclusiveQueueFamily: QueueFamily
        get() {
            for (queueFamily in queueFamilies) {
                if (queueFamily.flags == VK10.VK_QUEUE_COMPUTE_BIT) return queueFamily
            }
            throw Exception("No Queue with compute exclusive support found")
        }

    @get:Throws(Exception::class)
    val transferExclusiveQueueFamily: QueueFamily
        get() {
            for (queueFamily in queueFamilies) {
                if (queueFamily.flags == VK10.VK_QUEUE_TRANSFER_BIT) return queueFamily
            }
            throw Exception("No Queue transfer exclusive support found")
        }

    init {
        queueFamilies = ArrayList()
        val pQueueFamilyPropertyCount = MemoryUtil.memAllocInt(1)
        VK10.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyPropertyCount, null)
        val queueCount = pQueueFamilyPropertyCount[0]
        val queueProps = VkQueueFamilyProperties.calloc(queueCount)
        VK10.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyPropertyCount, queueProps)
        logger.info("Available Queues: $queueCount")
        val supportsPresent = MemoryUtil.memAllocInt(queueCount)
        for (i in 0 until queueCount) {
            supportsPresent.position(i)
            supportsPresent.put(i, 0)
            val flags = queueProps[i].queueFlags()
            val count = queueProps[i].queueCount()

            // check if surface exists
            if (surface != -1L) {
                val err = KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice,
                        i,
                        surface,
                        supportsPresent)
                if (err != VK10.VK_SUCCESS) {
                    throw AssertionError("Failed to physical device surface support: " + VkUtil.translateVulkanResult(err))
                }
            }
            logger.info("Index:" + i + " flags:" + flags + " count:" + count + " presentation:" + supportsPresent[i])
            val queueFamily = QueueFamily()
            queueFamily.index = i
            queueFamily.flags = flags
            queueFamily.count = count
            queueFamily.presentFlag = supportsPresent[i]
            queueFamilies.add(queueFamily)
        }
        MemoryUtil.memFree(pQueueFamilyPropertyCount)
        queueProps.free()
    }
}