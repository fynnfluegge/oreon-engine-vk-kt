package org.oreon.core.vk.wrapper.command

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.oreon.core.vk.command.CommandBuffer
import java.nio.ByteBuffer

class SecondaryDrawCmdBuffer : CommandBuffer {
    constructor(device: VkDevice?, commandPool: Long,
                pipeline: Long, pipelineLayout: Long, framebuffer: Long,
                renderpass: Long, subpass: Int, descriptorSets: LongArray,
                vertexBuffer: Long, vertexCount: Int) : super(device!!, commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_SECONDARY) {
        record(pipeline, pipelineLayout, framebuffer,
                renderpass, subpass, descriptorSets,
                vertexBuffer, vertexCount,
                null, -1)
    }

    constructor(device: VkDevice?, commandPool: Long,
                pipeline: Long, pipelineLayout: Long, framebuffer: Long,
                renderpass: Long, subpass: Int, descriptorSets: LongArray,
                vertexBuffer: Long, vertexCount: Int,
                pushConstantsData: ByteBuffer?, pushConstantsStageFlags: Int) : super(device!!, commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_SECONDARY) {
        record(pipeline, pipelineLayout, framebuffer,
                renderpass, subpass, descriptorSets,
                vertexBuffer, vertexCount,
                pushConstantsData, pushConstantsStageFlags)
    }

    private fun record(pipeline: Long, pipelineLayout: Long, framebuffer: Long,
                       renderpass: Long, subpass: Int, descriptorSets: LongArray, vertexBuffer: Long, vertexCount: Int,
                       pushConstantsData: ByteBuffer?, pushConstantsStageFlags: Int) {
        beginRecordSecondary(VK10.VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT,
                framebuffer, renderpass, subpass)
        if (pushConstantsStageFlags != -1) {
            pushConstantsCmd(pipelineLayout, pushConstantsStageFlags, pushConstantsData)
        }
        bindGraphicsPipelineCmd(pipeline)
        bindVertexInputCmd(vertexBuffer)
        bindGraphicsDescriptorSetsCmd(pipelineLayout, descriptorSets)
        drawCmd(vertexCount)
        finishRecord()
    }
}