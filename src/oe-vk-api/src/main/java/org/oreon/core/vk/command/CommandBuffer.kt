package org.oreon.core.vk.command

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.oreon.core.math.Vec3f
import org.oreon.core.vk.util.VkUtil
import java.nio.ByteBuffer

open class CommandBuffer(private val device: VkDevice, private val commandPool: Long, level: Int) {

    val handle: VkCommandBuffer
    val handlePointer: PointerBuffer

    fun beginRecord(flags: Int) {
        val beginInfo = VkCommandBufferBeginInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .pNext(0)
                .flags(flags)
        val err = VK10.vkBeginCommandBuffer(handle, beginInfo)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to begin record command buffer: "
                    + VkUtil.translateVulkanResult(err))
        }
        beginInfo.free()
    }

    fun beginRecordSecondary(flags: Int, framebuffer: Long,
                             renderPass: Long, subpass: Int) {
        val inheritanceInfo = VkCommandBufferInheritanceInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO)
                .pNext(0)
                .framebuffer(framebuffer)
                .renderPass(renderPass)
                .subpass(subpass)
                .occlusionQueryEnable(false)
                .queryFlags(0)
                .pipelineStatistics(0)
        val beginInfo = VkCommandBufferBeginInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .pNext(0)
                .flags(flags)
                .pInheritanceInfo(inheritanceInfo)
        val err = VK10.vkBeginCommandBuffer(handle, beginInfo)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to begin record command buffer: "
                    + VkUtil.translateVulkanResult(err))
        }
        beginInfo.free()
    }

    fun finishRecord() {
        val err = VK10.vkEndCommandBuffer(handle)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to finish record command buffer: "
                    + VkUtil.translateVulkanResult(err))
        }
    }

    fun beginRenderPassCmd(renderPass: Long, frameBuffer: Long,
                           width: Int, height: Int, colorAttachmentCount: Int, depthAttachment: Int,
                           contentsFlag: Int) {
        val clearValues = VkClearValue.calloc(
                colorAttachmentCount + depthAttachment)
        for (i in 0 until colorAttachmentCount) {
            clearValues.put(VkUtil.getClearValueColor(Vec3f(0f, 0f, 0f)))
        }
        if (depthAttachment == 1) {
            clearValues.put(VkUtil.clearValueDepth)
        }
        clearValues.flip()
        beginRenderPassCmd(renderPass, frameBuffer, width, height,
                contentsFlag, clearValues)
        clearValues.free()
    }

    fun beginRenderPassCmd(renderPass: Long, frameBuffer: Long,
                           width: Int, height: Int, colorAttachmentCount: Int, depthAttachment: Int,
                           contentsFlag: Int, clearColor: Vec3f?) {
        val clearValues = VkClearValue.calloc(
                colorAttachmentCount + depthAttachment)
        for (i in 0 until colorAttachmentCount) {
            clearValues.put(clearColor?.let { VkUtil.getClearValueColor(it) })
        }
        if (depthAttachment == 1) {
            clearValues.put(VkUtil.clearValueDepth)
        }
        clearValues.flip()
        beginRenderPassCmd(renderPass, frameBuffer, width, height,
                contentsFlag, clearValues)
        clearValues.free()
    }

    private fun beginRenderPassCmd(renderPass: Long, frameBuffer: Long,
                                   width: Int, height: Int, flags: Int, clearValues: VkClearValue.Buffer) {
        val renderPassBeginInfo = VkRenderPassBeginInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .pNext(0)
                .renderPass(renderPass)
                .pClearValues(clearValues)
                .framebuffer(frameBuffer)
        val renderArea = renderPassBeginInfo.renderArea()
        renderArea.offset()[0] = 0
        renderArea.extent()[width] = height
        VK10.vkCmdBeginRenderPass(handle, renderPassBeginInfo, flags)
        renderPassBeginInfo.free()
    }

    fun endRenderPassCmd() {
        VK10.vkCmdEndRenderPass(handle)
    }

    fun bindComputePipelineCmd(pipeline: Long) {
        VK10.vkCmdBindPipeline(handle, VK10.VK_PIPELINE_BIND_POINT_COMPUTE, pipeline)
    }

    fun bindGraphicsPipelineCmd(pipeline: Long) {
        VK10.vkCmdBindPipeline(handle, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline)
    }

    fun viewPortCmd() {

        // TODO
    }

    fun scissorCmd() {

        // TODO
    }

    fun pushConstantsCmd(pipelineLayout: Long, stageFlags: Int, data: ByteBuffer?) {
        VK10.vkCmdPushConstants(handle,
                pipelineLayout,
                stageFlags,
                0,
                data)
    }

    fun bindVertexInputCmd(vertexBuffer: Long, indexBuffer: Long) {
        val offsets = MemoryUtil.memAllocLong(1)
        offsets.put(0, 0L)
        val pVertexBuffers = MemoryUtil.memAllocLong(1)
        pVertexBuffers.put(0, vertexBuffer)
        VK10.vkCmdBindVertexBuffers(handle, 0, pVertexBuffers, offsets)
        VK10.vkCmdBindIndexBuffer(handle, indexBuffer, 0, VK10.VK_INDEX_TYPE_UINT32)
        MemoryUtil.memFree(pVertexBuffers)
        MemoryUtil.memFree(offsets)
    }

    fun bindVertexInputCmd(vertexBuffer: Long) {
        val offsets = MemoryUtil.memAllocLong(1)
        offsets.put(0, 0L)
        val pVertexBuffers = MemoryUtil.memAllocLong(1)
        pVertexBuffers.put(0, vertexBuffer)
        VK10.vkCmdBindVertexBuffers(handle, 0, pVertexBuffers, offsets)
        MemoryUtil.memFree(pVertexBuffers)
        MemoryUtil.memFree(offsets)
    }

    fun bindComputeDescriptorSetsCmd(pipelinyLayout: Long, descriptorSets: LongArray) {
        bindDescriptorSetsCmd(pipelinyLayout, descriptorSets,
                VK10.VK_PIPELINE_BIND_POINT_COMPUTE)
    }

    fun bindGraphicsDescriptorSetsCmd(pipelinyLayout: Long, descriptorSets: LongArray) {
        bindDescriptorSetsCmd(pipelinyLayout, descriptorSets,
                VK10.VK_PIPELINE_BIND_POINT_GRAPHICS)
    }

    private fun bindDescriptorSetsCmd(pipelinyLayout: Long, descriptorSets: LongArray,
                                      pipelineBindPoint: Int) {
        VK10.vkCmdBindDescriptorSets(handle, pipelineBindPoint,
                pipelinyLayout, 0, descriptorSets, null)
    }

    fun clearColorImageCmd(image: Long, imageLayout: Int) {
        val subresourceRange = VkImageSubresourceRange.calloc()
                .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1)
        VK10.vkCmdClearColorImage(handle, image, imageLayout,
                VkUtil.clearColorValue, subresourceRange)
    }

    fun drawIndexedCmd(indexCount: Int) {
        VK10.vkCmdDrawIndexed(handle, indexCount, 1, 0, 0, 0)
    }

    fun drawCmd(vertexCount: Int) {
        VK10.vkCmdDraw(handle, vertexCount, 1, 0, 0)
    }

    fun dispatchCmd(groupCountX: Int, groupCountY: Int, groupCountZ: Int) {
        VK10.vkCmdDispatch(handle, groupCountX, groupCountY, groupCountZ)
    }

    fun copyBufferCmd(srcBuffer: Long, dstBuffer: Long,
                      srcOffset: Long, dstOffset: Long,
                      size: Long) {
        val copyRegion = VkBufferCopy.calloc(1)
                .srcOffset(srcOffset)
                .dstOffset(dstOffset)
                .size(size)
        VK10.vkCmdCopyBuffer(handle, srcBuffer, dstBuffer, copyRegion)
    }

    fun copyBufferToImageCmd(srcBuffer: Long, dstImage: Long, width: Int, height: Int, depth: Int) {
        val copyRegion = VkBufferImageCopy.calloc(1)
                .bufferOffset(0)
                .bufferRowLength(0)
                .bufferImageHeight(0)
        val subresource = VkImageSubresourceLayers.calloc()
                .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                .mipLevel(0)
                .baseArrayLayer(0)
                .layerCount(1)
        val extent = VkExtent3D.calloc()
                .width(width)
                .height(height)
                .depth(depth)
        val offset = VkOffset3D.calloc()
                .x(0)
                .y(0)
                .z(0)
        copyRegion.imageSubresource(subresource)
        copyRegion.imageExtent(extent)
        copyRegion.imageOffset(offset)
        VK10.vkCmdCopyBufferToImage(handle, srcBuffer, dstImage,
                VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copyRegion)
    }

    fun pipelineImageMemoryBarrierCmd(image: Long, oldLayout: Int, newLayout: Int,
                                      srcAccessMask: Int, dstAccessMask: Int, srcStageMask: Int, dstStageMask: Int,
                                      baseMipLevel: Int, mipLevelCount: Int) {
        val barrier = VkImageMemoryBarrier.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .oldLayout(oldLayout)
                .newLayout(newLayout)
                .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .image(image)
                .srcAccessMask(srcAccessMask)
                .dstAccessMask(dstAccessMask)
        barrier.subresourceRange()
                .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(baseMipLevel)
                .levelCount(mipLevelCount)
                .baseArrayLayer(0)
                .layerCount(1)
        VK10.vkCmdPipelineBarrier(handle, srcStageMask, dstStageMask,
                VK10.VK_DEPENDENCY_BY_REGION_BIT, null, null, barrier)
        barrier.free()
    }

    fun pipelineImageMemoryBarrierCmd(image: Long, srcStageMask: Int, dstStageMask: Int,
                                      barrier: VkImageMemoryBarrier.Buffer?) {
        VK10.vkCmdPipelineBarrier(handle, srcStageMask, dstStageMask,
                VK10.VK_DEPENDENCY_BY_REGION_BIT, null, null, barrier)
    }

    fun pipelineMemoryBarrierCmd(srcAccessMask: Int, dstAccessMask: Int,
                                 srcStageMask: Int, dstStageMask: Int) {
        val barrier = VkMemoryBarrier.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_MEMORY_BARRIER)
                .srcAccessMask(srcAccessMask)
                .dstAccessMask(dstAccessMask)
        VK10.vkCmdPipelineBarrier(handle, srcStageMask, dstStageMask,
                VK10.VK_DEPENDENCY_BY_REGION_BIT, barrier, null, null)
    }

    fun pipelineBarrierCmd(srcStageMask: Int, dstStageMask: Int) {
        VK10.vkCmdPipelineBarrier(handle, srcStageMask, dstStageMask,
                VK10.VK_DEPENDENCY_BY_REGION_BIT, null, null, null)
    }

    fun recordSecondaryCmdBuffers(secondaryCmdBuffers: PointerBuffer?) {
        VK10.vkCmdExecuteCommands(handle, secondaryCmdBuffers)
    }

    fun reset() {
        VK10.vkResetCommandBuffer(handle, VK10.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT)
    }

    fun destroy() {
        VK10.vkFreeCommandBuffers(device, commandPool, handlePointer)
    }

    init {
        val cmdBufAllocateInfo = VkCommandBufferAllocateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(commandPool)
                .level(level)
                .commandBufferCount(1)
        handlePointer = MemoryUtil.memAllocPointer(1)
        val err = VK10.vkAllocateCommandBuffers(device, cmdBufAllocateInfo, handlePointer)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to allocate command buffer: "
                    + VkUtil.translateVulkanResult(err))
        }
        handle = VkCommandBuffer(handlePointer[0], device)
        cmdBufAllocateInfo.free()
    }
}