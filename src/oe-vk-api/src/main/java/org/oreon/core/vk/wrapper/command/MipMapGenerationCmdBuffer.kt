package org.oreon.core.vk.wrapper.command

import org.lwjgl.vulkan.*
import org.oreon.core.vk.command.CommandBuffer

class MipMapGenerationCmdBuffer(device: VkDevice?, commandPool: Long,
                                image: Long, width: Int, height: Int, mipLevels: Int,
                                initialLayout: Int, initialSrcAccessMask: Int, initialSrcStageMask: Int,
                                finalLayout: Int, finalDstAccessMask: Int, finalDstStageMask: Int) : CommandBuffer(device!!, commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY) {
    init {
        beginRecord(VK10.VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT)
        val barrier = VkImageMemoryBarrier.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .image(image)
                .oldLayout(initialLayout)
                .newLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                .srcAccessMask(initialSrcAccessMask)
                .dstAccessMask(VK10.VK_ACCESS_TRANSFER_READ_BIT)
        barrier.subresourceRange()
                .baseMipLevel(0)
                .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                .baseArrayLayer(0)
                .layerCount(1)
                .levelCount(1)
        pipelineImageMemoryBarrierCmd(image,
                initialSrcStageMask,
                VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                barrier)
        var mipWidth = width
        var mipHeight = height
        for (i in 1 until mipLevels) {
            barrier.oldLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
                    .newLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .srcAccessMask(0)
                    .dstAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT)
            barrier.subresourceRange()
                    .baseMipLevel(i)
            VK10.vkCmdPipelineBarrier(handle,
                    VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                    0, null, null, barrier)
            val src0_offset3D = VkOffset3D.calloc()
                    .x(0).y(0).z(0)
            val src1_offset3D = VkOffset3D.calloc()
                    .x(mipWidth).y(mipHeight).z(1)
            val dst0_offset3D = VkOffset3D.calloc()
                    .x(0).y(0).z(0)
            val dst1_offset3D = VkOffset3D.calloc()
                    .x(mipWidth / 2).y(mipHeight / 2).z(1)
            val blit = VkImageBlit.calloc(1)
                    .srcOffsets(0, src0_offset3D)
                    .srcOffsets(1, src1_offset3D)
                    .dstOffsets(0, dst0_offset3D)
                    .dstOffsets(1, dst1_offset3D)
            blit.srcSubresource()
                    .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(i - 1)
                    .baseArrayLayer(0)
                    .layerCount(1)
            blit.dstSubresource()
                    .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(i)
                    .baseArrayLayer(0)
                    .layerCount(1)
            VK10.vkCmdBlitImage(handle,
                    image, VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    image, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    blit, VK10.VK_FILTER_LINEAR)
            barrier.oldLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .newLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                    .srcAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(VK10.VK_ACCESS_TRANSFER_READ_BIT)
            VK10.vkCmdPipelineBarrier(handle,
                    VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                    0, null, null, barrier)
            if (mipWidth > 1) mipWidth /= 2
            if (mipHeight > 1) mipHeight /= 2
        }
        barrier.subresourceRange()
                .baseMipLevel(0)
                .levelCount(mipLevels)
        barrier.oldLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                .newLayout(finalLayout)
                .srcAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(finalDstAccessMask)
                .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
        VK10.vkCmdPipelineBarrier(handle,
                VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, finalDstStageMask,
                0, null, null, barrier)
        finishRecord()
    }
}