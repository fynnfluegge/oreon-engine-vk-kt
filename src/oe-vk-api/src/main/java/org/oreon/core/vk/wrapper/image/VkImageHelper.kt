package org.oreon.core.vk.wrapper.image

import org.lwjgl.vulkan.*
import org.oreon.core.util.Util.getMipLevelCount
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.command.SubmitInfo
import org.oreon.core.vk.image.VkImage
import org.oreon.core.vk.image.VkImageLoader.decodeImage
import org.oreon.core.vk.image.VkImageLoader.getImageMetaData
import org.oreon.core.vk.synchronization.Fence
import org.oreon.core.vk.wrapper.buffer.StagingBuffer
import org.oreon.core.vk.wrapper.command.ImageCopyCmdBuffer
import org.oreon.core.vk.wrapper.command.ImageLayoutTransitionCmdBuffer

object VkImageHelper {
    fun loadImageFromFile(device: VkDevice,
                          memoryProperties: VkPhysicalDeviceMemoryProperties,
                          commandPool: Long, queue: VkQueue, file: String,
                          usage: Int, layout: Int, dstAccesMask: Int, dstStageMask: Int,
                          dstQueueFamilyIndex: Int): VkImage {
        return loadImage(device, memoryProperties, commandPool, queue,
                file, usage, layout, dstAccesMask, dstStageMask,
                dstQueueFamilyIndex, false)
    }

    fun loadImageFromFileMipmap(device: VkDevice,
                                memoryProperties: VkPhysicalDeviceMemoryProperties,
                                commandPool: Long, queue: VkQueue, file: String,
                                usage: Int, layout: Int, dstAccessMask: Int, dstStageMask: Int,
                                dstQueueFamilyIndex: Int): VkImage {
        return loadImage(device, memoryProperties, commandPool, queue,
                file, usage, layout, dstAccessMask, dstStageMask,
                dstQueueFamilyIndex, true)
    }

    private fun loadImage(device: VkDevice,
                          memoryProperties: VkPhysicalDeviceMemoryProperties, commandPool: Long, queue: VkQueue,
                          file: String, usage: Int, finalLayout: Int, dstAccessMask: Int, dstStageMask: Int,
                          dstQueueFamilyIndex: Int, mipmap: Boolean): VkImage {
        val metaData = getImageMetaData(file)
        val imageBuffer = decodeImage(file)
        val stagingBuffer = StagingBuffer(device, memoryProperties, imageBuffer!!)
        val mipLevels = if (mipmap) getMipLevelCount(metaData) else 1
        val image: VkImage = Image2DDeviceLocal(device,
                memoryProperties, metaData.width, metaData.height,
                VK10.VK_FORMAT_R8G8B8A8_UNORM, usage or VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT or  // if mipmap == true, usage flag TRANSFER_SRC_BIT is necessarry for mipmap generation
                if (mipmap) VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT else 0,
                1, mipLevels, metaData)

        // transition layout barrier
        val imageMemoryBarrierLayout0 = ImageLayoutTransitionCmdBuffer(device, commandPool)
        imageMemoryBarrierLayout0.record(image.handle,
                VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT,
                VK10.VK_IMAGE_LAYOUT_UNDEFINED, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                0, VK10.VK_ACCESS_TRANSFER_WRITE_BIT, VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, mipLevels)
        imageMemoryBarrierLayout0.submit(queue)

        // copy buffer to image
        val imageCopyCmd = ImageCopyCmdBuffer(device, commandPool)
        imageCopyCmd.record(stagingBuffer.handle, image.handle, metaData)
        imageCopyCmd.submit(queue)

        // transition layout barrier
        val imageMemoryBarrierLayout1 = ImageLayoutTransitionCmdBuffer(device, commandPool)
        imageMemoryBarrierLayout1.record(image.handle,
                VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT,
                VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, finalLayout,
                VK10.VK_ACCESS_TRANSFER_WRITE_BIT, dstAccessMask,
                VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, dstStageMask, mipLevels)
        val fence = Fence(device)
        imageMemoryBarrierLayout1.submit(queue, fence)
        if (mipmap) {
            generateMipmap(device, commandPool, queue,
                    image.handle, metaData.width, metaData.height, mipLevels,
                    finalLayout, finalLayout,
                    dstAccessMask, dstAccessMask,
                    dstStageMask, dstStageMask,
                    dstQueueFamilyIndex)
        }
        imageMemoryBarrierLayout0.destroy()
        imageMemoryBarrierLayout1.destroy()
        imageCopyCmd.destroy()
        stagingBuffer.destroy()
        fence.destroy()
        return image
    }

    fun generateMipmap(device: VkDevice?, commandPool: Long, queue: VkQueue?,
                       image: Long, width: Int, height: Int, mipLevels: Int,
                       initialLayout: Int, finalLayout: Int,
                       initialSrcAccesMask: Int, finalDstAccesMask: Int,
                       initialSrcStageMask: Int, finalDstStageMask: Int,
                       dstQueueFamilyIndex: Int) {
        val commandBuffer = CommandBuffer(device!!,
                commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
        commandBuffer.beginRecord(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
        val barrier = VkImageMemoryBarrier.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .image(image)
                .oldLayout(initialLayout)
                .newLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                .srcAccessMask(initialSrcAccesMask)
                .dstAccessMask(VK10.VK_ACCESS_TRANSFER_READ_BIT)
        barrier.subresourceRange()
                .baseMipLevel(0)
                .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                .baseArrayLayer(0)
                .layerCount(1)
                .levelCount(1)
        commandBuffer.pipelineImageMemoryBarrierCmd(image,
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
            VK10.vkCmdPipelineBarrier(commandBuffer.handle,
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
            VK10.vkCmdBlitImage(commandBuffer.handle,
                    image, VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    image, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    blit, VK10.VK_FILTER_LINEAR)
            barrier.oldLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .newLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                    .srcAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(VK10.VK_ACCESS_TRANSFER_READ_BIT)
            VK10.vkCmdPipelineBarrier(commandBuffer.handle,
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
                .dstAccessMask(finalDstAccesMask)
                .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
        VK10.vkCmdPipelineBarrier(commandBuffer.handle,
                VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, finalDstStageMask,
                0, null, null, barrier)
        commandBuffer.finishRecord()
        val submitInfo = SubmitInfo()
        submitInfo.setCommandBuffers(commandBuffer.handlePointer)
        submitInfo.submit(queue)
    }
}