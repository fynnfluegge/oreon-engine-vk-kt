package org.oreon.vk.engine

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.oreon.core.context.BaseContext.Companion.config
import org.oreon.core.vk.framebuffer.FrameBufferColorAttachment
import org.oreon.core.vk.framebuffer.FrameBufferDepthAttachment
import org.oreon.core.vk.framebuffer.VkFrameBuffer
import org.oreon.core.vk.framebuffer.VkFrameBufferObject
import org.oreon.core.vk.pipeline.RenderPass
import org.oreon.core.vk.wrapper.image.VkImageBundle

class OffScreenFbo(device: VkDevice?, memoryProperties: VkPhysicalDeviceMemoryProperties?) : VkFrameBufferObject() {
    init {
        width = config.frameWidth
        height = config.frameHeight
        val samples = config.multisampling_sampleCount
        val albedoAttachment: VkImageBundle = FrameBufferColorAttachment(device!!, memoryProperties!!,
                width, height, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, samples)
        val worldPositionAttachment: VkImageBundle = FrameBufferColorAttachment(device, memoryProperties,
                width, height, VK10.VK_FORMAT_R32G32B32A32_SFLOAT, samples)
        val normalAttachment: VkImageBundle = FrameBufferColorAttachment(device, memoryProperties,
                width, height, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, samples)
        val lightScatteringMaskAttachment: VkImageBundle = FrameBufferColorAttachment(device, memoryProperties,
                width, height, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, samples)
        val specularEmissionAttachment: VkImageBundle = FrameBufferColorAttachment(device, memoryProperties,
                width, height, VK10.VK_FORMAT_R16G16B16A16_SFLOAT, samples)
        val depthBuffer: VkImageBundle = FrameBufferDepthAttachment(device, memoryProperties,
                width, height, VK10.VK_FORMAT_D32_SFLOAT, samples)
        attachments[Attachment.COLOR] = albedoAttachment
        attachments[Attachment.POSITION] = worldPositionAttachment
        attachments[Attachment.NORMAL] = normalAttachment
        attachments[Attachment.LIGHT_SCATTERING] = lightScatteringMaskAttachment
        attachments[Attachment.SPECULAR_EMISSION_DIFFUSE_SSAO_BLOOM] = specularEmissionAttachment
        attachments[Attachment.DEPTH] = depthBuffer
        renderPass = RenderPass(device)
        renderPass!!.addColorAttachment(0, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, samples, VK10.VK_IMAGE_LAYOUT_UNDEFINED,
                VK10.VK_IMAGE_LAYOUT_GENERAL)
        renderPass!!.addColorAttachment(1, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VK10.VK_FORMAT_R32G32B32A32_SFLOAT, samples, VK10.VK_IMAGE_LAYOUT_UNDEFINED,
                VK10.VK_IMAGE_LAYOUT_GENERAL)
        renderPass!!.addColorAttachment(2, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, samples, VK10.VK_IMAGE_LAYOUT_UNDEFINED,
                VK10.VK_IMAGE_LAYOUT_GENERAL)
        renderPass!!.addColorAttachment(3, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, samples, VK10.VK_IMAGE_LAYOUT_UNDEFINED,
                VK10.VK_IMAGE_LAYOUT_GENERAL)
        renderPass!!.addColorAttachment(4, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VK10.VK_FORMAT_R16G16B16A16_SFLOAT, samples, VK10.VK_IMAGE_LAYOUT_UNDEFINED,
                VK10.VK_IMAGE_LAYOUT_GENERAL)
        renderPass!!.addDepthAttachment(5, VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                VK10.VK_FORMAT_D32_SFLOAT, samples, VK10.VK_IMAGE_LAYOUT_UNDEFINED,
                VK10.VK_IMAGE_LAYOUT_GENERAL)
        renderPass!!.addSubpassDependency(VK10.VK_SUBPASS_EXTERNAL, 0,
                VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                VK10.VK_ACCESS_MEMORY_READ_BIT,
                VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT or
                        VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                VK10.VK_DEPENDENCY_BY_REGION_BIT)
        renderPass!!.addSubpassDependency(0, VK10.VK_SUBPASS_EXTERNAL,
                VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT or
                        VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_DEPENDENCY_BY_REGION_BIT)
        renderPass!!.createSubpass()
        renderPass!!.createRenderPass()
        depthAttachmentCount = 1
        colorAttachmentCount = renderPass!!.attachmentCount - depthAttachmentCount
        val pImageViews = MemoryUtil.memAllocLong(renderPass!!.attachmentCount)
        pImageViews.put(0, attachments[Attachment.COLOR]!!.imageView.handle)
        pImageViews.put(1, attachments[Attachment.POSITION]!!.imageView.handle)
        pImageViews.put(2, attachments[Attachment.NORMAL]!!.imageView.handle)
        pImageViews.put(3, attachments[Attachment.SPECULAR_EMISSION_DIFFUSE_SSAO_BLOOM]!!.imageView.handle)
        pImageViews.put(4, attachments[Attachment.LIGHT_SCATTERING]!!.imageView.handle)
        pImageViews.put(5, attachments[Attachment.DEPTH]!!.imageView.handle)
        frameBuffer = VkFrameBuffer(device, width, height, 1, pImageViews, renderPass!!.handle)
    }
}