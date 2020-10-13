package org.oreon.core.vk.wrapper.command

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.oreon.core.vk.command.CommandBuffer
import java.nio.ByteBuffer

class DrawCmdBuffer : CommandBuffer {
    constructor(device: VkDevice?, commandPool: Long,
                pipeline: Long, pipelineLayout: Long, renderPass: Long,
                frameBuffer: Long, width: Int, height: Int,
                colorAttachmentCount: Int, depthAttachment: Int,
                descriptorSets: LongArray, vertexBuffer: Long, indexBuffer: Long, indexCount: Int) : super(device!!, commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY) {
        record(pipeline, pipelineLayout, renderPass, frameBuffer,
                width, height, colorAttachmentCount, depthAttachment,
                descriptorSets, vertexBuffer, indexBuffer, indexCount,
                null, -1)
    }

    constructor(device: VkDevice?, commandPool: Long,
                pipeline: Long, pipelineLayout: Long, renderPass: Long,
                frameBuffer: Long, width: Int, height: Int,
                colorAttachmentCount: Int, depthAttachment: Int,
                descriptorSets: LongArray, vertexBuffer: Long, indexBuffer: Long, indexCount: Int,
                pushConstantsData: ByteBuffer?, pushConstantsStageFlags: Int) : super(device!!, commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY) {
        record(pipeline, pipelineLayout, renderPass, frameBuffer,
                width, height, colorAttachmentCount, depthAttachment,
                descriptorSets, vertexBuffer, indexBuffer, indexCount,
                pushConstantsData, pushConstantsStageFlags)
    }

    private fun record(pipeline: Long, pipelineLayout: Long, renderPass: Long,
                       frameBuffer: Long, width: Int, height: Int,
                       colorAttachmentCount: Int, depthAttachment: Int,
                       descriptorSets: LongArray, vertexBuffer: Long, indexBuffer: Long, indexCount: Int,
                       pushConstantsData: ByteBuffer?, pushConstantsStageFlags: Int) {
        beginRecord(VK10.VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT)
        beginRenderPassCmd(renderPass, frameBuffer, width, height,
                colorAttachmentCount, depthAttachment, VK10.VK_SUBPASS_CONTENTS_INLINE)
        pushConstantsData?.let { pushConstantsCmd(pipelineLayout, pushConstantsStageFlags, it) }
        bindGraphicsPipelineCmd(pipeline)
        bindVertexInputCmd(vertexBuffer, indexBuffer)
        bindGraphicsDescriptorSetsCmd(pipelineLayout, descriptorSets)
        drawIndexedCmd(indexCount)
        endRenderPassCmd()
        finishRecord()
    }
}