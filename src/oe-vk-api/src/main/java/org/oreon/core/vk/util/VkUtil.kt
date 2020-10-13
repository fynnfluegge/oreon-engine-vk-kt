/**************************************************
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */
package org.oreon.core.vk.util

import mu.KotlinLogging
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.oreon.core.math.Vec3f
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import java.nio.ByteBuffer
import java.nio.LongBuffer

object VkUtil {

    private val logger = KotlinLogging.logger {}

    fun vkCheckResult(err: Int) {
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError(translateVulkanResult(err))
        }
    }

    /**
     * Translates a Vulkan `VkResult` value to a String describing the result.
     *
     * @param result
     * the `VkResult` value
     *
     * @return the result description
     */
    fun translateVulkanResult(result: Int): String {
        return when (result) {
            VK10.VK_SUCCESS -> "Command successfully completed."
            VK10.VK_NOT_READY -> "A fence or query has not yet completed."
            VK10.VK_TIMEOUT -> "A wait operation has not completed in the specified time."
            VK10.VK_EVENT_SET -> "An event is signaled."
            VK10.VK_EVENT_RESET -> "An event is unsignaled."
            VK10.VK_INCOMPLETE -> "A return array was too small for the result."
            KHRSwapchain.VK_SUBOPTIMAL_KHR -> "A swapchain no longer matches the surface properties exactly, but can still be used to present to the surface successfully."
            VK10.VK_ERROR_OUT_OF_HOST_MEMORY -> "A host memory allocation has failed."
            VK10.VK_ERROR_OUT_OF_DEVICE_MEMORY -> "A device memory allocation has failed."
            VK10.VK_ERROR_INITIALIZATION_FAILED -> "Initialization of an object could not be completed for implementation-specific reasons."
            VK10.VK_ERROR_DEVICE_LOST -> "The logical or physical device has been lost."
            VK10.VK_ERROR_MEMORY_MAP_FAILED -> "Mapping of a memory object has failed."
            VK10.VK_ERROR_LAYER_NOT_PRESENT -> "A requested layer is not present or could not be loaded."
            VK10.VK_ERROR_EXTENSION_NOT_PRESENT -> "A requested extension is not supported."
            VK10.VK_ERROR_FEATURE_NOT_PRESENT -> "A requested feature is not supported."
            VK10.VK_ERROR_INCOMPATIBLE_DRIVER -> "The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons."
            VK10.VK_ERROR_TOO_MANY_OBJECTS -> "Too many objects of the type have already been created."
            VK10.VK_ERROR_FORMAT_NOT_SUPPORTED -> "A requested format is not supported on this device."
            KHRSurface.VK_ERROR_SURFACE_LOST_KHR -> "A surface is no longer available."
            KHRSurface.VK_ERROR_NATIVE_WINDOW_IN_USE_KHR -> "The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API."
            KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR -> "A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the swapchain will fail. Applications must query the new surface properties and recreate their swapchain if they wish to continue" + "presenting to the surface."
            KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR -> "The display used by a swapchain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an" + " image."
            EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT -> "A validation layer found an error."
            else -> String.format("%s [%d]", "Unknown", Integer.valueOf(result))
        }
    }

    fun getValidationLayerNames(validation: Boolean, layers: Array<ByteBuffer?>): PointerBuffer {
        val ppEnabledLayerNames = MemoryUtil.memAllocPointer(layers.size)
        var i = 0
        while (validation && i < layers.size) {
            ppEnabledLayerNames.put(layers[i])
            i++
        }
        ppEnabledLayerNames.flip()
        return ppEnabledLayerNames
    }

    fun getClearValueColor(clearColor: Vec3f): VkClearValue {
        val clearValues = VkClearValue.calloc()
        clearValues.color()
                .float32(0, clearColor.x)
                .float32(1, clearColor.y)
                .float32(2, clearColor.z)
                .float32(3, 1.0f)
        return clearValues
    }

    val clearColorValue: VkClearColorValue
        get() {
            val clearValues = VkClearColorValue.calloc()
            clearValues
                    .float32(0, 0.0f)
                    .float32(1, 0.0f)
                    .float32(2, 0.0f)
                    .float32(3, 1.0f)
            return clearValues
        }
    val clearValueDepth: VkClearValue
        get() {
            val clearValues = VkClearValue.calloc()
            clearValues.depthStencil()
                    .depth(1.0f)
            return clearValues
        }

    fun getSampleCountBit(samples: Int): Int {
        var sampleCountBit = 0
        when (samples) {
            1 -> sampleCountBit = VK10.VK_SAMPLE_COUNT_1_BIT
            2 -> sampleCountBit = VK10.VK_SAMPLE_COUNT_2_BIT
            4 -> sampleCountBit = VK10.VK_SAMPLE_COUNT_4_BIT
            8 -> sampleCountBit = VK10.VK_SAMPLE_COUNT_8_BIT
            16 -> sampleCountBit = VK10.VK_SAMPLE_COUNT_16_BIT
            32 -> sampleCountBit = VK10.VK_SAMPLE_COUNT_32_BIT
            64 -> sampleCountBit = VK10.VK_SAMPLE_COUNT_64_BIT
        }
        if (sampleCountBit == 0) {
            logger.error("Multisamplecount: $samples. Allowed numbers [1,2,4,8,16,32,64]")
        }
        return sampleCountBit
    }

    fun createLongArray(descriptorSets: List<DescriptorSet>): LongArray {
        val descriptorSetHandles = LongArray(descriptorSets.size)
        for (i in descriptorSets.indices) {
            descriptorSetHandles[i] = descriptorSets[i].handle
        }
        return descriptorSetHandles
    }

    fun createLongArray(descriptorSet: DescriptorSet): LongArray {
        val descriptorSetHandles = LongArray(1)
        descriptorSetHandles[0] = descriptorSet.handle
        return descriptorSetHandles
    }

    fun createLongBuffer(descriptorSetLayouts: List<DescriptorSetLayout>): LongBuffer {
        if (descriptorSetLayouts.size == 0) {
            logger.error("createLongBuffer: descriptorSetLayouts empty")
        }
        val descriptorSetLayoutsBuffer = MemoryUtil.memAllocLong(descriptorSetLayouts.size)
        for (layout in descriptorSetLayouts) {
            descriptorSetLayoutsBuffer.put(layout.handle)
        }
        descriptorSetLayoutsBuffer.flip()
        return descriptorSetLayoutsBuffer
    }

    fun createPointerBuffer(commandBuffers: Collection<CommandBuffer?>): PointerBuffer {
        if (commandBuffers.size == 0) {
            logger.error("createPointerBuffer: commandBuffers empty")
        }
        val cmdBuffersPointer = MemoryUtil.memAllocPointer(commandBuffers.size)
        for (cmdBuffer in commandBuffers) {
            cmdBuffersPointer.put(cmdBuffer!!.handlePointer)
        }
        cmdBuffersPointer.flip()
        return cmdBuffersPointer
    }
}