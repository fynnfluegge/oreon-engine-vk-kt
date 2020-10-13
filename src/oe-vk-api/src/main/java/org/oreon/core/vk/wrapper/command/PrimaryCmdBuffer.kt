package org.oreon.core.vk.wrapper.command

import org.lwjgl.PointerBuffer
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.oreon.core.math.Vec3f
import org.oreon.core.vk.command.CommandBuffer

class PrimaryCmdBuffer(device: VkDevice?, commandPool: Long) : CommandBuffer(device!!, commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY) {
    fun record(renderPass: Long, frameBuffer: Long,
               width: Int, height: Int, colorAttachmentCount: Int, depthAttachment: Int,
               secondaryCmdBuffers: PointerBuffer?) {
        beginRecord(VK10.VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT)
        beginRenderPassCmd(renderPass, frameBuffer, width, height,
                colorAttachmentCount, depthAttachment,
                VK10.VK_SUBPASS_CONTENTS_SECONDARY_COMMAND_BUFFERS)
        secondaryCmdBuffers?.let { recordSecondaryCmdBuffers(it) }
        endRenderPassCmd()
        finishRecord()
    }

    fun record(renderPass: Long, frameBuffer: Long,
               width: Int, height: Int, colorAttachmentCount: Int, depthAttachment: Int,
               clearColor: Vec3f?, secondaryCmdBuffers: PointerBuffer?) {
        beginRecord(VK10.VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT)
        beginRenderPassCmd(renderPass, frameBuffer, width, height,
                colorAttachmentCount, depthAttachment,
                VK10.VK_SUBPASS_CONTENTS_SECONDARY_COMMAND_BUFFERS,
                clearColor)
        secondaryCmdBuffers?.let { recordSecondaryCmdBuffers(it) }
        endRenderPassCmd()
        finishRecord()
    }
}