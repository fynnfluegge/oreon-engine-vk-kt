package org.oreon.core.vk.pipeline

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
import org.lwjgl.vulkan.VkShaderModuleCreateInfo
import org.oreon.core.util.ResourceLoader.ioResourceToByteBuffer
import org.oreon.core.vk.util.VkUtil
import java.io.IOException
import java.nio.ByteBuffer

open class ShaderModule(private val device: VkDevice, filePath: String, stage: Int) {

    var shaderStageInfo: VkPipelineShaderStageCreateInfo? = null

    var handle: Long = 0

    private fun createShaderModule(filePath: String) {
        var shaderCode: ByteBuffer? = null
        try {
            shaderCode = ioResourceToByteBuffer(filePath, 1024)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val err: Int
        val moduleCreateInfo = VkShaderModuleCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pNext(0)
                .pCode(shaderCode)
                .flags(0)
        val pShaderModule = MemoryUtil.memAllocLong(1)
        err = VK10.vkCreateShaderModule(device, moduleCreateInfo, null, pShaderModule)
        handle = pShaderModule[0]
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create shader module: " + VkUtil.translateVulkanResult(err))
        }
        MemoryUtil.memFree(pShaderModule)
        moduleCreateInfo.free()
    }

    private fun createShaderStage(stage: Int) {
        shaderStageInfo = VkPipelineShaderStageCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(stage)
                .module(handle)
                .pName(MemoryUtil.memUTF8("main"))
                .pSpecializationInfo(null)
    }

    fun destroy() {
        VK10.vkDestroyShaderModule(device, handle, null)
        handle = -1
    }

    init {
        createShaderModule(filePath)
        createShaderStage(stage)
    }
}