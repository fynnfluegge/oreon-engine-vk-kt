package org.oreon.core.vk.wrapper.command

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.oreon.core.vk.command.CommandBuffer
import java.nio.ByteBuffer

class SecondaryDrawIndexedCmdBuffer : CommandBuffer {
    constructor(device: VkDevice?, commandPool: Long) : super(device!!, commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_SECONDARY) {}
    constructor(device: VkDevice?, commandPool: Long,
                pipeline: Long, pipelineLayout: Long, framebuffer: Long,
                renderpass: Long, subpass: Int, descriptorSets: LongArray?,
                vertexBuffer: Long, indexBuffer: Long, indexCount: Int) : super(device!!, commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_SECONDARY) {
        record(pipeline, pipelineLayout, framebuffer,
                renderpass, subpass, descriptorSets,
                vertexBuffer, indexBuffer, indexCount,
                null, -1)
    }

    constructor(device: VkDevice?, commandPool: Long,
                pipeline: Long, pipelineLayout: Long, framebuffer: Long,
                renderpass: Long, subpass: Int, descriptorSets: LongArray?,
                vertexBuffer: Long, indexBuffer: Long, indexCount: Int,
                pushConstantsData: ByteBuffer?, pushConstantsStageFlags: Int) : super(device!!, commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_SECONDARY) {
        record(pipeline, pipelineLayout, framebuffer,
                renderpass, subpass, descriptorSets,
                vertexBuffer, indexBuffer, indexCount,
                pushConstantsData, pushConstantsStageFlags)
    }

    fun record(pipeline: Long, pipelineLayout: Long, framebuffer: Long,
               renderpass: Long, subpass: Int, descriptorSets: LongArray?, vertexBuffer: Long,
               indexBuffer: Long, indexCount: Int,
               pushConstantsData: ByteBuffer?, pushConstantsStageFlags: Int) {
        beginRecordSecondary(VK10.VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT,
                framebuffer, renderpass, subpass)
        if (pushConstantsStageFlags != -1) {
            pushConstantsCmd(pipelineLayout, pushConstantsStageFlags, pushConstantsData)
        }
        bindGraphicsPipelineCmd(pipeline)
        bindVertexInputCmd(vertexBuffer, indexBuffer)
        descriptorSets?.let { bindGraphicsDescriptorSetsCmd(pipelineLayout, it) }
        drawIndexedCmd(indexCount)
        finishRecord()
    }
}